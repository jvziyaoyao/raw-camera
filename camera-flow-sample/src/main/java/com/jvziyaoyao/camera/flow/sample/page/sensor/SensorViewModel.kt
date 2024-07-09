package com.jvziyaoyao.camera.flow.sample.page.sensor

import androidx.lifecycle.ViewModel
import com.jvziyaoyao.camera.flow.holder.sensor.SensorFlow

class SensorViewModel : ViewModel() {

    private lateinit var sensorFlow: SensorFlow

    val gravityFlow
        get() = sensorFlow.gravityFlow

    val pitchFlow
        get() = sensorFlow.pitchFlow
    val rollFlow
        get() = sensorFlow.rollFlow
    val yawFlow
        get() = sensorFlow.yawFlow

    fun setupSensor() {
        sensorFlow = SensorFlow()
    }

    fun startSensor() = sensorFlow.start()

    fun stopSensor() = sensorFlow.stop()

}