package com.jvziyaoyao.camera.raw.holder.camera

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.ImageFormat
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.CaptureResult
import android.hardware.camera2.TotalCaptureResult
import android.media.Image
import android.media.ImageReader
import android.opengl.GLSurfaceView
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.view.Surface
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.geometry.Rect
import androidx.exifinterface.media.ExifInterface
import com.jvziyaoyao.camera.raw.util.ContextUtil
import com.jvziyaoyao.camera.raw.util.testTime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import org.opencv.android.OpenCVLoader
import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import java.io.File
import java.nio.ByteBuffer
import java.util.concurrent.Executors
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

fun defaultStoragePath(): File {
    val picturesFile =
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).absoluteFile
    val storageFile = File(picturesFile, "Camera")
    if (!storageFile.exists()) storageFile.mkdirs()
    return storageFile
}

typealias ProvideSaveFile = (Long, Int) -> File?

val defaultProvideSaveFile: ProvideSaveFile = { time, format ->
    val ext = when (format) {
        ImageFormat.JPEG, ImageFormat.DEPTH_JPEG -> "JPEG"
        ImageFormat.HEIC -> "HEIC"
        ImageFormat.RAW_SENSOR -> "DNG"
        else -> null
    }
    if (ext == null) null else {
        File(
            defaultStoragePath(),
            "IMG_$time.$ext"
        )
    }
}

val cameraRequirePermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
    listOf(
        Manifest.permission.CAMERA,
        Manifest.permission.READ_MEDIA_IMAGES,
        Manifest.permission.READ_MEDIA_AUDIO,
        Manifest.permission.READ_MEDIA_VIDEO,
    )
} else {
    listOf(
        Manifest.permission.CAMERA,
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.READ_EXTERNAL_STORAGE
    )
}

