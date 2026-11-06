package com.firstlogistics.flproof.location

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.abs
import kotlin.math.roundToInt

data class CompassData(
    val azimuthDegrees: Float = 0f,
    val directionName: String = "U",
    val formattedCompass: String = "U 0°",
    val pitch: Float = 0f,
    val roll: Float = 0f,
    val isLevel: Boolean = false
)

class CompassSensorManager(context: Context) : SensorEventListener {

    private val sensorManager =
        context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager

    private val rotationSensor =
        sensorManager?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
    private val accelerometer =
        sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val magnetometer =
        sensorManager?.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

    private val _compassData = MutableStateFlow(CompassData())
    val compassData: StateFlow<CompassData> = _compassData.asStateFlow()

    private val gravityMatrix = FloatArray(9)
    private val orientationAngles = FloatArray(3)
    private var lastAccelerometer = FloatArray(3)
    private var lastMagnetometer = FloatArray(3)
    private var lastAccelerometerSet = false
    private var lastMagnetometerSet = false

    private var isListening = false

    fun startListening() {
        if (isListening || sensorManager == null) return
        isListening = true

        if (rotationSensor != null) {
            sensorManager.registerListener(this, rotationSensor, SensorManager.SENSOR_DELAY_UI)
        } else {
            accelerometer?.let {
                sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
            }
            magnetometer?.let {
                sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
            }
        }
    }

    fun stopListening() {
        if (!isListening || sensorManager == null) return
        isListening = false
        sensorManager.unregisterListener(this)
        lastAccelerometerSet = false
        lastMagnetometerSet = false
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return

        if (event.sensor.type == Sensor.TYPE_ROTATION_VECTOR) {
            SensorManager.getRotationMatrixFromVector(gravityMatrix, event.values)
            SensorManager.getOrientation(gravityMatrix, orientationAngles)
            computeOrientation()
            return
        }

        if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            System.arraycopy(event.values, 0, lastAccelerometer, 0, event.values.size)
            lastAccelerometerSet = true
        } else if (event.sensor.type == Sensor.TYPE_MAGNETIC_FIELD) {
            System.arraycopy(event.values, 0, lastMagnetometer, 0, event.values.size)
            lastMagnetometerSet = true
        }

        if (lastAccelerometerSet && lastMagnetometerSet) {
            if (SensorManager.getRotationMatrix(gravityMatrix, null, lastAccelerometer, lastMagnetometer)) {
                SensorManager.getOrientation(gravityMatrix, orientationAngles)
                computeOrientation()
            }
        }
    }

    private fun computeOrientation() {
        // orientationAngles: [0] = azimuth (radians), [1] = pitch (radians), [2] = roll (radians)
        var azimuthRad = orientationAngles[0]
        var azimuthDeg = Math.toDegrees(azimuthRad.toDouble()).toFloat()
        if (azimuthDeg < 0) azimuthDeg += 360f

        val pitchDeg = Math.toDegrees(orientationAngles[1].toDouble()).toFloat()
        val rollDeg = Math.toDegrees(orientationAngles[2].toDouble()).toFloat()

        val isLevel = abs(pitchDeg) < 2.5f && abs(rollDeg) < 2.5f

        val direction = getCardinalDirection(azimuthDeg)
        val formatted = "$direction ${azimuthDeg.roundToInt()}°"

        _compassData.value = CompassData(
            azimuthDegrees = azimuthDeg,
            directionName = direction,
            formattedCompass = formatted,
            pitch = pitchDeg,
            roll = rollDeg,
            isLevel = isLevel
        )
    }

    private fun getCardinalDirection(deg: Float): String {
        return when {
            deg >= 337.5 || deg < 22.5 -> "U"
            deg >= 22.5 && deg < 67.5 -> "TL"
            deg >= 67.5 && deg < 112.5 -> "T"
            deg >= 112.5 && deg < 157.5 -> "TG"
            deg >= 157.5 && deg < 202.5 -> "S"
            deg >= 202.5 && deg < 247.5 -> "BD"
            deg >= 247.5 && deg < 292.5 -> "B"
            deg >= 292.5 && deg < 337.5 -> "BL"
            else -> "U"
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // No-op
    }
}

