package com.jvziyaoyao.raw.sample.page.main

import android.opengl.GLSurfaceView
import android.os.Environment
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.geometry.Rect
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jvziyaoyao.camera.raw.holder.camera.CameraHolder
import com.jvziyaoyao.camera.raw.holder.sensor.SensorHolder
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import java.io.File

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


class CameraRawViewModel : ViewModel() {

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
     * 相机相关
     *
     */

    private lateinit var cameraHolder: CameraHolder

    val currentCameraPairFlow
        get() = cameraHolder.currentCameraPairFlow

    val cameraPairListFlow
        get() = cameraHolder.cameraPairListFlow

    val currentOutputItemFlow
        get() = cameraHolder.currentOutputItemFlow

    // 拍摄控制

    val captureController
        get() = cameraHolder.captureController

    val captureResultFlow
        get() = cameraHolder.captureResultFlow

    // 画面渲染

    val exposureHistogramDataFlow
        get() = cameraHolder.exposureHistogramDataFlow

    val focusPeakingEnableFlow
        get() = cameraHolder.focusPeakingEnableFlow

    val brightnessPeakingEnableFlow
        get() = cameraHolder.brightnessPeakingEnableFlow

    val exposureHistogramEnableFlow
        get() = cameraHolder.exposureHistogramEnableFlow

    // 性能相关

    val captureFrameRate
        get() = cameraHolder.captureFrameRate

    val rendererFrameRate
        get() = cameraHolder.rendererFrameRate

    // 屏幕旋转

    val displayRotation
        get() = cameraHolder.displayRotation

    val rotationOrientation
        get() = cameraHolder.rotationOrientation

    private fun getStoragePath(): File {
        val picturesFile =
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).absoluteFile
        val storageFile = File(picturesFile, "yao")
        if (!storageFile.exists()) storageFile.mkdirs()
        return storageFile
    }

    fun onPermissionChanged(allGranted: Boolean) = cameraHolder.onPermissionChanged(allGranted)

    fun setupCamera(
        displayRotation: Int,
    ) {
        cameraHolder = CameraHolder(displayRotation = displayRotation)
        cameraHolder.setupCamera()
    }

    fun releaseCamera() {
        cameraHolder.releaseCamera()
    }

    fun setSurfaceView(glSurfaceView: GLSurfaceView) = cameraHolder.setSurfaceView(glSurfaceView)

    suspend fun onCapture() {
        val outputItem = cameraHolder.currentOutputItemFlow.value ?: return
        val extName = outputItem.outputMode.extName
        val time = System.currentTimeMillis()
        val outputFile = File(getStoragePath(), "YAO_$time.$extName")
        cameraHolder.capture(
            outputFile,
            additionalRotation = saveImageOrientation.value,
        )
    }

    val focusRequestTriggerFlow
        get() = captureController.focusRequestTriggerFlow

    fun focusCancel() {
        cameraHolder.focusCancel()
        viewModelScope.launch {
            delay(2000L)
            if (focusRequestTriggerFlow.value?.focusCancel == true) {
                focusIdle()
            }
        }
    }

    fun focusIdle() {
        cameraHolder.focusIdle()
        focusRequestOrientation.value = null
    }

    fun focusRequest(rect: Rect) {
        focusRequestOrientation.value = FocusRequestOrientation(
            pitch = pitchFlow.value,
            roll = rollFlow.value,
            yaw = yawFlow.value,
        )
        focusPointRectFlow.value = rect
        cameraHolder.focusRequest(rect)
    }

    fun resumeCamera() = cameraHolder.onResume()

    fun pauseCamera() = cameraHolder.onPause()

    /**
     *
     * UI相关
     *
     */

    val captureModeFlow = MutableStateFlow(CaptureMode.MANUAL)

    val captureLoading = mutableStateOf(false)

    val focusRequestOrientation = mutableStateOf<FocusRequestOrientation?>(null)

    val focusPointRectFlow = MutableStateFlow<Rect?>(null)

    val saveImageOrientation = mutableStateOf(0)

}