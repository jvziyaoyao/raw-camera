package com.jvziyaoyao.raw.camera.page.sensor

import androidx.lifecycle.ViewModel
import com.jvziyaoyao.raw.camera.holder.SensorHolder

class SensorViewModel : ViewModel() {

    private lateinit var sensorHolder: SensorHolder

    val gravityFlow
        get() = sensorHolder.gravityFlow

    val pitchFlow
        get() = sensorHolder.pitchFlow
    val rollFlow
        get() = sensorHolder.rollFlow
    val yawFlow
        get() = sensorHolder.yawFlow

    fun setupSensor() {
        sensorHolder = SensorHolder()
    }

    fun startSensor() = sensorHolder.start()

    fun stopSensor() = sensorHolder.stop()

}