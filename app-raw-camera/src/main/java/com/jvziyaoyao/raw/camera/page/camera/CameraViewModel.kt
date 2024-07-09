package com.jvziyaoyao.raw.camera.page.camera

import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.YuvImage
import android.hardware.camera2.CameraMetadata
import android.opengl.GLSurfaceView
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.geometry.Rect
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jvziyaoyao.camera.flow.holder.camera.CameraFlow
import com.jvziyaoyao.camera.flow.holder.camera.OutputMode
import com.jvziyaoyao.camera.flow.holder.camera.chooseDefaultCameraPair
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
import com.jvziyaoyao.raw.camera.domain.model.MediaQueryEntity
import com.jvziyaoyao.raw.camera.domain.repository.ImageRepo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.opencv.core.Mat
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.ByteBuffer

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

class CameraViewModel(
    private val imageRepo: ImageRepo,
) : ViewModel() {


    /**
     *
     * 启动相关
     *
     */

    private val resumeTimestampFlow = MutableStateFlow<Long?>(null)

    private val cameraResumeTimestampFlow = MutableStateFlow<Long?>(null)

    fun resume() {
        resumeTimestampFlow.value = System.currentTimeMillis()
    }

    fun pause() {
        resumeTimestampFlow.value = null
    }

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

    val rotationOrientation
        get() = cameraFlow.rotationOrientationFlow

    val focusRequestTriggerFlow
        get() = captureController.focusRequestTriggerFlow

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

        viewModelScope.launch {
            combine(
                rotationOrientation,
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

        // 控制Camera启停
        viewModelScope.launch {
            combine(
                resumeTimestampFlow,
                previewerVisibleTargetFlow
            ) { p01, p02 -> Pair(p01, p02) }.collectLatest { pair ->
                val (resumeTimestamp, visibleTarget) = pair
                if (resumeTimestamp != null) {
                    if (visibleTarget == true) {
                        cameraResumeTimestampFlow.value = null
                    } else if (visibleTarget == false) {
                        cameraResumeTimestampFlow.value = resumeTimestamp
                    } else if (cameraResumeTimestampFlow.value == null && previewerVisibleFlow.value != true) {
                        cameraResumeTimestampFlow.value = resumeTimestamp
                    }
                } else {
                    cameraResumeTimestampFlow.value = null
                }
            }
        }

        // 控制Camera启停
        viewModelScope.launch {
            cameraResumeTimestampFlow.collectLatest {
                if (it != null) {
                    startSensor()
                    resumeCamera()
                } else {
                    stopSensor()
                    pauseCamera()
                }
            }
        }

        // 传图给滤镜列表
        viewModelScope.launch(Dispatchers.IO) {
            yuvCameraRenderer.yuvDataFlow.collectLatest { yuvData ->
                if (showFilterList.value) {
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

        // 除了JPEG意外其他模式不支持滤镜
        viewModelScope.launch(Dispatchers.IO) {
            currentOutputItemFlow.collectLatest {
                if (it?.outputMode != OutputMode.JPEG) {
                    currentImageFilterFlow.value = imageFilterList.first().shaderStr
                    showFilterList.value = false
                }
            }
        }

        // 相机切换回自动模式后设置曝光参数为自动
        viewModelScope.launch(Dispatchers.IO) {
            pictureModeFlow.collectLatest {
                captureController.zoomRatioFlow.value = 1F
                if (it != PictureMode.Manual) {
                    captureController.sensorExposureTimeFlow.value = null
                    captureController.sensorSensitivityFlow.value = null
                    captureController.focalDistanceFlow.value = null
                    captureController.customTemperatureFlow.value = null
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

    suspend fun capture() {
        val context = ContextUtil.getApplicationByReflect()
        val outputItem = cameraFlow.currentOutputItemFlow.value ?: return
        val extName = outputItem.outputMode.extName
        val time = System.currentTimeMillis()
        val name = "YAO_$time"
        val outputFile = File(storagePath, "$name.$extName")
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

        // 刷新图片
        fetchImages()
    }

    fun focusCancel() {
        // 曝光补偿设置回 0
        captureController.aeCompensationFlow.value = 0
        // 取消对焦
        cameraFlow.focusCancel()
    }

    fun focusRequest(rect: Rect) {
        // 曝光补偿设置回 0
        captureController.aeCompensationFlow.value = 0
        // 开始对焦
        cameraFlow.focusRequest(rect)
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

    val showFilterList = mutableStateOf(false)

    val textureVertexFlow = MutableStateFlow<FloatArray?>(null)

    val filterRendererMatFlow = MutableStateFlow<Mat?>(null)

    val imageFilterList by lazy { defaultImageFilterList }

    val currentImageFilterFlow
        get() = yuvCameraRenderer.currentImageFilterFlow

    /**
     *
     * UI相关
     *
     */

    val pictureModeFlow = MutableStateFlow(PictureMode.Normal)

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
                chooseDefaultCameraPair(
                    cameraPairList,
                    CameraMetadata.LENS_FACING_BACK
                )
            } else {
                chooseDefaultCameraPair(
                    cameraPairList,
                    CameraMetadata.LENS_FACING_FRONT
                )
            }
            if (nextCameraPair != null) {
                currentCameraPairFlow.value = nextCameraPair
            }
        }
    }

    /**
     *
     * 图片预览相关
     *
     */

    private val storagePath
        get() = imageRepo.storagePath

    val imageList
        get() = imageRepo.imageList

    val previewerVisibleFlow = MutableStateFlow<Boolean?>(false)

    val previewerVisibleTargetFlow = MutableStateFlow<Boolean?>(false)

    fun fetchImages() = imageRepo.fetchImages()

    fun deleteImage(mediaQueryEntity: MediaQueryEntity) = imageRepo.deleteImage(mediaQueryEntity)

}