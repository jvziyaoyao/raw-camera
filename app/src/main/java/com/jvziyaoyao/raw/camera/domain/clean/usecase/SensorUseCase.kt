package com.jvziyaoyao.raw.camera.domain.clean.usecase

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.activity.ComponentActivity
import com.jvziyaoyao.raw.camera.util.ContextUtil
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.math.acos
import kotlin.math.sqrt

class SensorUseCase {

    private var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private var magnetometer: Sensor? = null

    val gravityFlow = MutableStateFlow(0F)

    val pitchFlow = MutableStateFlow(0F)
    val rollFlow = MutableStateFlow(0F)
    val yawFlow = MutableStateFlow(0F)

    init {
        val context = ContextUtil.getApplicationByReflect()
        sensorManager = context.getSystemService(ComponentActivity.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
    }

    fun start() {
        val sensorDelay = SensorManager.SENSOR_DELAY_GAME
//        val sensorDelay = SensorManager.SENSOR_DELAY_NORMAL
        sensorManager.registerListener(
            sensorListener,
            accelerometer,
            sensorDelay
        )
        sensorManager.registerListener(
            sensorListener,
            magnetometer,
            sensorDelay
        )
    }

    fun stop() {
        sensorManager.unregisterListener(sensorListener, accelerometer)
        sensorManager.unregisterListener(sensorListener, magnetometer)
    }

    private val sensorListener = object : SensorEventListener {
        private var accelerometerValues: FloatArray? = null
        private var magneticValues: FloatArray? = null

        override fun onSensorChanged(event: SensorEvent?) {
            if (event?.sensor?.type == Sensor.TYPE_ACCELEROMETER) {
                accelerometerValues = event.values
                gravityFlow.value = getGravityDegreesFromAccelerometer(event.values)
            } else if (event?.sensor?.type == Sensor.TYPE_MAGNETIC_FIELD) {
                magneticValues = event.values
            }
            if (accelerometerValues != null && magneticValues != null) {
                val rotationMatrix = FloatArray(9)
                SensorManager.getRotationMatrix(
                    rotationMatrix,
                    null,
                    accelerometerValues,
                    magneticValues
                )
                val orientation = FloatArray(3)
                SensorManager.getOrientation(rotationMatrix, orientation)
                pitchFlow.value = Math.toDegrees(orientation[1].toDouble()).toFloat()
                rollFlow.value = Math.toDegrees(orientation[2].toDouble()).toFloat()
                yawFlow.value = Math.toDegrees(orientation[0].toDouble()).toFloat()
            }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        }
    }

    private fun getGravityDegreesFromAccelerometer(values: FloatArray): Float {
        val ax = values[0]
        val ay = values[1]
        val g = sqrt((ax * ax + ay * ay).toDouble())
        var cos = ay / g
        if (cos > 1) {
            cos = 1.0
        } else if (cos < -1) {
            cos = -1.0
        }
        var rad = acos(cos)
        if (ax < 0) rad = 2 * Math.PI - rad
        return (180 * rad / Math.PI).toFloat()
    }

}