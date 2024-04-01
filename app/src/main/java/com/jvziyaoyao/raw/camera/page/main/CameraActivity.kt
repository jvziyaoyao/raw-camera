package com.jvziyaoyao.raw.camera.page.main

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.ImageFormat
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCaptureSession.CaptureCallback
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CameraMetadata
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.CaptureResult
import android.hardware.camera2.DngCreator
import android.hardware.camera2.TotalCaptureResult
import android.media.ImageReader
import android.opengl.GLSurfaceView
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.view.Surface
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.exifinterface.media.ExifInterface
import com.jvziyaoyao.raw.camera.ui.base.CommonPermissions
import com.jvziyaoyao.raw.camera.ui.base.animateRotationAsState
import com.jvziyaoyao.raw.camera.ui.theme.Layout
import com.jvziyaoyao.raw.camera.util.testTime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.opencv.android.OpenCVLoader
import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import kotlin.math.absoluteValue


/**
 * @program: TestFocusable
 *
 * @description:
 *
 * @author: JVZIYAOYAO
 *
 * @create: 2024-01-08 17:36
 **/
class CameraActivity : ComponentActivity(), CoroutineScope by MainScope() {

    private val TAG = CameraActivity::class.java.name

    private val mViewModel by viewModel<CameraViewModel>()

    private var cameraDeviceFlow = MutableStateFlow<CameraDevice?>(null)

    private var cameraCaptureSessionFlow = MutableStateFlow<CameraCaptureSession?>(null)

    private var glSurfaceViewFlow = MutableStateFlow<GLSurfaceView?>(null)

    private var surfaceFlow = MutableStateFlow<Surface?>(null)

    private val cameraSurfaceRender = CameraSurfaceRenderer(TEX_VERTEX_MAT_0)

    private var imageOutputReader: ImageReader? = null

    private var imagePreviewReader: ImageReader? = null

    private var captureResult: CaptureResult? = null

    private var captureTimestamp: Long? = null

    private val handlerThread = HandlerThread("CameraHandlerThread").apply { start() }

    private val handler = Handler(handlerThread.looper)

