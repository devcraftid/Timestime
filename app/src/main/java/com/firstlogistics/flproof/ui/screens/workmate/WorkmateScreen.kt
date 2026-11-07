package com.firstlogistics.flproof.ui.screens.workmate

import android.Manifest
import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Base64
import android.webkit.JavascriptInterface
import android.webkit.PermissionRequest
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.firstlogistics.flproof.FLProofApplication
import com.firstlogistics.flproof.ui.components.FLProofTopBar
import com.firstlogistics.flproof.ui.theme.BrandBlue
import com.firstlogistics.flproof.ui.theme.NavyBackground
import java.io.ByteArrayOutputStream
import java.io.File

/**
 * JavaScript Interface untuk interaksi native antara Workmate WebView dengan OS Android.
 * - Membuka kamera langsung untuk foto fisik barang.
 * - Menyimpan foto fisik barang langsung ke Galeri HP (Pictures/FL_WORKMATE) via MediaStore.
 */
class WorkmateNativeBridge(
    private val context: Context,
    private val onOpenNativeCamera: (slotIndex: Int, isEdit: Boolean) -> Unit
) {
    @JavascriptInterface
    fun isNativeApp(): Boolean = true

    @JavascriptInterface
    fun openNativeCamera(slotIndex: Int, isEdit: Boolean) {
        Handler(Looper.getMainLooper()).post {
            onOpenNativeCamera(slotIndex, isEdit)
        }
    }

    @JavascriptInterface
    fun saveImageToGallery(base64Data: String, fileName: String): Boolean {
        return try {
            val cleanBase64 = if (base64Data.contains(",")) {
                base64Data.substringAfter(",")
            } else {
                base64Data
            }
            val bytes = Base64.decode(cleanBase64, Base64.DEFAULT)

            val resolver = context.contentResolver
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/FL_WORKMATE")
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }
            }

            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            if (uri != null) {
                resolver.openOutputStream(uri)?.use { out ->
                    out.write(bytes)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    contentValues.clear()
                    contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                    resolver.update(uri, contentValues, null, null)
                }
                Handler(Looper.getMainLooper()).post {
                    Toast.makeText(context, "Foto tersimpan di Galeri: $fileName", Toast.LENGTH_SHORT).show()
                }
                true
            } else {
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Handler(Looper.getMainLooper()).post {
                Toast.makeText(context, "Gagal simpan ke memori: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
            false
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WorkmateScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val app = context.applicationContext as FLProofApplication
    var webViewInstance: WebView? by remember { mutableStateOf(null) }

    // Request CAMERA and RECORD_AUDIO for in-app barcode scanning & speech recognition
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { _ -> }

    LaunchedEffect(Unit) {
        val hasCam = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        val hasAudio = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        if (!hasCam || !hasAudio) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.CAMERA,
                    Manifest.permission.RECORD_AUDIO
                )
            )
        }
    }

    // Direct Native Camera Launcher for Physical Photo Capture
    var pendingPhotoFile by remember { mutableStateOf<File?>(null) }
    var pendingSlotIndex by remember { mutableStateOf(0) }
    var isEditPhotoTarget by remember { mutableStateOf(false) }

    val takeCameraPictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        val file = pendingPhotoFile
        if (success && file != null && file.exists() && file.length() > 0) {
            try {
                val exif = androidx.exifinterface.media.ExifInterface(file.absolutePath)
                val orientation = exif.getAttributeInt(
                    androidx.exifinterface.media.ExifInterface.TAG_ORIENTATION,
                    androidx.exifinterface.media.ExifInterface.ORIENTATION_NORMAL
                )
                val rotation = when (orientation) {
                    androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                    androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                    androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                    else -> 0f
                }

                var bitmap = BitmapFactory.decodeFile(file.absolutePath)
                if (bitmap != null) {
                    if (rotation != 0f) {
                        val matrix = Matrix().apply { postRotate(rotation) }
                        bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
                    }

                    val maxDim = 1024
                    val ratio = minOf(maxDim.toFloat() / bitmap.width, maxDim.toFloat() / bitmap.height, 1.0f)
                    val scaled = if (ratio < 1.0f) {
                        Bitmap.createScaledBitmap(bitmap, (bitmap.width * ratio).toInt(), (bitmap.height * ratio).toInt(), true)
                    } else bitmap

                    val stream = ByteArrayOutputStream()
                    scaled.compress(Bitmap.CompressFormat.JPEG, 85, stream)
                    val base64 = Base64.encodeToString(stream.toByteArray(), Base64.NO_WRAP)
                    val dataUrl = "data:image/jpeg;base64,$base64"

                    val jsCall = if (isEditPhotoTarget) {
                        "if(window.handleNativeEditPhotoResult) window.handleNativeEditPhotoResult($pendingSlotIndex, '$dataUrl');"
                    } else {
                        "if(window.handleNativePhotoResult) window.handleNativePhotoResult($pendingSlotIndex, '$dataUrl');"
                    }
                    webViewInstance?.evaluateJavascript(jsCall, null)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, "Gagal memproses foto: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            } finally {
                try { file.delete() } catch (_: Exception) {}
                pendingPhotoFile = null
            }
        } else {
            pendingPhotoFile?.delete()
            pendingPhotoFile = null
        }
    }

    val triggerNativeCamera: (Int, Boolean) -> Unit = { slot, isEdit ->
        try {
            val file = File(context.cacheDir, "workmate_cam_${System.currentTimeMillis()}.jpg")
            val uri = FileProvider.getUriForFile(
                context,
                "com.firstlogistics.flproof.fileprovider",
                file
            )
            pendingPhotoFile = file
            pendingSlotIndex = slot
            isEditPhotoTarget = isEdit
            takeCameraPictureLauncher.launch(uri)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Kamera error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    // File Chooser for HTML file inputs (<input type="file" ...>)
    var fileChooserCallback by remember { mutableStateOf<ValueCallback<Array<Uri>>?>(null) }
    val fileChooserLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val intentData = result.data
        val results: Array<Uri>? = when {
            intentData?.data != null -> arrayOf(intentData.data!!)
            intentData?.clipData != null -> {
                val clipData = intentData.clipData!!
                Array(clipData.itemCount) { i -> clipData.getItemAt(i).uri }
            }
            else -> null
        }
        fileChooserCallback?.onReceiveValue(results)
        fileChooserCallback = null
    }

    // Hardware back press handler for WebView
    BackHandler {
        if (webViewInstance?.canGoBack() == true) {
            webViewInstance?.goBack()
        } else {
            onNavigateBack()
        }
    }

    Scaffold(
        topBar = {
            FLProofTopBar(
                title = "Volume & Berat",
                canNavigateBack = true,
                onNavigateBack = onNavigateBack,
                showSettingsButton = false,
                actions = {
                    IconButton(
                        onClick = {
                            val portalBase = app.settingsRepository.vercelPortalUrl.trim().ifBlank {
                                app.settingsRepository.customerWebPortalUrl.trim()
                            }
                            val url = if (portalBase.isNotBlank()) {
                                val cleanBase = portalBase.substringBefore('?').trimEnd('/')
                                "$cleanBase/workmate"
                            } else {
                                "https://timestime.vercel.app/workmate"
                            }
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Language,
                            contentDescription = "Buka di Web Browser",
                            tint = BrandBlue
                        )
                    }
                }
            )
        },
        containerColor = NavyBackground
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            AndroidView(
                factory = { ctx ->
                    WebView(ctx).apply {
                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            allowFileAccess = true
                            allowContentAccess = true
                            mediaPlaybackRequiresUserGesture = false
                            cacheMode = WebSettings.LOAD_DEFAULT
                            useWideViewPort = false
                            loadWithOverviewMode = false
                            setSupportZoom(false)
                            builtInZoomControls = false
                            displayZoomControls = false
                        }

                        // Cegah scroll & overscroll horizontal pada WebView
                        isHorizontalScrollBarEnabled = false
                        isVerticalScrollBarEnabled = true
                        overScrollMode = WebView.OVER_SCROLL_NEVER

                        // Connect native bridge for gallery photo saving and native camera opening
                        addJavascriptInterface(WorkmateNativeBridge(ctx, triggerNativeCamera), "AndroidBridge")

                        webViewClient = WebViewClient()

                        webChromeClient = object : WebChromeClient() {
                            override fun onPermissionRequest(request: PermissionRequest) {
                                request.grant(request.resources)
                            }

                            override fun onShowFileChooser(
                                webView: WebView?,
                                filePathCallback: ValueCallback<Array<Uri>>?,
                                fileChooserParams: FileChooserParams?
                            ): Boolean {
                                fileChooserCallback?.onReceiveValue(null)
                                fileChooserCallback = filePathCallback

                                val intent = fileChooserParams?.createIntent() ?: Intent(Intent.ACTION_GET_CONTENT).apply {
                                    type = "image/*"
                                    addCategory(Intent.CATEGORY_OPENABLE)
                                }
                                return try {
                                    fileChooserLauncher.launch(intent)
                                    true
                                } catch (e: Exception) {
                                    fileChooserCallback?.onReceiveValue(null)
                                    fileChooserCallback = null
                                    false
                                }
                            }
                        }

                        setDownloadListener { url, _, _, _, _ ->
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                ctx.startActivity(intent)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }

                        loadUrl("file:///android_asset/workmate.html")
                        webViewInstance = this
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