class CameraFlow(
    val displayRotation: Int = 0,
    val provideSaveFile: ProvideSaveFile = defaultProvideSaveFile,
) : CoroutineScope by MainScope() {

    private val TAG = CameraFlow::class.java.name

    private val context = ContextUtil.getApplicationByReflect()

    private val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager

    private var cameraDeviceFlow = MutableStateFlow<CameraDevice?>(null)

    private var cameraCaptureSessionFlow = MutableStateFlow<CameraCaptureSession?>(null)

    private var glSurfaceViewFlow = MutableStateFlow<GLSurfaceView?>(null)

    private var surfaceFlow = MutableStateFlow<Surface?>(null)

    private val cameraSurfaceRender = CameraSurfaceRenderer(TEX_VERTEX_MAT_0)

    private var imageOutputReader: ImageReader? = null

    private var imagePreviewReader: ImageReader? = null

    private val handlerThread = HandlerThread("CameraHandlerThread").apply { start() }

    private val handler = Handler(handlerThread.looper)

    private val executor = Executors.newSingleThreadExecutor()

    private var frameCount = 0

    private val yuvDataFlow = MutableStateFlow<YUVRenderData?>(null)

    private val grayMatFlow = MutableStateFlow<Mat?>(null)

    private val focusPeakingMatFlow = MutableStateFlow<Mat?>(null)

    private val brightnessPeakingMatFlow = MutableStateFlow<Mat?>(null)

    init {
        // 初始化OpenCV
        OpenCVLoader.initLocal()
    }

//    suspend fun runFlashAE() {
//        val previewSurface = surfaceFlow.value ?: return
//        if (cameraCaptureSessionFlow.value == null) return
//        val cameraDevice = cameraDeviceFlow.value ?: return
//        val cameraCaptureRequest =
//            cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
//                .apply {
//                    addTarget(previewSurface)
//                    setCurrentCaptureParams(false, this)
//                    set(
//                        CaptureRequest.FLASH_MODE,
//                        CaptureRequest.FLASH_MODE_SINGLE,
//                    )
//                    set(
//                        CaptureRequest.CONTROL_AE_MODE,
//                        CaptureRequest.CONTROL_AE_MODE_ON_ALWAYS_FLASH,
//                    )
//                }.build()
//        val captureResult =
//            cameraCaptureSessionFlow.value?.captureAsync(cameraCaptureRequest, handler)
//        Log.i(TAG, "runFlashAE: captureResult $captureResult")
//    }

    suspend fun capture(
        outputFile: File? = null,
        additionalRotation: Int? = null,
    ) {
        if (cameraCaptureSessionFlow.value == null) return
        val cameraDevice = cameraDeviceFlow.value ?: return

        var imageOrientation = rotationOrientation.value
        if (additionalRotation != null) {
            val srcImageOrientation =
                if (imageOrientation < additionalRotation) imageOrientation + 360 else imageOrientation
            imageOrientation = (srcImageOrientation - additionalRotation) % 360
        }

        val cameraCaptureRequest =
            cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)
                .apply {
                    imageOutputReader?.apply { addTarget(surface) }
                    set(CaptureRequest.JPEG_ORIENTATION, imageOrientation)
                    setCurrentCaptureParams(preview = false, trigger = false, builder = this)
                }.build()
        val captureTimestamp = System.currentTimeMillis()
        val awaitResult = coroutineScope {
            listOf(
                async {
                    return@async awaitOutputImage()
                },
                async {
                    return@async cameraCaptureSessionFlow.value?.captureAsync(
                        cameraCaptureRequest,
                        handler
                    )
                },
            ).awaitAll()
        }
        (awaitResult[0] as Image?)?.let { image ->
            val destFile = outputFile
                ?: provideSaveFile(
                    captureTimestamp,
                    image.format
                )
            destFile?.let { file ->
                when (image.format) {
                    ImageFormat.JPEG,
                    ImageFormat.DEPTH_JPEG,
                    ImageFormat.HEIC -> writeImageAsJpeg(image, file)

                    ImageFormat.RAW_SENSOR -> {
                        val cameraPair = currentCameraPairFlow.value
                        val cameraCharacteristics = cameraPair?.second
                        val captureResult = awaitResult[1] as CaptureResult?
                        val exifOrientation = when (imageOrientation) {
                            90 -> ExifInterface.ORIENTATION_ROTATE_90
                            180 -> ExifInterface.ORIENTATION_ROTATE_180
                            270 -> ExifInterface.ORIENTATION_ROTATE_270
                            else -> ExifInterface.ORIENTATION_NORMAL
                        }
                        if (captureResult != null && cameraCharacteristics != null) {
                            writeImageAsDng(
                                image,
                                cameraCharacteristics,
                                captureResult,
                                exifOrientation,
                                file
                            )
                        }
                    }

                    else -> {}
                }
            }
            image.close()
        }
    }

    private suspend fun awaitOutputImage() = suspendCoroutine { c ->
        imageOutputReader?.setOnImageAvailableListener({
            val image = it?.acquireNextImage()
            c.resume(image)
        }, handler)
    }

    fun setSurfaceView(glSurfaceView: GLSurfaceView) {
        glSurfaceViewFlow.value = glSurfaceView
        glSurfaceView.setEGLContextClientVersion(3)
        glSurfaceView.setRenderer(cameraSurfaceRender)
    }

    private val previewCaptureCallback = object : CameraCaptureSession.CaptureCallback() {
        override fun onCaptureCompleted(
            session: CameraCaptureSession,
            request: CaptureRequest,
            result: TotalCaptureResult
        ) {
            super.onCaptureCompleted(session, request, result)
            captureResultFlow.value = result
        }
    }

    private fun setOrientation(cameraCharacteristics: CameraCharacteristics) {
        // 设置预览方向相关
        val sensorSize = cameraCharacteristics.sensorSize
        val cameraFacing = cameraCharacteristics.cameraFacing
        val isFrontCamera = cameraCharacteristics.isFrontCamera
        if (sensorSize != null && cameraFacing != null) {
            val sensorOrientation = cameraCharacteristics.sensorOrientation ?: 90
            Log.i(TAG, "setOrientation: displayRotation.value $displayRotation")
            val rotationOrientationResult = if (isFrontCamera) {
                (sensorOrientation + displayRotation) % 360
            } else {
                (sensorOrientation - displayRotation + 360) % 360
            }
            rotationOrientation.value = rotationOrientationResult
            var nextTextureVertex = when (rotationOrientationResult) {
                0 -> TEX_VERTEX_MAT_0
                90 -> TEX_VERTEX_MAT_90
                180 -> TEX_VERTEX_MAT_180
                270 -> TEX_VERTEX_MAT_270
                else -> TEX_VERTEX_MAT_90
            }
            if (isFrontCamera) nextTextureVertex = vertexHorizontalFlip(nextTextureVertex)
            cameraSurfaceRender.currentYuvData = null
            cameraSurfaceRender.updateTextureBuffer(nextTextureVertex)
        }
    }

    private suspend fun startPreview(
        cameraDevice: CameraDevice,
        cameraCharacteristics: CameraCharacteristics,
        outputItem: OutputItem?,
    ) {
        val scaleStreamConfigurationMap = cameraCharacteristics.scaleStreamMap
        val sensorAspectRatio = cameraCharacteristics.sensorAspectRatio
        val surfaceList = mutableListOf<Surface>()

        if (outputItem != null) {
            imageOutputReader?.close()
            imageOutputReader = ImageReader.newInstance(
                outputItem.bestSize.width,
                outputItem.bestSize.height,
                outputItem.outputMode.imageFormat,
                2
            )
            surfaceList.add(imageOutputReader!!.surface)
        }

        val outputSizeList = scaleStreamConfigurationMap?.getOutputSizes(ImageFormat.YUV_420_888)
        val bestPreviewSize = outputSizeList?.run {
            val aspectList = getSizeByAspectRatio(this, sensorAspectRatio)
            return@run findBestSize(aspectList.toTypedArray(), 1280)
        }
        if (bestPreviewSize != null) {
            Log.i(TAG, "startPreview: bestPreviewSize $bestPreviewSize")
            imagePreviewReader?.close()
            imagePreviewReader = ImageReader.newInstance(
                bestPreviewSize.width,
                bestPreviewSize.height,
                ImageFormat.YUV_420_888,
                2
            )
            imagePreviewReader!!.setOnImageAvailableListener(previewImageAvailableListener, handler)
            surfaceFlow.value = imagePreviewReader!!.surface
            surfaceList.add(imagePreviewReader!!.surface)
        }

        cameraCaptureSessionFlow.value = cameraDevice.createCameraSessionAsync(handler, surfaceList)
    }

    private val previewImageAvailableListener = object : ImageReader.OnImageAvailableListener {
        override fun onImageAvailable(reader: ImageReader?) {
            val image = reader?.acquireNextImage() ?: return
            frameCount++
            val width = image.width
            val height = image.height
            val plans = image.planes
            val y = plans[0].buffer
            val u = plans[1].buffer
            val v = plans[2].buffer
            y.position(0)
            u.position(0)
            v.position(0)

            val yByteBuffer = ByteBuffer.allocateDirect(y.capacity())
            val uByteBuffer = ByteBuffer.allocateDirect(u.capacity())
            val vByteBuffer = ByteBuffer.allocateDirect(v.capacity())
            yByteBuffer.put(y)
            uByteBuffer.put(u)
            vByteBuffer.put(v)
            yByteBuffer.position(0)
            uByteBuffer.position(0)
            vByteBuffer.position(0)

            grayMatFlow.value = Mat(height, width, CvType.CV_8UC1, yByteBuffer)
            yByteBuffer.position(0)
            yuvDataFlow.value = YUVRenderData(
                width = width,
                height = height,
                yByteArray = yByteBuffer,
                uByteArray = uByteBuffer,
                vByteArray = vByteBuffer,
            )
            image.close()
        }
    }

    @SuppressLint("MissingPermission")
    private fun openCamera(cameraPair: CameraPair) {
        Log.i(TAG, "openCamera: cameraPair $cameraPair")
        val cameraCharacteristics = cameraPair.second
        // 获取当前摄像头支持的输出格式
        fetchCurrentSupportedOutput(cameraCharacteristics)
        // 设置预览渲染的旋转角度
        setOrientation(cameraCharacteristics)
        // 实际开启摄像头代码
        val cameraId = cameraPair.first
        cameraManager.openCamera(cameraId, object : CameraDevice.StateCallback() {
            override fun onOpened(camera: CameraDevice) {
                cameraDeviceFlow.value = camera
            }

            override fun onDisconnected(camera: CameraDevice) {
            }

            override fun onError(camera: CameraDevice, error: Int) {
            }

        }, handler)
    }

    private fun closeCamera() {
        imageOutputReader?.close()
        imageOutputReader = null
        cameraCaptureSessionFlow.value?.close()
        cameraCaptureSessionFlow.value = null
        cameraDeviceFlow.value?.close()
        cameraDeviceFlow.value = null
    }

    fun setupCamera() {
        // 获取摄像头列表
        cameraPairListFlow.value = context.fetchCameraPairList()

        // 初始化选择摄像头
        launch(Dispatchers.IO) {
            combine(cameraPairListFlow, currentCameraPairFlow) { t1, t2 ->
                Pair(t1, t2)
            }.collectLatest { _ ->
                val pairList = cameraPairListFlow.value
                val currentCameraPair = currentCameraPairFlow.value
                if (currentCameraPair == null) {
                    currentCameraPairFlow.value = chooseDefaultCameraPair(pairList)
                }
            }
        }

        // 开启当前摄像头
        launch(Dispatchers.IO) {
            combine(
                currentCameraPairFlow,
                resumeTimestampFlow,
                allPermissionGrantedFlow
            ) { t1, t2, t3 -> Triple(t1, t2, t3) }.collectLatest { _ ->
                val cameraPair = currentCameraPairFlow.value
                val allPermissionGrantedFlow = allPermissionGrantedFlow.value
                val resumeTimeStamp = resumeTimestampFlow.value
                if (allPermissionGrantedFlow && resumeTimeStamp != null) {
                    if (cameraDeviceFlow.value != null) {
                        if (cameraDeviceFlow.value?.id != cameraPair?.first) {
                            // 关闭摄像头
                            closeCamera()
                            // 开启摄像头
                            cameraPair?.let { openCamera(it) }
                        }
                    } else {
                        // 开启摄像头
                        cameraPair?.let { openCamera(it) }
                    }
                } else {
                    // 关闭摄像头
                    closeCamera()
                }
            }
        }

        // 开启预览
        launch(Dispatchers.IO) {
            combine(
                cameraDeviceFlow,
                currentOutputItemFlow,
                currentCameraPairFlow,
            ) { t0, t1, t2 ->
                Triple(t0, t1, t2)
            }.collectLatest { t ->
                val cameraDevice = t.first
                val currentOutputItem = t.second
                val cameraPair = t.third
                val cameraCharacteristics = cameraPair?.second
                val cameraId = cameraPair?.first
                if (
                    cameraDevice != null
                    && cameraCharacteristics != null
                    && cameraId == cameraDevice.id
                ) {
                    startPreview(
                        cameraDevice,
                        cameraCharacteristics,
                        currentOutputItem,
                    )
                }
            }
        }

        // 变更预览参数
        launch(Dispatchers.IO) {
            combine(
                cameraCaptureSessionFlow,
                cameraDeviceFlow,
                surfaceFlow,
                captureController.manualSensorParamsFlow,
            ) { t0, t1, t2, t3 ->
                arrayOf(t0, t1, t2, t3)
            }.collectLatest { _ ->
                try {
                    val cameraCaptureSession = cameraCaptureSessionFlow.value
                    val cameraDevice = cameraDeviceFlow.value
                    val previewSurface = surfaceFlow.value
                    val focusRequestTrigger = captureController.focusRequestTriggerFlow.value
                    if (cameraCaptureSession != null && cameraDevice != null && previewSurface != null) {
                        if (focusRequestTrigger?.focusRequest == true) {
                            val captureRequest =
                                cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
                                    .apply {
                                        addTarget(previewSurface)
                                        setCurrentCaptureParams(
                                            preview = true, trigger = false, builder = this
                                        )
                                    }.build()
                            cameraCaptureSession.setRepeatingRequest(
                                captureRequest,
                                previewCaptureCallback,
                                handler
                            )

                            val triggerCaptureRequest =
                                cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
                                    .apply {
                                        addTarget(previewSurface)
                                        setCurrentCaptureParams(
                                            preview = true, trigger = true, builder = this
                                        )
                                    }.build()
                            cameraCaptureSession.captureBurst(
                                listOf(triggerCaptureRequest),
                                previewCaptureCallback,
                                handler
                            )
                            focusRequestTrigger.focusRequest = false
                        } else if (focusRequestTrigger?.focusCancel == true) {
                            val triggerCaptureRequest =
                                cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
                                    .apply {
                                        addTarget(previewSurface)
                                        setCurrentCaptureParams(
                                            preview = true, trigger = true, builder = this
                                        )
                                    }.build()
                            cameraCaptureSession.captureBurst(
                                listOf(triggerCaptureRequest),
                                previewCaptureCallback,
                                handler,
                            )

                            val captureRequest =
                                cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
                                    .apply {
                                        addTarget(previewSurface)
                                        setCurrentCaptureParams(
                                            preview = true, trigger = false, builder = this
                                        )
                                    }.build()
                            cameraCaptureSession.setRepeatingRequest(
                                captureRequest,
                                previewCaptureCallback,
                                handler
                            )

                            focusRequestTrigger.focusCancel = false
                        } else {
                            val captureRequest =
                                cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
                                    .apply {
                                        addTarget(previewSurface)
                                        setCurrentCaptureParams(
                                            preview = true, trigger = false, builder = this
                                        )
                                    }.build()
                            cameraCaptureSession.setRepeatingRequest(
                                captureRequest,
                                previewCaptureCallback,
                                handler
                            )
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        // 计算帧率
        launch(Dispatchers.IO) {
            var job: Job? = null
            resumeTimestampFlow.collectLatest {
                launch {
                    val resumeTimestamp = resumeTimestampFlow.value
                    if (resumeTimestamp != null) {
                        job?.cancel()
                        job = launch(Dispatchers.IO) {
                            var currentCount = frameCount
                            var currentRenderCount = cameraSurfaceRender.frameCount
                            while (resumeTimestampFlow.value != null) {
                                delay(1000)
                                Log.i(
                                    TAG,
                                    "onCreate: calc frame 计算帧率： $frameCount - ${cameraSurfaceRender.frameCount}"
                                )
                                captureFrameRate.value = frameCount - currentCount
                                rendererFrameRate.value =
                                    cameraSurfaceRender.frameCount - currentRenderCount
                                currentCount = frameCount
                                currentRenderCount = cameraSurfaceRender.frameCount
                            }
                        }
                    } else {
                        job = null
                    }
                }
            }
        }

        // 填充预览yuv数据
        launch(Dispatchers.IO) {
            yuvDataFlow.collectLatest {
                cameraSurfaceRender.currentYuvData = it
            }
        }

        // 填充直方图数据
        launch(Dispatchers.IO) {
            combine(grayMatFlow, exposureHistogramEnableFlow) { t01, t02 ->
                arrayOf(t01, t02)
            }.collectLatest {
                val grayMat = grayMatFlow.value
                val histogram = exposureHistogramEnableFlow.value
                exposureHistogramDataFlow.value =
                    if (histogram && grayMat != null) getHistogramData(grayMat) else null
            }
        }

        // 填充峰值亮度数据
        launch(Dispatchers.IO) {
            val zebraOffsetArr = arrayOf(0).toIntArray()
            combine(grayMatFlow, brightnessPeakingEnableFlow) { t01, t02 ->
                arrayOf(t01, t02)
            }.collectLatest {
                val grayMat = grayMatFlow.value
                val enable = brightnessPeakingEnableFlow.value
                brightnessPeakingMatFlow.value =
                    if (grayMat != null && enable) markOverExposedRegions(
                        zebraOffsetArr,
                        grayMat
                    ) else null
            }
        }

        // 填充峰值对焦数据
        launch(Dispatchers.IO) {
            combine(grayMatFlow, focusPeakingEnableFlow) { t01, t02 ->
                arrayOf(t01, t02)
            }.collectLatest {
                val time = testTime {
                    val grayMat = grayMatFlow.value
                    val enable = focusPeakingEnableFlow.value
                    focusPeakingMatFlow.value =
                        if (grayMat != null && enable) {
                            markShapeImageRegions(grayMat)
                                .apply { preMultiplyAlpha() }
                        } else null
                }
//                Log.i(TAG, "onCreate: focusPeakingMat time -> $time")
            }
        }

        // 附加图层合并填充
        launch(Dispatchers.IO) {
            combine(brightnessPeakingMatFlow, focusPeakingMatFlow) { t01, t02 ->
                arrayOf(t01, t02)
            }.collectLatest {
                val brightnessPeakingMat = brightnessPeakingMatFlow.value
                val focusPeakingMat = focusPeakingMatFlow.value
                cameraSurfaceRender.currentAdditionalMat =
                    if (brightnessPeakingMat != null && focusPeakingMat != null) {
                        Mat(brightnessPeakingMat.size(), CvType.CV_8UC4).apply {
                            Core.addWeighted(
                                brightnessPeakingMat,
                                1.0,
                                focusPeakingMat,
                                1.0,
                                0.0,
                                this
                            )
                        }
                    } else brightnessPeakingMat ?: focusPeakingMat
            }
        }
    }

    fun releaseCamera() {
        cancel()
    }

    fun onResume() {
        resumeTimestampFlow.value = System.currentTimeMillis()
    }

    fun onPause() {
        Log.i(TAG, "onPause: ~")
        resumeTimestampFlow.value = null
    }

    /**
     *
     * ----------------------------------------------------------------------------------------->
     *
     */

    /**
     *
     * 影响相机启动相关
     *
     */

    private val resumeTimestampFlow = MutableStateFlow<Long?>(null)

    val rotationOrientation = mutableStateOf(0)

    val cameraPairListFlow =
        MutableStateFlow(emptyList<Pair<String, CameraCharacteristics>>())

    // TODO 不对外暴露
    val currentCameraPairFlow = MutableStateFlow<Pair<String, CameraCharacteristics>?>(null)

    val currentOutputItemFlow = MutableStateFlow<OutputItem?>(null)

    val allPermissionGrantedFlow = MutableStateFlow(false)

    fun onPermissionChanged(allGranted: Boolean) {
        allPermissionGrantedFlow.value = allGranted
    }

    /**
     *
     *
     *
     */

    val captureFrameRate = mutableStateOf(0)

    val rendererFrameRate = mutableStateOf(0)

    val focusPeakingEnableFlow = MutableStateFlow(false)

    val brightnessPeakingEnableFlow = MutableStateFlow(false)

    val exposureHistogramEnableFlow = MutableStateFlow(true)

    val exposureHistogramDataFlow = MutableStateFlow<FloatArray?>(null)

    /**
     *
     * 预览数据
     *
     */

    val captureResultFlow = MutableStateFlow<CaptureResult?>(null)

    private fun fetchCurrentSupportedOutput(cameraCharacteristics: CameraCharacteristics) {
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

    /**
     *
     * 曝光控制相关
     *
     */

    val captureController = CaptureController()

    private fun setCurrentCaptureParams(
        preview: Boolean,
        trigger: Boolean,
        builder: CaptureRequest.Builder
    ) = captureController.setCurrentCaptureParams(preview, trigger, builder)

    fun focusRequest(rect: Rect) {
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
        captureController.focusCancel()
    }

}