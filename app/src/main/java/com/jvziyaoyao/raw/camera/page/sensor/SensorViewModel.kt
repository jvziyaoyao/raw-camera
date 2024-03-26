package com.jvziyaoyao.raw.camera.page.sensor

import androidx.lifecycle.ViewModel
import com.jvziyaoyao.raw.camera.domain.usecase.SensorUseCase

class SensorViewModel(
    private val sensorUseCase: SensorUseCase
) : ViewModel() {

    val gravityFlow = sensorUseCase.gravityFlow

    val pitchFlow = sensorUseCase.pitchFlow
    val rollFlow = sensorUseCase.rollFlow
    val yawFlow = sensorUseCase.yawFlow

    fun startSensor() = sensorUseCase.start()

    fun stopSensor() = sensorUseCase.stop()

}