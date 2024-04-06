package com.jvziyaoyao.raw.camera.page.main

import android.graphics.Bitmap
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.CaptureResult
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.jvziyaoyao.raw.camera.holder.SensorHolder
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * @program: TestFocusable
 *
 * @description:
 *
 * @author: JVZIYAOYAO
 *
 * @create: 2024-02-02 11:48
 **/

enum class CaptureMode() {
    AUTO,
    MANUAL,
    ;
}

enum class ExposureTime(
    val label: String,
    val time: Long,
) {
    E_1_4000(
        label = "1/4000",
        time = 250 * 1000,
    ),
    E_1_2000(
        label = "1/2000",
        time = 500 * 1000,
    ),
    E_1_1000(
        label = "1/1000",
        time = 1000 * 1000,
    ),
    E_1_500(
        label = "1/500",
        time = 2 * 1000 * 1000,
    ),
    E_1_200(
        label = "1/200",
        time = 5 * 1000 * 1000,
    ),
    E_1_100(
        label = "1/100",
        time = 10 * 1000 * 1000,
    ),
    E_0_1(
        label = "0.1",
        time = 100 * 1000 * 1000,
    ),
    ;
}

data class FocusRequestOrientation(
    var pitch: Float,
    var roll: Float,
    var yaw: Float,
    var timestamp: Long = System.currentTimeMillis(),
)

class CameraViewModel : ViewModel() {

    private val TAG = CameraViewModel::class.java.name

    /**
     *
     * 姿态传感器
     *
     */

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

    /**
     *
     * 影响相机启动相关
     *
     */

    val displayRotation = mutableStateOf(0)

    val resumeTimestampFlow = MutableStateFlow<Long?>(null)

    val cameraPairListFlow =
        MutableStateFlow(emptyList<Pair<String, CameraCharacteristics>>())

    val currentCameraPairFlow = MutableStateFlow<Pair<String, CameraCharacteristics>?>(null)

    val currentOutputItemFlow = MutableStateFlow<OutputItem?>(null)

    val allPermissionGrantedFlow = MutableStateFlow(false)

    /**
     *
     *
     *
     */

    val captureLoading = mutableStateOf(false)

    val rotationOrientation = mutableStateOf(0)

    val captureModeFlow = MutableStateFlow(CaptureMode.MANUAL)

    val captureFrameRate = mutableStateOf(0)

    val rendererFrameRate = mutableStateOf(0)

    val focusPeakingEnableFlow = MutableStateFlow(false)

    val brightnessPeakingEnableFlow = MutableStateFlow(false)

    val exposureHistogramEnableFlow = MutableStateFlow(true)

    /**
     *
     * 预览数据
     *
     */

    val captureResultFlow = MutableStateFlow<CaptureResult?>(null)

    fun fetchCurrentSupportedOutput(cameraCharacteristics: CameraCharacteristics) {
        cameraCharacteristics.apply {
            if (outputSupportedMode.isNotEmpty()) {
                currentOutputItemFlow.value = outputSupportedMode[0]
            }
            captureController.apply {
                if (faceDetectModes.isNotEmpty()) {
                    currentFaceDetectModeFlow.value = faceDetectModes.last()
                }
                if (oisAvailable) {
                    oisEnableFlow.value = true
                }
            }
        }
    }

    val surfaceBitmap = mutableStateOf<Bitmap?>(null)

    val exposureHistogramDataFlow = MutableStateFlow<FloatArray?>(null)

    val focusRequestOrientation = mutableStateOf<FocusRequestOrientation?>(null)

    val focusPointRectFlow = MutableStateFlow<androidx.compose.ui.geometry.Rect?>(null)

    /**
     *
     * 曝光控制相关
     *
     */

    val captureController = CaptureController()

    fun setCurrentCaptureParams(builder: CaptureRequest.Builder) =
        captureController.setCurrentCaptureParams(builder)

    fun focusRequest(rect: androidx.compose.ui.geometry.Rect) {
        focusRequestOrientation.value = FocusRequestOrientation(
            pitch = pitchFlow.value,
            roll = rollFlow.value,
            yaw = yawFlow.value,
        )
        focusPointRectFlow.value = rect
        val cameraPair = currentCameraPairFlow.value
        val characteristics = cameraPair?.second
        val flipHorizontal = characteristics?.isFrontCamera == true
        val sensorSize = characteristics?.sensorSize
        if (sensorSize != null) {
            val focusRect = composeRect2SensorRect(
                rect = rect,
                rotationOrientation = rotationOrientation.value,
                flipHorizontal = flipHorizontal,
                sensorWidth = sensorSize.width(),
                sensorHeight = sensorSize.height(),
            )
            captureController.focusRequest(focusRect)
        }
    }

    fun focusCancel() {
        focusRequestOrientation.value = null
        captureController.focusCancel()
        Log.i(TAG, "focusCancel: 取消对焦～")
    }

}