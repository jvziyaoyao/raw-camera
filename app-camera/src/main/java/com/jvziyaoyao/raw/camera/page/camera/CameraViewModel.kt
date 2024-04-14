package com.jvziyaoyao.raw.camera.page.camera

import android.hardware.camera2.CameraMetadata
import android.opengl.GLSurfaceView
import android.os.Environment
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.geometry.Rect
import androidx.lifecycle.ViewModel
import com.jvziyaoyao.camera.raw.holder.camera.CameraHolder
import com.jvziyaoyao.camera.raw.holder.camera.chooseDefaultCameraPair
import com.jvziyaoyao.camera.raw.holder.camera.isFrontCamera
import com.jvziyaoyao.camera.raw.holder.sensor.SensorHolder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import java.io.File

class CameraViewModel : ViewModel() {

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

    val currentCameraCharacteristicsFlow
        get() = currentCameraPairFlow.map { it?.second }

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

    suspend fun capture() {
        val outputItem = cameraHolder.currentOutputItemFlow.value ?: return
        val extName = outputItem.outputMode.extName
        val time = System.currentTimeMillis()
        val outputFile = File(getStoragePath(), "YAO_$time.$extName")
        cameraHolder.capture(
            outputFile,
            additionalRotation = saveImageOrientation.value,
        )
    }

    fun focusCancel() = cameraHolder.focusCancel()

    fun focusRequest(rect: Rect) = cameraHolder.focusRequest(rect)

    fun resumeCamera() = cameraHolder.onResume()

    fun pauseCamera() = cameraHolder.onPause()

    /**
     *
     * UI相关
     *
     */

    val captureLoading = mutableStateOf(false)

//    val focusRequestOrientation = mutableStateOf<FocusRequestOrientation?>(null)

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