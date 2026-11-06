package com.firstlogistics.flproof.ui.screens.camera

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Matrix
import android.media.MediaActionSound
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.*
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.firstlogistics.flproof.FLProofApplication
import com.firstlogistics.flproof.camera.CameraQualityEvaluator
import com.firstlogistics.flproof.camera.QualityCheckResult
import com.firstlogistics.flproof.data.entity.EvidenceEntity
import com.firstlogistics.flproof.data.model.DocumentationCategory
import com.firstlogistics.flproof.data.repository.EvidenceRepository
import com.firstlogistics.flproof.data.repository.SettingsRepository
import com.firstlogistics.flproof.location.AddressResolver
import com.firstlogistics.flproof.location.CompassSensorManager
import com.firstlogistics.flproof.location.LocationTracker
import com.firstlogistics.flproof.utils.DateFormatter
import com.firstlogistics.flproof.utils.StorageManager
import com.firstlogistics.flproof.watermark.WatermarkRenderer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

enum class FlashMode { OFF, ON, AUTO }
enum class CameraMode { PHOTO, VIDEO }

data class CameraUiState(
    val cameraMode: CameraMode = CameraMode.PHOTO,
    val selectedCategory: DocumentationCategory = DocumentationCategory.NONE,
    val flashMode: FlashMode = FlashMode.OFF,
    val isFrontCamera: Boolean = false,
    val showGrid: Boolean = true,
    val showLevel: Boolean = true,
    val zoomRatio: Float = 1.0f,
    val exposureIndex: Int = 0,
    val exposureRange: Pair<Int, Int> = Pair(-4, 4),
    val isCapturing: Boolean = false,
    val isRecordingVideo: Boolean = false,
    val recordingDurationSec: Int = 0,
    val activeSessionId: String? = null,
    val sessionMediaCount: Int = 0,
    val sessionIndex: Int = 1,
    val capturedBitmap: Bitmap? = null,
    val previewWatermarkedBitmap: Bitmap? = null,
    val recordedVideoFile: File? = null,
    val qualityCheck: QualityCheckResult? = null,
    val resolvedAddress: String = "Mencari lokasi...",
    val saveSuccessEvidenceId: String? = null,
    val currentTimeString: String = "",
    val currentDateString: String = "",
    val liveEvidenceId: String = "FL-2026-000001",
    val officerName: String = "FADEL RIPAI",
    val isGpsWeak: Boolean = false
)

