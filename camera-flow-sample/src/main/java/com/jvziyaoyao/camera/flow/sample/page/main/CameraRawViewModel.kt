package com.jvziyaoyao.camera.flow.sample.page.main

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.YuvImage
import android.opengl.GLSurfaceView
import android.os.Environment
import android.os.Handler
import android.os.HandlerThread
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.geometry.Rect
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jvziyaoyao.camera.flow.holder.camera.CameraFlow
import com.jvziyaoyao.camera.flow.holder.camera.OutputMode
import com.jvziyaoyao.camera.flow.holder.camera.filter.defaultImageFilterList
import com.jvziyaoyao.camera.flow.holder.camera.isFrontCamera
import com.jvziyaoyao.camera.flow.holder.camera.off.getGLFilterBitmapAsync
import com.jvziyaoyao.camera.flow.holder.camera.render.YuvCameraPreviewer
import com.jvziyaoyao.camera.flow.holder.camera.render.YuvCameraRenderer
import com.jvziyaoyao.camera.flow.holder.camera.render.getCameraFacingVertex
import com.jvziyaoyao.camera.flow.holder.camera.render.isEmptyImageFilter
import com.jvziyaoyao.camera.flow.holder.camera.resizeMat
import com.jvziyaoyao.camera.flow.holder.camera.toMat
import com.jvziyaoyao.camera.flow.holder.sensor.SensorFlow
import com.jvziyaoyao.camera.flow.util.ContextUtil
import com.jvziyaoyao.camera.flow.util.saveBitmapWithExif
import com.jvziyaoyao.camera.flow.sample.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import org.opencv.core.Mat
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.ByteBuffer

enum class CaptureMode {
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

    private val handlerThread = HandlerThread("CameraHandlerThread").apply { start() }

    private val handler = Handler(handlerThread.looper)

    private lateinit var cameraFlow: CameraFlow

    private lateinit var yuvCameraRenderer: YuvCameraRenderer

    private lateinit var cameraPreviewer: YuvCameraPreviewer

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
        get() = yuvCameraRenderer.exposureHistogramDataFlow

    val focusPeakingEnableFlow
        get() = yuvCameraRenderer.focusPeakingEnableFlow

    val brightnessPeakingEnableFlow
        get() = yuvCameraRenderer.brightnessPeakingEnableFlow

    val exposureHistogramEnableFlow
        get() = yuvCameraRenderer.exposureHistogramEnableFlow

    // 性能相关

    val captureFrameRate
        get() = yuvCameraRenderer.captureFrameRate

    val rendererFrameRate
        get() = yuvCameraRenderer.rendererFrameRate

    // 屏幕旋转

    val displayRotation
        get() = cameraFlow.displayRotation

    val rotationOrientationFlow
        get() = cameraFlow.rotationOrientationFlow

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
        yuvCameraRenderer = YuvCameraRenderer()
        cameraPreviewer = YuvCameraPreviewer { image ->
            yuvCameraRenderer.processImage(image)
        }
        cameraFlow = CameraFlow(
            handler = handler,
            displayRotation = displayRotation,
            getPreviewSurface = { size ->
                cameraPreviewer.getPreviewSurface(size, handler)
            }
        )

        cameraFlow.setupCamera()
        yuvCameraRenderer.setupRenderer()

        // 更新画面渲染顶点
        viewModelScope.launch(Dispatchers.IO) {
            combine(
                rotationOrientationFlow,
                // 等相机启动有画面了再设置顶点
                cameraFlow.cameraDeviceFlow,
            ) { p0, p1 -> Pair(p0, p1) }.collectLatest { pair ->
                val rotationOrientation = pair.first
                val currentCameraPair = cameraFlow.currentCameraPairFlow.value
                if (currentCameraPair != null) {
                    val isFrontCamera = currentCameraPair.second.isFrontCamera
                    val nextTextureVertex =
                        getCameraFacingVertex(isFrontCamera, rotationOrientation)
                    yuvCameraRenderer.updateVertex(nextTextureVertex)

                    filterRendererMatFlow.value = null
                    textureVertexFlow.value = nextTextureVertex
                }
            }
        }

        // 传递滤镜预览图
        viewModelScope.launch(Dispatchers.IO) {
            yuvCameraRenderer.yuvDataFlow.collectLatest { yuvData ->
                yuvData?.let {
                    val yByteArray = it.yByteArray.duplicate()
                    val vByteArray = it.vByteArray.duplicate()
                    yByteArray.clear()
                    vByteArray.clear()
                    val width = it.width
                    val height = it.height

                    val buffer = ByteBuffer.allocate(
                        yByteArray.remaining() + vByteArray.remaining()
                    )
                    buffer.put(yByteArray)
                    buffer.put(vByteArray)
                    val yuvImage =
                        YuvImage(buffer.array(), ImageFormat.NV21, width, height, null)
                    val out = ByteArrayOutputStream()
                    yuvImage.compressToJpeg(
                        android.graphics.Rect(0, 0, width, height), 50, out
                    )
                    val imageBytes = out.toByteArray()
                    val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                    val mat = bitmap.toMat()
                    filterRendererMatFlow.value =
                        resizeMat(mat, mat.width().div(4), mat.height().div(4))
                }
            }
        }
    }

    fun releaseCamera() {
        cameraFlow.release()
        yuvCameraRenderer.release()
    }

    fun setSurfaceView(glSurfaceView: GLSurfaceView) =
        yuvCameraRenderer.setSurfaceView(glSurfaceView)

    suspend fun onCapture() {
        val context = ContextUtil.getApplicationByReflect()
        val outputItem = cameraFlow.currentOutputItemFlow.value ?: return
        val extName = outputItem.outputMode.extName
        val time = System.currentTimeMillis()
        val outputFile = File(getStoragePath(), "YAO_$time.$extName")
        cameraFlow.capture(
            outputFile,
            additionalRotation = saveImageOrientation.value,
        )
        // 滤镜当前仅支持JPEG
        if (outputItem.outputMode == OutputMode.JPEG) {
            val imageFilter = currentImageFilterFlow.value
            if (!imageFilter.isEmptyImageFilter()) {
                saveBitmapWithExif(outputFile, outputFile) { bitmap ->
                    getGLFilterBitmapAsync(context, imageFilter!!, bitmap)
                }
            }
        }
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

    fun resumeCamera() {
        cameraFlow.resume()
        yuvCameraRenderer.resume()
    }

    fun pauseCamera() {
        cameraFlow.pause()
        yuvCameraRenderer.pause()
    }

    /**
     *
     * 滤镜相关
     *
     */

    val filterRendererMatFlow = MutableStateFlow<Mat?>(null)

    val textureVertexFlow = MutableStateFlow<FloatArray?>(null)

    val imageFilterList by lazy { defaultImageFilterList }

    val currentImageFilterFlow
        get() = yuvCameraRenderer.currentImageFilterFlow

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
        val imageFilterStr = currentImageFilterFlow.value
        testBitmap.value = if (imageFilterStr.isNullOrEmpty()) bitmap else {
            getGLFilterBitmapAsync(context, imageFilterStr, bitmap)
        }
    }

}