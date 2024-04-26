package com.jvziyaoyao.raw.camera.page.camera

import android.hardware.camera2.CameraMetadata
import android.opengl.GLSurfaceView
import android.os.Environment
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.geometry.Rect
import androidx.lifecycle.ViewModel
import com.jvziyaoyao.camera.raw.holder.camera.CameraFlow
import com.jvziyaoyao.camera.raw.holder.camera.chooseDefaultCameraPair
import com.jvziyaoyao.camera.raw.holder.camera.isFrontCamera
import com.jvziyaoyao.camera.raw.holder.sensor.SensorFlow
import kotlinx.coroutines.flow.map
import java.io.File

enum class PictureMode(
    val label: String,
) {
    Normal(
        label = "拍照",
    ),
    Manual(
        label = "手动",
    ),
    ;
}

class CameraViewModel : ViewModel() {

    /**
     *
     * 姿态传感器
     *
     */

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

    /**
     *
     * 相机相关
     *
     */

    private lateinit var cameraFlow: CameraFlow


    val currentCameraPairFlow
        get() = cameraFlow.currentCameraPairFlow

    val currentCameraCharacteristicsFlow
        get() = currentCameraPairFlow.map { it?.second }

    val cameraPairListFlow
        get() = cameraFlow.cameraPairListFlow

    val currentOutputItemFlow
        get() = cameraFlow.currentOutputItemFlow

    // 拍摄控制

    val captureController
        get() = cameraFlow.captureController

    val flashLightFlow
        get() = cameraFlow.flashLightFlow

    val captureResultFlow
        get() = cameraFlow.captureResultFlow

    // 画面渲染

    val exposureHistogramDataFlow
        get() = cameraFlow.exposureHistogramDataFlow

    val focusPeakingEnableFlow
        get() = cameraFlow.focusPeakingEnableFlow

    val brightnessPeakingEnableFlow
        get() = cameraFlow.brightnessPeakingEnableFlow

    val exposureHistogramEnableFlow
        get() = cameraFlow.exposureHistogramEnableFlow

    // 性能相关

    val captureFrameRate
        get() = cameraFlow.captureFrameRate

    val rendererFrameRate
        get() = cameraFlow.rendererFrameRate

    // 屏幕旋转

    val displayRotation
        get() = cameraFlow.displayRotation

    val rotationOrientation
        get() = cameraFlow.rotationOrientation

    val focusRequestTriggerFlow
        get() = captureController.focusRequestTriggerFlow

    private fun getStoragePath(): File {
        val picturesFile =
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).absoluteFile
        val storageFile = File(picturesFile, "yao")
        if (!storageFile.exists()) storageFile.mkdirs()
        return storageFile
    }

    fun onPermissionChanged(allGranted: Boolean) = cameraFlow.onPermissionChanged(allGranted)

    fun setupCamera(
        displayRotation: Int,
    ) {
        cameraFlow = CameraFlow(displayRotation = displayRotation)
        cameraFlow.setupCamera()
    }

    fun releaseCamera() {
        cameraFlow.releaseCamera()
    }

    fun setSurfaceView(glSurfaceView: GLSurfaceView) = cameraFlow.setSurfaceView(glSurfaceView)

    suspend fun capture() {
        val outputItem = cameraFlow.currentOutputItemFlow.value ?: return
        val extName = outputItem.outputMode.extName
        val time = System.currentTimeMillis()
        val outputFile = File(getStoragePath(), "YAO_$time.$extName")
        cameraFlow.capture(
            outputFile,
            additionalRotation = saveImageOrientation.value,
        )
    }

    fun focusCancel() = cameraFlow.focusCancel()

    fun focusRequest(rect: Rect) = cameraFlow.focusRequest(rect)

    fun resumeCamera() = cameraFlow.onResume()

    fun pauseCamera() = cameraFlow.onPause()

    /**
     *
     * UI相关
     *
     */

    val pictureMode = mutableStateOf(PictureMode.Manual)

    val captureLoading = mutableStateOf(false)

    val saveImageOrientation = mutableStateOf(0)

    val showCameraSetting = mutableStateOf(false)

    val gridEnable = mutableStateOf(true)

    val levelIndicatorEnable = mutableStateOf(true)

    fun showSetting() {
        showCameraSetting.value = true
    }

    fun switchCamera() {
        val cameraPair = currentCameraPairFlow.value
        val cameraCharacteristics = cameraPair?.second
        val cameraPairList = cameraPairListFlow.value
        if (cameraCharacteristics == null) {
            if (cameraPairList.isNotEmpty()) {
                currentCameraPairFlow.value = cameraPairList.first()
            }
        } else {
            val nextCameraPair = if (cameraCharacteristics.isFrontCamera) {
                chooseDefaultCameraPair(cameraPairList, CameraMetadata.LENS_FACING_BACK)
            } else {
                chooseDefaultCameraPair(cameraPairList, CameraMetadata.LENS_FACING_FRONT)
            }
            if (nextCameraPair != null) {
                currentCameraPairFlow.value = nextCameraPair
            }
        }
    }

}