class CameraViewModel(
    private val context: Context,
    private val evidenceRepository: EvidenceRepository,
    private val settingsRepository: SettingsRepository,
    private val storageManager: StorageManager,
    val locationTracker: LocationTracker,
    val compassSensorManager: CompassSensorManager,
    private val addressResolver: AddressResolver
) : ViewModel() {

    private val _uiState = MutableStateFlow(CameraUiState())
    val uiState: StateFlow<CameraUiState> = _uiState.asStateFlow()

    private var imageCapture: ImageCapture? = null
    private var videoCapture: VideoCapture<Recorder>? = null
    private var activeRecording: Recording? = null
    private var timerJob: Job? = null
    private var cameraControl: CameraControl? = null
    private var cameraInfo: CameraInfo? = null
    private val cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private val sound = MediaActionSound()

    init {
        locationTracker.startTracking()
        compassSensorManager.startListening()

        _uiState.value = _uiState.value.copy(
            officerName = settingsRepository.officerName.ifBlank { "FADEL RIPAI" }
        )

        viewModelScope.launch {
            while (true) {
                _uiState.value = _uiState.value.copy(
                    currentTimeString = DateFormatter.getCurrentTimeFormatted(),
                    currentDateString = DateFormatter.getCurrentDateFormatted()
                )
                delay(1000)
            }
        }

        viewModelScope.launch {
            val nextId = evidenceRepository.generateNextEvidenceId()
            _uiState.value = _uiState.value.copy(liveEvidenceId = nextId)
        }

        viewModelScope.launch {
            locationTracker.gpsData.collect { gps ->
                val isWeak = !gps.isAvailable || gps.accuracy > 25f || gps.accuracy <= 0f
                if (gps.isAvailable && gps.latitude != 0.0) {
                    val addr = addressResolver.resolveAddress(gps.latitude, gps.longitude)
                    _uiState.value = _uiState.value.copy(
                        resolvedAddress = addr,
                        isGpsWeak = isWeak
                    )
                } else {
                    _uiState.value = _uiState.value.copy(isGpsWeak = isWeak)
                }
            }
        }
    }

    fun initSession(sessionId: String?) {
        if (!sessionId.isNullOrBlank()) {
            viewModelScope.launch {
                val existing = evidenceRepository.getSessionPhotosList(sessionId)
                _uiState.value = _uiState.value.copy(
                    activeSessionId = sessionId,
                    sessionIndex = existing.size + 1,
                    sessionMediaCount = existing.size,
                    liveEvidenceId = sessionId
                )
            }
        }
    }

    fun bindCamera(
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView
    ) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

            val cameraSelector = if (_uiState.value.isFrontCamera) {
                CameraSelector.DEFAULT_FRONT_CAMERA
            } else {
                CameraSelector.DEFAULT_BACK_CAMERA
            }

            try {
                cameraProvider.unbindAll()

                val camera = if (_uiState.value.cameraMode == CameraMode.PHOTO) {
                    imageCapture = ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                        .setFlashMode(
                            when (_uiState.value.flashMode) {
                                FlashMode.ON -> ImageCapture.FLASH_MODE_ON
                                FlashMode.AUTO -> ImageCapture.FLASH_MODE_AUTO
                                FlashMode.OFF -> ImageCapture.FLASH_MODE_OFF
                            }
                        )
                        .build()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        imageCapture
                    )
                } else {
                    val recorder = Recorder.Builder()
                        .setQualitySelector(QualitySelector.from(Quality.HD))
                        .build()
                    val vCap = VideoCapture.withOutput(recorder)
                    videoCapture = vCap
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        vCap
                    )
                }

                cameraControl = camera.cameraControl
                cameraInfo = camera.cameraInfo

                cameraInfo?.exposureState?.let { exp ->
                    _uiState.value = _uiState.value.copy(
                        exposureRange = Pair(exp.exposureCompensationRange.lower, exp.exposureCompensationRange.upper),
                        exposureIndex = exp.exposureCompensationIndex
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, ContextCompat.getMainExecutor(context))
    }

    fun setCameraMode(mode: CameraMode, lifecycleOwner: LifecycleOwner, previewView: PreviewView) {
        if (_uiState.value.cameraMode != mode) {
            _uiState.value = _uiState.value.copy(cameraMode = mode)
            bindCamera(lifecycleOwner, previewView)
        }
    }

    fun setCategory(category: DocumentationCategory) {
        _uiState.value = _uiState.value.copy(selectedCategory = category)
    }

    fun toggleFlash() {
        val next = when (_uiState.value.flashMode) {
            FlashMode.OFF -> FlashMode.ON
            FlashMode.ON -> FlashMode.AUTO
            FlashMode.AUTO -> FlashMode.OFF
        }
        _uiState.value = _uiState.value.copy(flashMode = next)
        imageCapture?.flashMode = when (next) {
            FlashMode.ON -> ImageCapture.FLASH_MODE_ON
            FlashMode.AUTO -> ImageCapture.FLASH_MODE_AUTO
            FlashMode.OFF -> ImageCapture.FLASH_MODE_OFF
        }
    }

    fun toggleCameraLens(lifecycleOwner: LifecycleOwner, previewView: PreviewView) {
        _uiState.value = _uiState.value.copy(isFrontCamera = !_uiState.value.isFrontCamera)
        bindCamera(lifecycleOwner, previewView)
    }

    fun toggleGrid() {
        _uiState.value = _uiState.value.copy(showGrid = !_uiState.value.showGrid)
    }

    fun toggleLevel() {
        _uiState.value = _uiState.value.copy(showLevel = !_uiState.value.showLevel)
    }

    fun setZoom(ratio: Float) {
        val clamped = ratio.coerceIn(1.0f, 5.0f)
        _uiState.value = _uiState.value.copy(zoomRatio = clamped)
        cameraControl?.setZoomRatio(clamped)
    }

    fun setExposure(index: Int) {
        val range = _uiState.value.exposureRange
        val clamped = index.coerceIn(range.first, range.second)
        _uiState.value = _uiState.value.copy(exposureIndex = clamped)
        cameraControl?.setExposureCompensationIndex(clamped)
    }

    fun focusOnPoint(x: Float, y: Float, previewWidth: Int, previewHeight: Int) {
        val factory = androidx.camera.core.SurfaceOrientedMeteringPointFactory(previewWidth.toFloat(), previewHeight.toFloat())
        val point = factory.createPoint(x, y)
        val action = FocusMeteringAction.Builder(point, FocusMeteringAction.FLAG_AF or FocusMeteringAction.FLAG_AE)
            .setAutoCancelDuration(3, java.util.concurrent.TimeUnit.SECONDS)
            .build()
        cameraControl?.startFocusAndMetering(action)
    }

    fun takePhoto(onReadyForPreview: () -> Unit) {
        val capture = imageCapture ?: return
        if (_uiState.value.isCapturing) return

        _uiState.value = _uiState.value.copy(isCapturing = true)

        if (settingsRepository.isShutterSoundEnabled) {
            sound.play(MediaActionSound.SHUTTER_CLICK)
        }

        capture.takePicture(
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(imageProxy: ImageProxy) {
                    processCapturedImage(imageProxy, onReadyForPreview)
                }

                override fun onError(exception: ImageCaptureException) {
                    _uiState.value = _uiState.value.copy(isCapturing = false)
                    exception.printStackTrace()
                }
            }
        )
    }

    private fun processCapturedImage(imageProxy: ImageProxy, onReadyForPreview: () -> Unit) {
        viewModelScope.launch(Dispatchers.Default) {
            val rotation = imageProxy.imageInfo.rotationDegrees
            val bitmap = imageProxyToBitmap(imageProxy, rotation)
            imageProxy.close()

            if (bitmap == null) {
                _uiState.value = _uiState.value.copy(isCapturing = false)
                return@launch
            }

            val qualityResult = CameraQualityEvaluator.evaluate(bitmap)
            val currentTargetId = evidenceRepository.generateNextEvidenceId()
            val targetSessionId = _uiState.value.activeSessionId ?: currentTargetId

            if (_uiState.value.activeSessionId == null) {
                _uiState.value = _uiState.value.copy(activeSessionId = targetSessionId)
            }

            val gps = locationTracker.gpsData.value
            val compass = compassSensorManager.compassData.value
            val dateStr = DateFormatter.getCurrentDateFormatted()
            val timeStr = DateFormatter.getCurrentTimeFormatted()
            val address = _uiState.value.resolvedAddress

            val wmConfig = settingsRepository.watermarkConfig.value
            val logoBmp = android.graphics.BitmapFactory.decodeResource(context.resources, com.firstlogistics.flproof.R.drawable.logo_fl)

            val app = context.applicationContext as? FLProofApplication
            val localSessionUrl = app?.evidenceHttpServer?.getSessionUrl(targetSessionId)
                ?: "http://192.168.43.1:8080/session?id=$targetSessionId"

            val portalBase = settingsRepository.vercelPortalUrl.trim().ifBlank {
                settingsRepository.customerWebPortalUrl.trim()
            }
            val finalQrUrl = if (portalBase.isNotBlank()) {
                if (portalBase.contains("?")) "$portalBase&id=$targetSessionId" else "$portalBase?id=$targetSessionId"
            } else {
                localSessionUrl
            }

            val wmBitmap = WatermarkRenderer.renderWatermark(
                sourceBitmap = bitmap,
                data = WatermarkRenderer.WatermarkData(
                    evidenceId = currentTargetId,
                    category = _uiState.value.selectedCategory.title,
                    dateString = dateStr,
                    timeString = timeStr,
                    address = address,
                    latitude = gps.latitude,
                    longitude = gps.longitude,
                    accuracy = gps.accuracy,
                    compassDirection = compass.formattedCompass,
                    resolution = "${bitmap.width}x${bitmap.height}",
                    officerName = _uiState.value.officerName,
                    cameraLens = if (_uiState.value.isFrontCamera) "Kamera Depan" else "Kamera Belakang",
                    deviceModel = "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}",
                    logoBitmap = logoBmp,
                    qrUrl = finalQrUrl
                ),
                config = wmConfig
            )

            withContext(Dispatchers.Main) {
                _uiState.value = _uiState.value.copy(
                    isCapturing = false,
                    capturedBitmap = bitmap,
                    previewWatermarkedBitmap = wmBitmap,
                    qualityCheck = qualityResult
                )
                onReadyForPreview()
            }
        }
    }

    fun startVideoRecording(onRecordingStarted: () -> Unit = {}) {
        val vCap = videoCapture ?: return
        if (_uiState.value.isRecordingVideo) return

        val targetSessionId = _uiState.value.activeSessionId ?: _uiState.value.liveEvidenceId
        if (_uiState.value.activeSessionId == null) {
            _uiState.value = _uiState.value.copy(activeSessionId = targetSessionId)
        }

        val videoFile = storageManager.createVideoOutputFile(targetSessionId, _uiState.value.sessionIndex)
        val outputOptions = FileOutputOptions.Builder(videoFile).build()

        var pendingRecording = vCap.output.prepareRecording(context, outputOptions)
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            pendingRecording = pendingRecording.withAudioEnabled()
        }

        if (settingsRepository.isShutterSoundEnabled) {
            sound.play(MediaActionSound.START_VIDEO_RECORDING)
        }

        _uiState.value = _uiState.value.copy(
            isRecordingVideo = true,
            recordingDurationSec = 0
        )

        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (_uiState.value.isRecordingVideo) {
                delay(1000)
                _uiState.value = _uiState.value.copy(
                    recordingDurationSec = _uiState.value.recordingDurationSec + 1
                )
            }
        }

        activeRecording = pendingRecording.start(ContextCompat.getMainExecutor(context)) { recordEvent ->
            when (recordEvent) {
                is VideoRecordEvent.Start -> {
                    onRecordingStarted()
                }
                is VideoRecordEvent.Finalize -> {
                    if (!recordEvent.hasError()) {
                        _uiState.value = _uiState.value.copy(
                            isRecordingVideo = false,
                            recordedVideoFile = videoFile
                        )
                    } else {
                        _uiState.value = _uiState.value.copy(isRecordingVideo = false)
                        recordEvent.cause?.printStackTrace()
                    }
                }
            }
        }
    }

    fun stopVideoRecording(onReadyForPreview: () -> Unit) {
        if (!_uiState.value.isRecordingVideo) return
        if (settingsRepository.isShutterSoundEnabled) {
            sound.play(MediaActionSound.STOP_VIDEO_RECORDING)
        }
        timerJob?.cancel()
        timerJob = null
        activeRecording?.stop()
        activeRecording = null
        _uiState.value = _uiState.value.copy(isRecordingVideo = false)
        viewModelScope.launch {
            delay(350)
            onReadyForPreview()
        }
    }

    fun retakePhoto() {
        _uiState.value.capturedBitmap?.recycle()
        _uiState.value.previewWatermarkedBitmap?.recycle()
        _uiState.value = _uiState.value.copy(
            capturedBitmap = null,
            previewWatermarkedBitmap = null,
            qualityCheck = null
        )
    }

    fun retakeVideo() {
        _uiState.value.recordedVideoFile?.delete()
        _uiState.value = _uiState.value.copy(
            recordedVideoFile = null,
            recordingDurationSec = 0
        )
    }

    fun cancelSession() {
        _uiState.value.capturedBitmap?.recycle()
        _uiState.value.previewWatermarkedBitmap?.recycle()
        _uiState.value.recordedVideoFile?.delete()
        _uiState.value = _uiState.value.copy(
            capturedBitmap = null,
            previewWatermarkedBitmap = null,
            recordedVideoFile = null,
            activeSessionId = null,
            sessionIndex = 1,
            sessionMediaCount = 0
        )
    }

    fun saveCurrentEvidence(
        notes: String = "",
        addMoreMedia: Boolean = false,
        nextMode: CameraMode = CameraMode.PHOTO,
        onSaved: (String) -> Unit
    ) {
        val isPhoto = _uiState.value.previewWatermarkedBitmap != null && _uiState.value.capturedBitmap != null
        val isVideo = _uiState.value.recordedVideoFile != null

        if (!isPhoto && !isVideo) return

        val originalBmp = _uiState.value.capturedBitmap
        val watermarkedBmp = _uiState.value.previewWatermarkedBitmap
        val videoFile = _uiState.value.recordedVideoFile

        val targetSessionId = _uiState.value.activeSessionId ?: _uiState.value.liveEvidenceId
        val currentSessionIndex = _uiState.value.sessionIndex

        viewModelScope.launch(Dispatchers.IO) {
            val targetEvidenceId = evidenceRepository.generateNextEvidenceId()
            val gps = locationTracker.gpsData.value
            val compass = compassSensorManager.compassData.value
            val dateStr = DateFormatter.getCurrentDateFormatted()
            val timeStr = DateFormatter.getCurrentTimeFormatted()
            val address = _uiState.value.resolvedAddress

            val entity = if (isPhoto && originalBmp != null && watermarkedBmp != null) {
                val originalFile = storageManager.saveOriginalPhoto(originalBmp, targetEvidenceId)
                val watermarkedFile = storageManager.saveWatermarkedPhoto(watermarkedBmp, targetEvidenceId)
                val sha256 = storageManager.calculateSha256(originalFile)

                EvidenceEntity(
                    evidenceId = targetEvidenceId,
                    fileName = watermarkedFile.name,
                    dateFormatted = dateStr,
                    timeFormatted = timeStr,
                    timestamp = System.currentTimeMillis(),
                    latitude = gps.latitude,
                    longitude = gps.longitude,
                    address = address,
                    gpsAccuracy = gps.accuracy,
                    compassDegrees = compass.azimuthDegrees,
                    compassDirection = compass.formattedCompass,
                    originalPhotoPath = originalFile.absolutePath,
                    watermarkedPhotoPath = watermarkedFile.absolutePath,
                    sha256Hash = sha256,
                    status = if (_uiState.value.selectedCategory.isProblematic) "FLAGGED" else "VERIFIED",
                    category = _uiState.value.selectedCategory.title,
                    deviceModel = "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}",
                    resolution = "${watermarkedBmp.width}x${watermarkedBmp.height}",
                    fileSizeBytes = watermarkedFile.length(),
                    notes = notes,
                    sessionId = targetSessionId,
                    sessionIndex = currentSessionIndex,
                    officerName = _uiState.value.officerName,
                    cameraLens = if (_uiState.value.isFrontCamera) "Kamera Depan" else "Kamera Belakang",
                    mediaType = "photo",
                    videoPath = null
                )
            } else if (isVideo && videoFile != null) {
                val sha256 = storageManager.calculateSha256(videoFile)
                EvidenceEntity(
                    evidenceId = targetEvidenceId,
                    fileName = videoFile.name,
                    dateFormatted = dateStr,
                    timeFormatted = timeStr,
                    timestamp = System.currentTimeMillis(),
                    latitude = gps.latitude,
                    longitude = gps.longitude,
                    address = address,
                    gpsAccuracy = gps.accuracy,
                    compassDegrees = compass.azimuthDegrees,
                    compassDirection = compass.formattedCompass,
                    originalPhotoPath = videoFile.absolutePath,
                    watermarkedPhotoPath = videoFile.absolutePath,
                    sha256Hash = sha256,
                    status = if (_uiState.value.selectedCategory.isProblematic) "FLAGGED" else "VERIFIED",
                    category = _uiState.value.selectedCategory.title,
                    deviceModel = "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}",
                    resolution = "1280x720 HD",
                    fileSizeBytes = videoFile.length(),
                    notes = notes,
                    sessionId = targetSessionId,
                    sessionIndex = currentSessionIndex,
                    officerName = _uiState.value.officerName,
                    cameraLens = if (_uiState.value.isFrontCamera) "Kamera Depan" else "Kamera Belakang",
                    mediaType = "video",
                    videoPath = videoFile.absolutePath
                )
            } else {
                return@launch
            }

            evidenceRepository.saveEvidence(entity)

            // Trigger asynchronous sync of this session to Supabase
            val app = context.applicationContext as? FLProofApplication
            app?.supabaseUploader?.syncSessionToCloud(targetSessionId)

            val newNextId = evidenceRepository.generateNextEvidenceId()

            withContext(Dispatchers.Main) {
                originalBmp?.recycle()
                watermarkedBmp?.recycle()

                if (addMoreMedia) {
                    _uiState.value = _uiState.value.copy(
                        capturedBitmap = null,
                        previewWatermarkedBitmap = null,
                        recordedVideoFile = null,
                        qualityCheck = null,
                        activeSessionId = targetSessionId,
                        sessionIndex = currentSessionIndex + 1,
                        sessionMediaCount = _uiState.value.sessionMediaCount + 1,
                        cameraMode = nextMode
                    )
                    onSaved(targetSessionId)
                } else {
                    _uiState.value = _uiState.value.copy(
                        capturedBitmap = null,
                        previewWatermarkedBitmap = null,
                        recordedVideoFile = null,
                        qualityCheck = null,
                        activeSessionId = null,
                        sessionIndex = 1,
                        sessionMediaCount = 0,
                        saveSuccessEvidenceId = targetSessionId,
                        liveEvidenceId = newNextId
                    )
                    onSaved(targetSessionId)
                }
            }
        }
    }

    private fun imageProxyToBitmap(image: ImageProxy, rotationDegrees: Int): Bitmap? {
        val plane = image.planes[0]
        val buffer = plane.buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)
        val bitmap = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return null

        return if (rotationDegrees != 0) {
            val matrix = Matrix().apply { postRotate(rotationDegrees.toFloat()) }
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        } else {
            bitmap
        }
    }

    override fun onCleared() {
        super.onCleared()
        locationTracker.stopTracking()
        compassSensorManager.stopListening()
        timerJob?.cancel()
        activeRecording?.stop()
        cameraExecutor.shutdown()
        sound.release()
    }
}
