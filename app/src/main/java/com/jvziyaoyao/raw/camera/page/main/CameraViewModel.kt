package com.jvziyaoyao.raw.camera.page.main

import android.graphics.Bitmap
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.CaptureResult
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.jvziyaoyao.raw.camera.domain.clean.usecase.SensorUseCase
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

class CameraViewModel(
    private val sensorUseCase: SensorUseCase
) : ViewModel() {

    private val TAG = CameraViewModel::class.java.name

    /**
     *
     * 姿态传感器
     *
     */

    val gravityFlow = sensorUseCase.gravityFlow

    val pitchFlow = sensorUseCase.pitchFlow
    val rollFlow = sensorUseCase.rollFlow
    val yawFlow = sensorUseCase.yawFlow

    fun startSensor() = sensorUseCase.start()

    fun stopSensor() = sensorUseCase.stop()

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
//        val scaleStreamConfigurationMap = cameraCharacteristics.get(
//            CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP
//        ) ?: throw RuntimeException("流配置列表为空！")
//        val outputFormats = scaleStreamConfigurationMap.outputFormats

//        outputSupportedItemList.clear()
//        OutputMode.entries.forEach { outputMode ->
//            if (outputFormats.contains(outputMode.imageFormat)) {
//                val outputSizes = scaleStreamConfigurationMap.getOutputSizes(outputMode.imageFormat)
//                val aspectList = getSizeByAspectRatio(outputSizes, imageAspectRatio)
//                val bestSize = findBestSize(aspectList.toTypedArray())
//                if (bestSize != null) {
//                    outputSupportedItemList.add(OutputItem(outputMode, bestSize))
//                }
//            }
//        }
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


//        scaleStreamConfigurationMap.apply {
//            outputFormats?.forEach { format ->
//                val label = when (format) {
//                    ImageFormat.JPEG -> "JPEG"
//                    ImageFormat.DEPTH_JPEG -> "DEPTH_JPEG"
//                    ImageFormat.RAW_SENSOR -> "RAW_SENSOR"
//                    ImageFormat.RAW_PRIVATE -> "RAW_PRIVATE"
//                    ImageFormat.RAW10 -> "RAW10"
//                    ImageFormat.RAW12 -> "RAW12"
//                    ImageFormat.NV21 -> "NV21"
//                    ImageFormat.NV16 -> "NV16"
//                    ImageFormat.YUV_420_888 -> "YUV_420_888"
//                    ImageFormat.YUV_422_888 -> "YUV_422_888"
//                    ImageFormat.YUV_444_888 -> "YUV_444_888"
//                    ImageFormat.PRIVATE -> "PRIVATE"
//                    ImageFormat.HEIC -> "HEIC"
//                    else -> "OTHER - $format"
//                }
//                Log.i(TAG, "openCamera: image format label: $label")
//                val sizeList = getOutputSizes(format)
//                sizeList.forEach { size ->
//                    Log.i(TAG, "scaleStreamConfigurationMap getOutputSizes: $size")
//                }
//            }
//            val surfaceHolderOutputSizeList = getOutputSizes(SurfaceHolder::class.java)
//            Log.i(TAG, "openCamera: image format label: ${SurfaceHolder::class.java.name}")
//            surfaceHolderOutputSizeList.forEach { size ->
//                Log.i(TAG, "scaleStreamConfigurationMap: getOutputSizes $size")
//            }
//        }

//        aeCompensationRange.value =
//            cameraCharacteristics[CameraCharacteristics.CONTROL_AE_COMPENSATION_RANGE]
//        sensorSensitivityRange.value =
//            cameraCharacteristics.get(CameraCharacteristics.SENSOR_INFO_SENSITIVITY_RANGE)
//        sensorExposureTimeRange.value =
//            cameraCharacteristics.get(CameraCharacteristics.SENSOR_INFO_EXPOSURE_TIME_RANGE)
//        val minimumFocusDistance =
//            cameraCharacteristics[CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE]
//        if (minimumFocusDistance != null) {
//            focalDistanceRange.value = Range(0.0F, minimumFocusDistance)
//        }
//        val ois =
//            cameraCharacteristics[CameraCharacteristics.LENS_INFO_AVAILABLE_OPTICAL_STABILIZATION]
//        oisAvailable.value =
//            ois?.contains(CameraCharacteristics.CONTROL_VIDEO_STABILIZATION_MODE_ON) == true
//        Log.i(
//            TAG,
//            "fetchCurrentSupportedOutput: ois ${Arrays.toString(ois)} - ${oisAvailable.value}"
//        )
//        val sceneModes = cameraCharacteristics[CameraCharacteristics.CONTROL_AVAILABLE_SCENE_MODES]
//        sceneModeAvailableList.clear()
//        sceneModes?.onEach { m ->
//            SceneMode.getByCode(m)?.let { sceneModeAvailableList.add(it) }
//        }

//        val hasHDRMode = sceneModes?.contains(CameraCharacteristics.CONTROL_SCENE_MODE_HDR) == true
//        Log.i(
//            TAG,
//            "fetchCurrentSupportedOutput: sceneModes ${Arrays.toString(sceneModes)} - $hasHDRMode"
//        )
//        val availableFocalLengths =
//            cameraCharacteristics[CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS]
//        Log.i(
//            TAG,
//            "fetchCurrentSupportedOutput: availableFocalLengths ${
//                Arrays.toString(availableFocalLengths)
//            }"
//        )
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
//            val nextZoomRatioRange =
//                cameraCharacteristics[CameraCharacteristics.CONTROL_ZOOM_RATIO_RANGE]
//            Log.i(TAG, "fetchCurrentSupportedOutput: nextZoomRatioRange $nextZoomRatioRange")
//            zoomRatioRange.value = nextZoomRatioRange
//        }
//        val maxDigitalZoom =
//            cameraCharacteristics[CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM]
//        Log.i(TAG, "fetchCurrentSupportedOutput: maxDigitalZoom $maxDigitalZoom")

//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
//            Log.i(
//                TAG,
//                "fetchCurrentSupportedOutput: physicalCameraIds ${cameraCharacteristics.physicalCameraIds}"
//            )
//        }
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
//            CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_LOGICAL_MULTI_CAMERA
//        }
//        val hardwareLevel =
//            cameraCharacteristics[CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL]
//        Log.i(TAG, "fetchCurrentSupportedOutput: hardwareLevel $hardwareLevel")

//        val faceDetectModes =
//            cameraCharacteristics[CameraCharacteristics.STATISTICS_INFO_AVAILABLE_FACE_DETECT_MODES]
//        val maxFaceCount =
//            cameraCharacteristics[CameraCharacteristics.STATISTICS_INFO_MAX_FACE_COUNT]
//        Log.i(
//            TAG,
//            "fetchCurrentSupportedOutput: ${Arrays.toString(faceDetectModes)} - $maxFaceCount"
//        )
//        availableFaceDetectModes.clear()
//        faceDetectModes?.forEach {
//            FaceDetectMode.getByCode(it)?.let { faceDetectMode ->
//                availableFaceDetectModes.add(faceDetectMode)
//            }
//        }
//        if (availableFaceDetectModes.isNotEmpty()) {
//            currentFaceDetectModeFlow.value = availableFaceDetectModes.last()
//        }
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