package com.jvziyaoyao.raw.sample.page.main

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.opengl.GLSurfaceView
import android.os.Environment
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.geometry.Rect
import androidx.lifecycle.ViewModel
import com.jvziyaoyao.camera.raw.holder.camera.CameraFlow
import com.jvziyaoyao.camera.raw.holder.camera.off.getGLFilterBitmapAsync
import com.jvziyaoyao.camera.raw.holder.sensor.SensorFlow
import com.jvziyaoyao.raw.sample.R
import kotlinx.coroutines.flow.MutableStateFlow
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

    suspend fun onCapture() {
        val outputItem = cameraFlow.currentOutputItemFlow.value ?: return
        val extName = outputItem.outputMode.extName
        val time = System.currentTimeMillis()
        val outputFile = File(getStoragePath(), "YAO_$time.$extName")
        cameraFlow.capture(
            outputFile,
            additionalRotation = saveImageOrientation.value,
        )
    }

    val focusRequestTriggerFlow
        get() = captureController.focusRequestTriggerFlow

    fun focusCancel() {
        cameraFlow.focusCancel()
    }

    fun focusRequest(rect: Rect) {
        focusRequestOrientation.value = FocusRequestOrientation(
            pitch = pitchFlow.value,
            roll = rollFlow.value,
            yaw = yawFlow.value,
        )
        focusPointRectFlow.value = rect
        cameraFlow.focusRequest(rect)
    }

    suspend fun focusRequestAsync(rect: Rect) {
        focusRequestOrientation.value = FocusRequestOrientation(
            pitch = pitchFlow.value,
            roll = rollFlow.value,
            yaw = yawFlow.value,
        )
        focusPointRectFlow.value = rect
        cameraFlow.focusRequestAsync(rect)
    }

    fun resumeCamera() = cameraFlow.onResume()

    fun pauseCamera() = cameraFlow.onPause()

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

    val testBitmap = mutableStateOf<Bitmap?>(null)

    suspend fun getTestBitmap(context: Context) {
        val bitmap = BitmapFactory.decodeResource(context.resources, R.drawable.scene)
        testBitmap.value = getGLFilterBitmapAsync(context, bitmap)
    }

}