    private val displayRotation: Int
        get() = when (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            display
        } else {
            windowManager.defaultDisplay
        }?.rotation) {
            Surface.ROTATION_90 -> 90
            Surface.ROTATION_180 -> 180
            Surface.ROTATION_270 -> 270
            else -> 0
        }

    private var frameCount = 0

    private val yuvDataFlow = MutableStateFlow<YUVRenderData?>(null)

    private val grayMatFlow = MutableStateFlow<Mat?>(null)

    private val focusPeakingMatFlow = MutableStateFlow<Mat?>(null)

    private val brightnessPeakingMatFlow = MutableStateFlow<Mat?>(null)

    private val outputFileFlow = MutableSharedFlow<File?>(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 初始化OpenCV
        OpenCVLoader.initLocal()
        // 获取摄像头列表
        // 必须先在activity中执行这一行，以确保UI中不会自己创建一个新的viewModel
        mViewModel.displayRotation.value = displayRotation
        mViewModel.cameraPairListFlow.value = fetchCameraPairList()

        setContent {
            CommonPermissions(
                permissions = camera2Permissions,
                onPermissionChange = {
                    mViewModel.allPermissionGrantedFlow.value = it
                }
            ) {
                Camera2Body(
                    onGLSurfaceView = { glSurfaceView ->
                        glSurfaceViewFlow.value = glSurfaceView
                        glSurfaceView.setEGLContextClientVersion(3)
                        glSurfaceView.setRenderer(cameraSurfaceRender)
                    },
                    onCapture = {
                        launch {
                            mViewModel.captureLoading.value = true
                            try {
                                onCapture()
                            } catch (e: Exception) {
                                e.printStackTrace()
                            } finally {
                                mViewModel.captureLoading.value = false
                            }
                        }
                    },
                )
            }
        }

        // 初始化选择摄像头
        launch {
            combine(mViewModel.cameraPairListFlow, mViewModel.currentCameraPairFlow) { t1, t2 ->
                Pair(t1, t2)
            }.collectLatest { t ->
                val pairList = mViewModel.cameraPairListFlow.value
                val currentCameraPair = mViewModel.currentCameraPairFlow.value
                if (currentCameraPair == null) {
                    mViewModel.currentCameraPairFlow.value = chooseDefaultCameraPair(pairList)
                }
            }
        }

        // 开启当前摄像头
        launch {
            combine(
                mViewModel.currentCameraPairFlow,
                mViewModel.resumeTimestampFlow,
                mViewModel.allPermissionGrantedFlow
            ) { t1, t2, t3 -> Triple(t1, t2, t3) }.collectLatest { t ->
                val cameraPair = mViewModel.currentCameraPairFlow.value
                val allPermissionGrantedFlow = mViewModel.allPermissionGrantedFlow.value
                val resumeTimeStamp = mViewModel.resumeTimestampFlow.value
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
        launch {
            combine(
                cameraDeviceFlow,
                mViewModel.currentOutputItemFlow,
                mViewModel.currentCameraPairFlow,
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
        launch {
            combine(
                cameraCaptureSessionFlow,
                cameraDeviceFlow,
                surfaceFlow,
                mViewModel.captureModeFlow,
                mViewModel.manualSensorParamsFlow,
            ) { t0, t1, t2, t3, t4 ->
                arrayOf(t0, t1, t2, t3, t4)
            }.collectLatest { t ->
                try {
                    val cameraCaptureSession = cameraCaptureSessionFlow.value
                    val cameraDevice = cameraDeviceFlow.value
                    val previewSurface = surfaceFlow.value
                    if (cameraCaptureSession != null && cameraDevice != null && previewSurface != null) {
                        val captureRequest =
                            cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW).apply {
                                addTarget(previewSurface)
                                mViewModel.setCurrentCaptureParams(this)
                            }.build()

                        cameraCaptureSession.setRepeatingRequest(
                            captureRequest,
                            previewCaptureCallback,
                            handler
                        )
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        // 计算帧率
        launch {
            var job: Job? = null
            mViewModel.resumeTimestampFlow.collectLatest {
                launch {
                    val resumeTimestamp = mViewModel.resumeTimestampFlow.value
                    if (resumeTimestamp != null) {
                        job?.cancel()
                        job = launch(Dispatchers.IO) {
                            var currentCount = frameCount
                            var currentRenderCount = cameraSurfaceRender.frameCount
                            while (mViewModel.resumeTimestampFlow.value != null) {
                                delay(1000)
                                Log.i(
                                    TAG,
                                    "onCreate: calc frame 计算帧率： $frameCount - ${cameraSurfaceRender.frameCount}"
                                )
                                mViewModel.captureFrameRate.value = frameCount - currentCount
                                mViewModel.rendererFrameRate.value =
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
            combine(grayMatFlow, mViewModel.exposureHistogramEnableFlow) { t01, t02 ->
                arrayOf(t01, t02)
            }.collectLatest {
                val grayMat = grayMatFlow.value
                val histogram = mViewModel.exposureHistogramEnableFlow.value
                mViewModel.exposureHistogramDataFlow.value =
                    if (histogram && grayMat != null) getHistogramData(grayMat) else null
            }
        }

        // 填充峰值亮度数据
        launch(Dispatchers.IO) {
            val zebraOffsetArr = arrayOf(0).toIntArray()
            combine(grayMatFlow, mViewModel.brightnessPeakingEnableFlow) { t01, t02 ->
                arrayOf(t01, t02)
            }.collectLatest {
                val grayMat = grayMatFlow.value
                val enable = mViewModel.brightnessPeakingEnableFlow.value
                brightnessPeakingMatFlow.value =
                    if (grayMat != null && enable) markOverExposedRegions(
                        zebraOffsetArr,
                        grayMat
                    ) else null
            }
        }

        // 填充峰值对焦数据
        launch(Dispatchers.IO) {
            combine(grayMatFlow, mViewModel.focusPeakingEnableFlow) { t01, t02 ->
                arrayOf(t01, t02)
            }.collectLatest {
                val time = testTime {
                    val grayMat = grayMatFlow.value
                    val enable = mViewModel.focusPeakingEnableFlow.value
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

        // 监听传感器角度变化
        launch(Dispatchers.IO) {
            combine(
                mViewModel.pitchFlow,
                mViewModel.rollFlow,
                mViewModel.yawFlow,
            ) { t01, t02, t03 ->
                arrayOf(t01, t02, t03)
            }.collectLatest {
                val currentPitch = mViewModel.pitchFlow.value
                val currentRoll = mViewModel.rollFlow.value
                val currentYaw = mViewModel.yawFlow.value
                val focusRequestOrientation = mViewModel.focusRequestOrientation.value
                focusRequestOrientation?.apply {
                    val delta = 10F
                    val delay = 1000 * 2 // 两秒内不要取消
                    if (
                        (System.currentTimeMillis() - timestamp > delay)
                        && ((currentPitch - pitch).absoluteValue > delta
                                || (currentRoll - roll).absoluteValue > delta
                                || (currentYaw - yaw).absoluteValue > delta)
                    ) {
                        mViewModel.focusCancel()
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        mViewModel.startSensor()
        mViewModel.resumeTimestampFlow.value = System.currentTimeMillis()
    }

    override fun onPause() {
        super.onPause()
        mViewModel.stopSensor()
        mViewModel.resumeTimestampFlow.value = null
    }

    override fun onDestroy() {
        super.onDestroy()
        cancel()
    }

    private suspend fun onCapture() {
        if (cameraCaptureSessionFlow.value == null) return
        val outputItem = mViewModel.currentOutputItemFlow.value
        val cameraDevice = cameraDeviceFlow.value ?: return
        val cameraCaptureRequest =
            cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)
                .apply {
                    if (outputItem != null) {
                        imageOutputReader?.apply { addTarget(surface) }
                    }
                    val rotationOrientation = mViewModel.rotationOrientation.value
                    set(CaptureRequest.JPEG_ORIENTATION, rotationOrientation)
                    mViewModel.setCurrentCaptureParams(this)
                }.build()
        captureTimestamp = System.currentTimeMillis()
        listOf(
            async {
                captureResult =
                    cameraCaptureSessionFlow.value?.captureAsync(cameraCaptureRequest, handler)
            },
            async {
                outputFileFlow.takeWhile { it != null }.first()
                outputFileFlow.emit(null)
            }
        ).awaitAll()
    }

    private val previewCaptureCallback = object : CaptureCallback() {
        override fun onCaptureCompleted(
            session: CameraCaptureSession,
            request: CaptureRequest,
            result: TotalCaptureResult
        ) {
            super.onCaptureCompleted(session, request, result)
            mViewModel.captureResultFlow.value = result
        }
    }

    private fun setOrientation(cameraCharacteristics: CameraCharacteristics) {
        // 设置预览方向相关
        val sensorSize = cameraCharacteristics.sensorSize
        val cameraFacing = cameraCharacteristics.cameraFacing
        val isFrontCamera = cameraCharacteristics.isFrontCamera
        if (sensorSize != null && cameraFacing != null) {
            val sensorOrientation = cameraCharacteristics.sensorOrientation ?: 90
            val rotationOrientationResult = if (isFrontCamera) {
                (sensorOrientation + displayRotation) % 360
            } else {
                (sensorOrientation - displayRotation + 360) % 360
            }
            mViewModel.rotationOrientation.value = rotationOrientationResult
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
            imageOutputReader!!.setOnImageAvailableListener(
                outputImageAvailableListener,
                handler
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

    private fun getStoragePath(): File {
        val picturesFile =
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).absoluteFile
        val storageFile = File(picturesFile, "yao")
        if (!storageFile.exists()) storageFile.mkdirs()
        return storageFile
    }

    private val outputImageAvailableListener = ImageReader.OnImageAvailableListener { reader ->
        Log.i(TAG, "image: OnImageAvailableListener")
        val image = reader.acquireNextImage()
        var outputFile: File? = null
        if (image.format == ImageFormat.JPEG || image.format == ImageFormat.DEPTH_JPEG) {
            var fos: FileOutputStream? = null
            try {
                val byteBuffer = image.planes[0].buffer
                val bytes = ByteArray(byteBuffer.remaining()).apply { byteBuffer.get(this) }
                val file = File(
                    getStoragePath(),
                    "YAO_${captureTimestamp ?: System.currentTimeMillis()}.jpg"
                )
                fos = FileOutputStream(file)
                fos.write(bytes)
                fos.flush()
                outputFile = file
                Log.i(TAG, "image: jpeg -> successful")
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                fos?.close()
            }
        } else if (image.format == ImageFormat.HEIC) {
            var fos: FileOutputStream? = null
            try {
                val byteBuffer = image.planes[0].buffer
                val bytes = ByteArray(byteBuffer.remaining()).apply { byteBuffer.get(this) }
                val file = File(
                    getStoragePath(),
                    "YAO_${captureTimestamp ?: System.currentTimeMillis()}.heic"
                )
                fos = FileOutputStream(file)
                fos.write(bytes)
                fos.flush()
                outputFile = file
                Log.i(TAG, "image: heic -> successful")
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                fos?.close()
            }
        } else if (image.format == ImageFormat.RAW_SENSOR && captureResult != null) {
            val cameraPair = mViewModel.currentCameraPairFlow.value
            val cameraCharacteristics = cameraPair?.second ?: return@OnImageAvailableListener
            var fos: FileOutputStream? = null
            var dngCreator: DngCreator? = null
            try {
                dngCreator =
                    DngCreator(cameraCharacteristics, captureResult!!)
                val file = File(
                    getStoragePath(),
                    "YAO_${captureTimestamp ?: System.currentTimeMillis()}.dng"
                )
                fos = FileOutputStream(file)
                val exifRotation = when (mViewModel.rotationOrientation.value) {
                    90 -> ExifInterface.ORIENTATION_ROTATE_90
                    180 -> ExifInterface.ORIENTATION_ROTATE_180
                    270 -> ExifInterface.ORIENTATION_ROTATE_270
                    else -> ExifInterface.ORIENTATION_NORMAL
                }

                // dng图片无法同时旋转和水平翻转
                dngCreator.setOrientation(exifRotation)
                dngCreator.writeImage(fos, image)
                captureResult = null
                outputFile = file
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                dngCreator?.close()
                fos?.close()
            }
            Log.i(TAG, "image: dng -> successful")
        }
        launch {
            outputFileFlow.emit(outputFile)
        }
        image.close()
    }

    private val previewImageAvailableListener = object : ImageReader.OnImageAvailableListener {
        override fun onImageAvailable(reader: ImageReader?) {
            val image = reader?.acquireNextImage() ?: return
            frameCount++
            val time = testTime {
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

            }
//            Log.i(TAG, "previewImageAvailableListener process frame time $time")
            image.close()
        }
    }

    @SuppressLint("MissingPermission")
    private fun openCamera(cameraPair: CameraPair) {
        val cameraCharacteristics = cameraPair.second
        // 获取当前摄像头支持的输出格式
        mViewModel.fetchCurrentSupportedOutput(cameraCharacteristics)
        // 设置预览渲染的旋转角度
        setOrientation(cameraCharacteristics)
        // 实际开启摄像头代码
        val cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
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

}

val camera2Permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
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

@Composable
fun Camera2Body(
    onGLSurfaceView: (GLSurfaceView) -> Unit,
    onCapture: () -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        // 预览图层
        Camera2PreviewLayer(
            onGLSurfaceView = onGLSurfaceView,
        )
        // 信息图层
        Camera2InfoLayer()
        // 操作图层
        Camera2ActionLayer(
            onCapture = onCapture,
        )
    }
}

@Composable
fun Camera2PreviewLayer(
    onGLSurfaceView: (GLSurfaceView) -> Unit,
) {
    val viewModel: CameraViewModel = koinViewModel()
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val portrait = maxHeight > maxWidth
        val currentCameraPair = viewModel.currentCameraPairFlow.collectAsState()
        val cameraCharacteristics = currentCameraPair.value?.second
        Box(
            modifier = Modifier
                .run {
                    val imageAspectRatio =
                        cameraCharacteristics?.sensorAspectRatio ?: defaultSensorAspectRatio
                    if (portrait) {
                        fillMaxWidth()
                            .aspectRatio(1.div(imageAspectRatio))
                    } else {
                        fillMaxHeight()
                            .aspectRatio(imageAspectRatio)
                    }
                }
                .background(Color.Cyan.copy(0.2F))
                .align(if (portrait) Alignment.TopCenter else Alignment.CenterStart),
        ) {
            AndroidView(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { clip = true },
                factory = { ctx ->
                    GLSurfaceView(ctx).apply {
                        layoutParams = FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        onGLSurfaceView(this)
                    }
                }
            )

            val previewSize = remember { mutableStateOf(IntSize.Zero) }
            val fingerClick = remember { mutableStateOf(Offset.Zero) }
            val fingerRect = remember { mutableStateOf(Rect.Zero) }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures {
                            fingerClick.value = it
                            val previewWidth = previewSize.value.width.toFloat()
                            val previewHeight = previewSize.value.height.toFloat()
                            val fingerSize = 100F
                            val rect = getFingerPointRect(
                                it.x,
                                it.y,
                                previewWidth,
                                previewHeight,
                                fingerSize
                            )
                            fingerRect.value = rect
                            val normalizedRect = rectNormalized(rect, previewWidth, previewHeight)
                            viewModel.focusRequest(normalizedRect)
                        }
                    }
                    .onSizeChanged {
                        previewSize.value = it
                    }
            ) {
                if (cameraCharacteristics != null) {
                    val focusPointRect = viewModel.focusPointRectFlow.collectAsState()
                    val captureResult = viewModel.captureResultFlow.collectAsState()
                    val sensorSize = cameraCharacteristics.sensorSize
                    if (sensorSize != null) {
                        val sensorWidth = sensorSize.width()
                        val sensorHeight = sensorSize.height()
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            focusPointRect.value?.let { fPointRect ->
                                val pointRect =
                                    rectFromNormalized(fPointRect, size.width, size.height)
                                drawRect(
                                    color = Color.White,
                                    topLeft = pointRect.topLeft,
                                    size = pointRect.size,
                                    style = Stroke(width = 10F)
                                )
                            }

                            captureResult.value?.apply {
                                afRegions?.forEach { meteringRectangle ->
                                    val rect = sensorDetectRect2ComposeRect(
                                        rect = meteringRectangle.rect,
                                        rotationOrientation = viewModel.rotationOrientation.value,
                                        flipHorizontal = cameraCharacteristics.isFrontCamera,
                                        size = size,
                                        sensorWidth = sensorWidth,
                                        sensorHeight = sensorHeight,
                                    )
                                    drawRect(
                                        color = Color.Red,
                                        topLeft = rect.topLeft,
                                        size = rect.size,
                                        style = Stroke(width = 4F)
                                    )
                                    val circleSize = 16F
                                    drawCircle(
                                        color = when (afState) {
                                            CameraMetadata.CONTROL_AF_STATE_ACTIVE_SCAN -> Color.White
                                            CameraMetadata.CONTROL_AF_STATE_FOCUSED_LOCKED -> Color.Green
                                            CameraMetadata.CONTROL_AF_STATE_NOT_FOCUSED_LOCKED -> Color.Red
                                            else -> Color.Gray
                                        },
                                        radius = circleSize,
                                        center = Offset(
                                            x = rect.right + circleSize,
                                            y = rect.top,
                                        ),
                                    )
                                }
                                aeRegions?.forEach { meteringRectangle ->
                                    val rect = sensorDetectRect2ComposeRect(
                                        rect = meteringRectangle.rect,
                                        rotationOrientation = viewModel.rotationOrientation.value,
                                        flipHorizontal = cameraCharacteristics.isFrontCamera,
                                        size = size,
                                        sensorWidth = sensorWidth,
                                        sensorHeight = sensorHeight,
                                    )
                                    drawRect(
                                        color = Color.Red,
                                        topLeft = rect.topLeft,
                                        size = rect.size,
                                        style = Stroke(width = 4F)
                                    )
                                    val circleSize = 16F
                                    drawCircle(
                                        color = when (aeState) {
                                            CameraMetadata.CONTROL_AE_STATE_PRECAPTURE -> Color.White
                                            CameraMetadata.CONTROL_AE_STATE_CONVERGED, CameraMetadata.CONTROL_AE_STATE_FLASH_REQUIRED -> Color.Green
                                            else -> Color.Gray
                                        },
                                        radius = circleSize,
                                        center = Offset(
                                            x = rect.right + circleSize,
                                            y = rect.bottom,
                                        ),
                                    )
                                }
                                faceDetectResult?.forEach {
                                    val faceRect = sensorDetectRect2ComposeRect(
                                        rect = it.bounds,
                                        rotationOrientation = viewModel.rotationOrientation.value,
                                        flipHorizontal = cameraCharacteristics.isFrontCamera,
                                        size = size,
                                        sensorWidth = sensorWidth,
                                        sensorHeight = sensorHeight,
                                    )
                                    drawRect(
                                        color = Color.Cyan,
                                        topLeft = faceRect.topLeft,
                                        size = faceRect.size,
                                        style = Stroke(width = 10F)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            CameraSeaLevel()
        }
    }
}

@Composable
fun Camera2InfoLayer() {
    val viewModel: CameraViewModel = koinViewModel()
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .clip(Layout.roundShape.rm)
                .background(
                    MaterialTheme.colorScheme.surface.copy(0.6F)
                )
                .padding(Layout.padding.pm)
        ) {
            Text(
                text = "${viewModel.captureFrameRate.value}-${viewModel.rendererFrameRate.value}",
                color = Color.Green,
                fontSize = Layout.fontSize.fl,
                fontWeight = FontWeight.Bold
            )
            val captureResult = viewModel.captureResultFlow.collectAsState()
            Text(text = "曝光时间：${captureResult.value?.sensorExposureTime}")
            Text(text = "感光度：${captureResult.value?.sensorSensitivity}")
            Text(text = "电子变焦：${captureResult.value?.zoomRatio}")
            Text(text = "场景模式：${SceneMode.getByCode(captureResult.value?.sceneMode ?: -1)}")
            Text(text = "OIS：${captureResult.value?.oisEnable}")
        }

        Column(
            modifier = Modifier
                .align(Alignment.TopEnd)
        ) {
            val surfaceBitmap = viewModel.surfaceBitmap
            if (surfaceBitmap.value != null) {
                Image(
                    modifier = Modifier
                        .size(200.dp),
                    bitmap = surfaceBitmap.value!!.asImageBitmap(),
                    contentDescription = null
                )
            }
            val exposureHistogramData = viewModel.exposureHistogramDataFlow.collectAsState()
            if (exposureHistogramData.value != null) {
                Canvas(
                    modifier = Modifier
                        .width(160.dp)
                        .height(80.dp),
                    onDraw = {
                        drawRect(color = Color.White)
                        val histData = exposureHistogramData.value!!
                        val histSize = histData.size
                        val maxValue = histData.maxOrNull() ?: 1.0f // 避免除以零
                        val scaleY = size.height / maxValue
                        val binWidth = size.width / histSize
                        var prevX = 0f
                        var prevY = size.height
                        for (i in 0 until histSize) {
                            val x = i * binWidth
                            val y = size.height - histData[i] * scaleY
                            drawLine(
                                color = Color.Black,
                                strokeWidth = 2F,
                                start = Offset(prevX, prevY),
                                end = Offset(x, y)
                            )
                            prevX = x
                            prevY = y
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun Camera2ActionLayer(
    onCapture: () -> Unit,
) {
    val viewModel: CameraViewModel = koinViewModel()
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .run {
                    if (maxHeight > maxWidth) {
                        fillMaxWidth().align(Alignment.BottomCenter)
                    } else {
                        fillMaxHeight().align(Alignment.BottomEnd)
                    }
                }
                .aspectRatio(1F)
                .padding(Layout.padding.pm)
                .clip(Layout.roundShape.rm)
                .background(
                    MaterialTheme.colorScheme.surface.copy(0.6F)
                )
                .verticalScroll(state = rememberScrollState())
                .padding(Layout.padding.pm)
                .align(Alignment.BottomCenter)
        ) {
            val currentCamera = viewModel.currentCameraPairFlow.collectAsState()
            val cameraList = viewModel.cameraPairListFlow.collectAsState()
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "摄像头：")
                cameraList.value.forEachIndexed { index, pair ->
                    Button(enabled = pair != currentCamera.value, onClick = {
                        viewModel.currentCameraPairFlow.value = pair
                    }) {
                        Text(text = "$index")
                    }
                }
            }

            val currentCameraPair = viewModel.currentCameraPairFlow.collectAsState()
            val cameraCharacteristics = currentCameraPair.value?.second
            if (cameraCharacteristics != null) {
                val outputItemList = cameraCharacteristics.outputSupportedMode
                val currentOutputItem = viewModel.currentOutputItemFlow.collectAsState()
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "输出格式：")
                    Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                        outputItemList.forEach { outputItem ->
                            val enable = currentOutputItem.value == outputItem
                            Button(
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (enable) MaterialTheme.colorScheme.error
                                    else MaterialTheme.colorScheme.primary
                                ),
                                onClick = {
                                    viewModel.currentOutputItemFlow.value = outputItem
                                }
                            ) {
                                Column {
                                    Text(text = outputItem.outputMode.label)
                                    Text(
                                        text = outputItem.bestSize.run { "${width}x${height}" },
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        }
                    }
                }

                val captureMode = viewModel.captureModeFlow.collectAsState()
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "拍照模式：")
                    for (mode in CaptureMode.entries) {
                        Button(enabled = captureMode.value != mode, onClick = {
                            viewModel.captureModeFlow.value = mode
                        }) {
                            Text(text = mode.name)
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "辅助：")
                    Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                        val focusPeakingEnable = viewModel.focusPeakingEnableFlow.collectAsState()
                        EnableButton(
                            enable = focusPeakingEnable.value,
                            label = "峰值对焦",
                            onClick = { viewModel.focusPeakingEnableFlow.apply { value = !value } }
                        )
                        val brightnessPeakingEnable =
                            viewModel.brightnessPeakingEnableFlow.collectAsState()
                        EnableButton(
                            enable = brightnessPeakingEnable.value,
                            label = "峰值亮度",
                            onClick = {
                                viewModel.brightnessPeakingEnableFlow.apply {
                                    value = !value
                                }
                            }
                        )
                        val exposureHistogramEnable =
                            viewModel.exposureHistogramEnableFlow.collectAsState()
                        EnableButton(
                            enable = exposureHistogramEnable.value,
                            label = "直方图",
                            onClick = {
                                viewModel.exposureHistogramEnableFlow.apply {
                                    value = !value
                                }
                            }
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "场景模式：")
                    Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                        val currentSceneMode = viewModel.currentSceneModeFlow.collectAsState()
                        EnableButton(
                            enable = currentSceneMode.value == null,
                            label = "无",
                            onClick = { viewModel.currentSceneModeFlow.value = null }
                        )
                        cameraCharacteristics.sceneModes.forEach { sceneMode ->
                            EnableButton(
                                enable = currentSceneMode.value == sceneMode,
                                label = sceneMode.name,
                                onClick = { viewModel.currentSceneModeFlow.value = sceneMode }
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "光学防抖：")
                    Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                        val oisEnable = viewModel.oisEnableFlow.collectAsState()
                        val oisAvailable = cameraCharacteristics.oisAvailable
                        if (oisAvailable) {
                            EnableButton(
                                enable = oisEnable.value,
                                label = "OIS",
                                onClick = { viewModel.oisEnableFlow.apply { value = !value } }
                            )
                        } else {
                            Button(onClick = { }, enabled = false) {
                                Text(text = "无")
                            }
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "人脸识别：")
                    Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                        val currentFaceDetectMode =
                            viewModel.currentFaceDetectModeFlow.collectAsState()
                        cameraCharacteristics.faceDetectModes.forEach { faceDetectMode ->
                            EnableButton(
                                enable = currentFaceDetectMode.value == faceDetectMode,
                                label = faceDetectMode.label,
                                onClick = {
                                    viewModel.currentFaceDetectModeFlow.value = faceDetectMode
                                }
                            )
                        }
                    }
                }

                val zoomRatioRange = cameraCharacteristics.zoomRatioRange
                if (zoomRatioRange != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "电子变焦：")
                        val zoomRatio = viewModel.zoomRatioFlow.collectAsState()
                        Box(modifier = Modifier.width(80.dp)) {
                            Text(text = "${zoomRatio.value}/${zoomRatioRange.upper}")
                        }
                        Slider(
                            valueRange = zoomRatioRange.run { lower.toFloat()..upper.toFloat() },
                            value = zoomRatio.value,
                            onValueChange = {
                                viewModel.zoomRatioFlow.value = it
                            },
                        )
                    }
                }

                Column(modifier = Modifier.animateContentSize()) {
                    if (captureMode.value == CaptureMode.MANUAL) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "手动控制：")

                            Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                                val afEnable = viewModel.afEnableFlow.collectAsState(initial = true)
                                EnableButton(
                                    enable = afEnable.value,
                                    label = "AF",
                                    onClick = { viewModel.setAfEnable() })

                                val aeEnable = viewModel.aeEnableFlow.collectAsState(initial = true)
                                EnableButton(
                                    enable = aeEnable.value,
                                    label = "AE",
                                    onClick = { viewModel.setAeEnable() })

                                val awbEnable =
                                    viewModel.awbEnableFlow.collectAsState(initial = true)
                                EnableButton(
                                    enable = awbEnable.value,
                                    label = "AWB",
                                    onClick = { viewModel.setAwbEnable() }
                                )
                            }
                        }

                        val focalDistanceRange = cameraCharacteristics.focalDistanceRange
                        val focalDistance = viewModel.focalDistanceFlow.collectAsState()
                        if (focalDistanceRange != null) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(text = "对焦距离：")
                                Box(modifier = Modifier.width(80.dp)) {
                                    Text(text = "${focalDistance.value}/${focalDistanceRange.upper}")
                                }
                                Slider(
                                    valueRange = focalDistanceRange.run { lower.toFloat()..upper.toFloat() },
                                    value = focalDistance.value
                                        ?: focalDistanceRange.lower.toFloat(),
                                    onValueChange = {
                                        viewModel.focalDistanceFlow.value = it
                                    },
                                )
                            }
                        }

                        val sensorSensitivityRange = cameraCharacteristics.sensorSensitivityRange
                        val sensorSensitivity = viewModel.sensorSensitivityFlow.collectAsState()
                        if (sensorSensitivityRange != null) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(text = "感光度：")
                                Box(modifier = Modifier.width(80.dp)) {
                                    Text(text = "${sensorSensitivity.value}/${sensorSensitivityRange.upper}")
                                }
                                Slider(
                                    valueRange = sensorSensitivityRange.run { lower.toFloat()..upper.toFloat() },
                                    value = sensorSensitivity.value?.toFloat()
                                        ?: sensorSensitivityRange.lower.toFloat(),
                                    onValueChange = {
                                        viewModel.sensorSensitivityFlow.value = it.toInt()
                                    },
                                )
                            }
                        }

                        val sensorExposureTime = viewModel.sensorExposureTimeFlow.collectAsState()
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "曝光时间：")
                            Row(
                                modifier = Modifier
                                    .weight(1F)
                                    .horizontalScroll(rememberScrollState())
                            ) {
                                for (exposureTime in ExposureTime.entries) {
                                    Button(
                                        enabled = exposureTime.time != sensorExposureTime.value,
                                        onClick = {
                                            viewModel.sensorExposureTimeFlow.value =
                                                exposureTime.time
                                        }
                                    ) {
                                        Text(text = exposureTime.label)
                                    }
                                }
                            }
                        }

                        val customTemperature = viewModel.customTemperatureFlow.collectAsState()
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "白平衡：")
                            Box(modifier = Modifier.width(80.dp)) {
                                Text(text = "${customTemperature.value}")
                            }
                            Slider(
                                valueRange = 0F..100F,
                                value = customTemperature.value?.toFloat() ?: 0F,
                                onValueChange = {
                                    viewModel.customTemperatureFlow.value = it.toInt()
                                },
                            )
                        }
                    } else {
                        val aeCompensationRange = cameraCharacteristics.aeCompensationRange
                        val aeCompensation = viewModel.aeCompensationFlow.collectAsState()
                        if (aeCompensationRange != null) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(text = "曝光补偿：")
                                Slider(
                                    valueRange = aeCompensationRange.run { lower.toFloat()..upper.toFloat() },
                                    value = aeCompensation.value.toFloat(),
                                    onValueChange = {
                                        viewModel.aeCompensationFlow.value = it.toInt()
                                    },
                                )
                            }
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.Center) {
                    val captureLoading = viewModel.captureLoading
                    Button(onClick = onCapture, enabled = !captureLoading.value) {
                        if (captureLoading.value) {
                            CircularProgressIndicator(
                                modifier = Modifier
                                    .padding(end = 12.dp)
                                    .size(16.dp),
                                strokeWidth = 2.dp
                            )
                        }
                        Text(text = "拍照")
                    }
                }
            }
        }
    }
}

@Composable
fun CameraSeaLevel() {
    val density = LocalDensity.current
    val viewModel: CameraViewModel = koinViewModel()
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .border(
                    width = 2.dp,
                    color = MaterialTheme.colorScheme.onBackground.copy(0.2F),
                    shape = CircleShape,
                )
                .align(Alignment.Center),
        )

        val gravity = viewModel.gravityFlow.collectAsState()
        val gravityDegreesAnimation =
            animateRotationAsState(targetValue = gravity.value - viewModel.displayRotation.value)
        val pitch = viewModel.pitchFlow.collectAsState()
        val roll = viewModel.rollFlow.collectAsState()
        val pitchAnimation = animateFloatAsState(targetValue = pitch.value)
        val rollAnimation = animateFloatAsState(targetValue = roll.value)
        val offsetPY = remember {
            derivedStateOf {
                var p = pitchAnimation.value.div(90F)
                if (p < -1) p = -1F
                if (p > 1) p = 1F
                p
            }
        }
        val offsetPX = remember {
            derivedStateOf {
                var p = -rollAnimation.value.div(90F)
                if (p < -1) p = -1F
                if (p > 1) p = 1F
                p
            }
        }
        val bubbleViewVisible = remember(offsetPX.value, offsetPY.value) {
            derivedStateOf {
                offsetPX.value.absoluteValue < 0.5 && offsetPY.value.absoluteValue < 0.5
            }
        }
        val bubbleAlphaAnimation =
            animateFloatAsState(targetValue = if (bubbleViewVisible.value) 1F else 0F)

        Box(
            modifier = Modifier
                .graphicsLayer {
                    rotationZ = gravityDegreesAnimation.value
                    alpha = 1 - bubbleAlphaAnimation.value
                }
                .width(100.dp)
                .height(4.dp)
                .clip(CircleShape)
                .background(color = MaterialTheme.colorScheme.primary)
                .align(Alignment.Center)
        )

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1F)
                .align(Alignment.Center)
        ) {
            val offsetY = remember(offsetPY.value) {
                derivedStateOf {
                    density.run { maxHeight.toPx().div(2).times(offsetPY.value) }
                }
            }
            val offsetX = remember(offsetPX.value) {
                derivedStateOf {
                    density.run { maxWidth.toPx().div(2).times(offsetPX.value) }
                }
            }
            Box(
                modifier = Modifier
                    .graphicsLayer {
                        translationX = offsetX.value
                        translationY = offsetY.value
                        alpha = bubbleAlphaAnimation.value
                    }
                    .size(60.dp)
                    .clip(CircleShape)
                    .background(color = MaterialTheme.colorScheme.onBackground.copy(0.2F))
                    .align(Alignment.Center)
            )
        }
    }
}

@Composable
fun EnableButton(
    enable: Boolean,
    label: String,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (enable) {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.primary
            }
        ),
    ) {
        Text(text = "$label(${if (enable) "ON" else "OFF"})")
    }
}