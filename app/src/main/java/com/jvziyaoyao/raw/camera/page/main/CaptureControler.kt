package com.jvziyaoyao.raw.camera.page.main

import android.graphics.Rect
import android.hardware.camera2.CameraMetadata
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.CaptureResult
import android.hardware.camera2.params.MeteringRectangle
import android.hardware.camera2.params.RggbChannelVector
import android.os.Build
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking

data class FocusRequestTrigger(
    var focusRect: Rect?,
    var focusRequest: Boolean,
    var focusCancel: Boolean,
)

fun calcTemperature(factor: Int): RggbChannelVector {
    return RggbChannelVector(
        0.635f + (0.0208333f * factor),
        1.0f,
        1.0f,
        3.7420394f + (-0.0287829f * factor)
    )
}

class CaptureController {

    val aeCompensationFlow = MutableStateFlow(0)

    val sensorSensitivityFlow = MutableStateFlow<Int?>(null)

    val sensorExposureTimeFlow = MutableStateFlow<Long?>(null)

    val focalDistanceFlow = MutableStateFlow<Float?>(null)

    val customTemperatureFlow = MutableStateFlow<Int?>(null)

    val oisEnableFlow = MutableStateFlow(false)

    val currentSceneModeFlow = MutableStateFlow<SceneMode?>(null)

    val zoomRatioFlow = MutableStateFlow(1F)

    val currentFaceDetectModeFlow = MutableStateFlow<FaceDetectMode?>(null)

    private val focusRequestTriggerFlow = MutableStateFlow<FocusRequestTrigger?>(null)

    val manualSensorParamsFlow = combine(
        listOf(
            aeCompensationFlow,
            sensorSensitivityFlow,
            sensorExposureTimeFlow,
            focalDistanceFlow,
            customTemperatureFlow,
            oisEnableFlow,
            currentSceneModeFlow,
            zoomRatioFlow,
            focusRequestTriggerFlow,
            currentFaceDetectModeFlow,
        )
    ) { list -> list }

    val afEnableFlow = focalDistanceFlow.map { it == null }

    val aeEnableFlow = combine(
        sensorSensitivityFlow,
        sensorExposureTimeFlow,
    ) { sensorSensitivity, sensorExposureTime ->
        sensorSensitivity == null && sensorExposureTime == null
    }

    val awbEnableFlow = customTemperatureFlow.map { it == null }

    fun setAfEnable() {
        focalDistanceFlow.value = null
    }

    fun setAeEnable() {
        sensorSensitivityFlow.value = null
        sensorExposureTimeFlow.value = null
    }

    fun setAwbEnable() {
        customTemperatureFlow.value = null
    }

    fun setCurrentCaptureParams(builder: CaptureRequest.Builder) {
        builder.apply {
            val afEnable = runBlocking { afEnableFlow.first() }
            val aeEnable = runBlocking { aeEnableFlow.first() }
            val awbEnable = runBlocking { awbEnableFlow.first() }

            if (afEnable) {
                set(
                    CaptureRequest.CONTROL_AF_MODE,
                    CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE
                )
            } else {
                set(
                    CaptureRequest.CONTROL_AF_MODE,
                    CaptureRequest.CONTROL_AF_MODE_OFF
                )
                val focalDistance = focalDistanceFlow.value
                set(CaptureRequest.LENS_FOCUS_DISTANCE, focalDistance)
            }

            if (aeEnable) {
                set(
                    CaptureRequest.CONTROL_AE_MODE,
                    CaptureRequest.CONTROL_AE_MODE_ON
                )
                val aeCompensation = aeCompensationFlow.value
                set(
                    CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION,
                    aeCompensation
                )
            } else {
                set(
                    CaptureRequest.CONTROL_AE_MODE,
                    CaptureRequest.CONTROL_AE_MODE_OFF
                )
                val sensorSensitivity = sensorSensitivityFlow.value
                val sensorExposureTime = sensorExposureTimeFlow.value
                set(CaptureRequest.SENSOR_EXPOSURE_TIME, sensorExposureTime)
                set(CaptureRequest.SENSOR_SENSITIVITY, sensorSensitivity)
            }

            if (awbEnable) {
                set(
                    CaptureRequest.CONTROL_AWB_MODE,
                    CaptureResult.CONTROL_AWB_MODE_AUTO,
                )
            } else {
                set(
                    CaptureRequest.CONTROL_AWB_MODE,
                    CaptureResult.CONTROL_AWB_MODE_OFF,
                )
                val customTemperature = customTemperatureFlow.value
                val rggbChannelVector = calcTemperature(customTemperature!!)
                set(
                    CaptureRequest.COLOR_CORRECTION_MODE,
                    CameraMetadata.COLOR_CORRECTION_MODE_TRANSFORM_MATRIX
                )
                set(CaptureRequest.COLOR_CORRECTION_GAINS, rggbChannelVector)
            }


            val currentSceneMode = currentSceneModeFlow.value
            if (currentSceneMode != null) {
                set(
                    CaptureRequest.CONTROL_SCENE_MODE,
                    currentSceneMode.code,
                )
            }

            set(
                CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE,
                if (oisEnableFlow.value) CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE_ON
                else CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE_OFF,
            )

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                set(
                    CaptureRequest.CONTROL_ZOOM_RATIO,
                    zoomRatioFlow.value
                )
            }

            currentFaceDetectModeFlow.value?.let {
                set(
                    CaptureRequest.STATISTICS_FACE_DETECT_MODE,
                    it.code
                )
            }

            focusRequestTriggerFlow.value?.apply {
                if (focusRequest && focusRect != null) {
                    val meteringRectangle =
                        MeteringRectangle(focusRect, MeteringRectangle.METERING_WEIGHT_MAX)

                    set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_AUTO)
                    set(CaptureRequest.CONTROL_AF_REGIONS, arrayOf(meteringRectangle))
                    set(
                        CaptureRequest.CONTROL_AF_TRIGGER,
                        CameraMetadata.CONTROL_AF_TRIGGER_START
                    )

                    set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON)
                    set(CaptureRequest.CONTROL_AE_REGIONS, arrayOf(meteringRectangle))
                    set(
                        CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER,
                        CameraMetadata.CONTROL_AE_PRECAPTURE_TRIGGER_START
                    )
                    focusRequest = false
                }
                if (focusCancel) {
                    set(
                        CaptureRequest.CONTROL_AF_TRIGGER,
                        CameraMetadata.CONTROL_AF_TRIGGER_IDLE
                    )
                    set(CaptureRequest.CONTROL_AF_REGIONS, null)
                    set(
                        CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER,
                        CameraMetadata.CONTROL_AE_PRECAPTURE_TRIGGER_IDLE
                    )
                    set(CaptureRequest.CONTROL_AE_REGIONS, null)
                }
            }
        }
    }

    fun focusRequest(rect: Rect) {
        focusRequestTriggerFlow.value = FocusRequestTrigger(
            focusRect = rect,
            focusRequest = true,
            focusCancel = false
        )
    }

    fun focusCancel() {
        focusRequestTriggerFlow.value = FocusRequestTrigger(
            focusRect = null,
            focusRequest = false,
            focusCancel = true,
        )
    }

}