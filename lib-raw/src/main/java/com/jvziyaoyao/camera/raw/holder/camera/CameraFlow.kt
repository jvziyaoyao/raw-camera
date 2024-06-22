package com.jvziyaoyao.camera.raw.holder.camera

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.ImageFormat
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CameraMetadata
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.CaptureResult
import android.hardware.camera2.TotalCaptureResult
import android.media.Image
import android.media.ImageReader
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.util.Log
import android.util.Size
import android.view.Surface
import androidx.compose.ui.geometry.Rect
import androidx.exifinterface.media.ExifInterface
import com.jvziyaoyao.camera.raw.util.ContextUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.launch
import java.io.File
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
    private val handler: Handler,
    val displayRotation: Int = 0,
    val provideSaveFile: ProvideSaveFile = defaultProvideSaveFile,
    val getPreviewSurface: (Size) -> Surface? = { null },
) : CoroutineScope by MainScope() {

    private val TAG = CameraFlow::class.java.name

    private val context = ContextUtil.getApplicationByReflect()

    private val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager

    private var cameraCaptureSessionFlow = MutableStateFlow<CameraCaptureSession?>(null)

    private var surfaceFlow = MutableStateFlow<Surface?>(null)

    private var imageOutputReader: ImageReader? = null

    private val executor = Executors.newSingleThreadExecutor()

    val cameraDeviceFlow = MutableStateFlow<CameraDevice?>(null)

    suspend fun capture(
        outputFile: File? = null,
        additionalRotation: Int? = null,
    ) {
        val captureResult = captureResultFlow.value
        val flashLight = flashLightFlow.first()
        if (captureResult?.is3AComplete != true && flashLight) {
            focusRequestAsync(null)
        }
        internalCapture(outputFile, additionalRotation)
    }

    private suspend fun internalCapture(
        outputFile: File? = null,
        additionalRotation: Int? = null,
    ) {
        if (cameraCaptureSessionFlow.value == null) return
        val cameraDevice = cameraDeviceFlow.value ?: return

        var imageOrientation = rotationOrientationFlow.value
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

    private val previewCaptureCallback = object : CameraCaptureSession.CaptureCallback() {
        override fun onCaptureCompleted(
            session: CameraCaptureSession,
            request: CaptureRequest,
            result: TotalCaptureResult
        ) {
            super.onCaptureCompleted(session, request, result)
            captureRequestFlow.value = request
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
            rotationOrientationFlow.value = rotationOrientationResult
        }
    }

    private suspend fun startPreview(
        cameraDevice: CameraDevice,
        previewSize: Size,
        outputItem: OutputItem?,
    ) {
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

        getPreviewSurface(previewSize)?.let {
            surfaceFlow.value = it
            surfaceList.add(it)
        }

        cameraCaptureSessionFlow.value = cameraDevice.createCameraSessionAsync(handler, surfaceList)
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
                currentPreviewSizeFlow,
                currentOutputItemFlow,
                currentCameraPairFlow,
            ) { t0, t1, t2, t3 ->
                arrayOf(t0, t1, t2, t3)
            }.collectLatest { t ->
                val cameraDevice = t[0] as CameraDevice?
                var previewSize = t[1] as Size?
                val currentOutputItem = t[2] as OutputItem?
                val cameraPair = t[3] as CameraPair?
                val cameraId = cameraPair?.first
                val cameraCharacteristics = cameraPair?.second
                if (
                    previewSize == null
                    && cameraCharacteristics != null
                ) {
                    previewSize = chooseDefaultPreviewSize(
                        cameraCharacteristics = cameraCharacteristics,
                    )
                }
                if (
                    cameraDevice != null
                    && previewSize != null
                    && cameraId == cameraDevice.id
                ) {
                    startPreview(
                        cameraDevice,
                        previewSize,
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
                        Log.i(TAG, "setupCamera: $focusRequestTrigger")
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

    }

    fun release() {
        cancel()
    }

    fun resume() {
        resumeTimestampFlow.value = System.currentTimeMillis()
    }

    fun pause() {
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

    val rotationOrientationFlow = MutableStateFlow(0)

    val cameraPairListFlow =
        MutableStateFlow(emptyList<Pair<String, CameraCharacteristics>>())

    // TODO 不对外暴露
    val currentCameraPairFlow = MutableStateFlow<Pair<String, CameraCharacteristics>?>(null)

    val currentOutputItemFlow = MutableStateFlow<OutputItem?>(null)

    val currentPreviewSizeFlow = MutableStateFlow<Size?>(null)

    private val allPermissionGrantedFlow = MutableStateFlow(false)

    fun onPermissionChanged(allGranted: Boolean) {
        allPermissionGrantedFlow.value = allGranted
    }

    /**
     *
     * 预览数据
     *
     */

    val captureRequestFlow = MutableStateFlow<CaptureRequest?>(null)

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

    val flashLightFlow = combine(
        captureResultFlow,
        captureController.flashModeFlow,
    ) { captureResult, flashMode ->
        val requiredFlash = captureResult?.aeState == CameraMetadata.CONTROL_AE_STATE_FLASH_REQUIRED
        flashMode == FlashMode.ON
                || flashMode == FlashMode.ALWAYS_ON
                || (flashMode == FlashMode.AUTO && requiredFlash)
    }

    private fun setCurrentCaptureParams(
        preview: Boolean,
        trigger: Boolean,
        builder: CaptureRequest.Builder
    ) = captureController.setCurrentCaptureParams(preview, trigger, builder)

    fun focusRequest(rect: Rect?) {
        val focusRect = if (rect == null) null else {
            val cameraPair = currentCameraPairFlow.value
            val characteristics = cameraPair?.second
            val flipHorizontal = characteristics?.isFrontCamera == true
            val sensorSize = characteristics?.sensorSize
            if (sensorSize == null) null else {
                composeRect2SensorRect(
                    rect = rect,
                    rotationOrientation = rotationOrientationFlow.value,
                    flipHorizontal = flipHorizontal,
                    sensorWidth = sensorSize.width(),
                    sensorHeight = sensorSize.height(),
                )
            }
        }
        captureController.focusRequest(focusRect)
    }

    suspend fun focusRequestAsync(rect: Rect?) {
        captureController.tag = "${System.currentTimeMillis()}"
        focusRequest(rect)
        var complete = false
        var isMyRequest = false
        captureResultFlow
            .takeWhile { !complete }
            .collectLatest {
                val currentIsMyRequest = captureRequestFlow.value?.tag == captureController.tag
                if (currentIsMyRequest) {
                    isMyRequest = true
                    if (it?.is3AComplete == true) {
                        complete = true
                    }
                } else if (isMyRequest) {
                    complete = true
                }
            }
    }

    fun focusCancel() {
        captureController.focusCancel()
    }

}