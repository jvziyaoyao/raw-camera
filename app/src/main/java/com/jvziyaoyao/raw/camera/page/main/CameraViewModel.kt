package com.jvziyaoyao.raw.camera.page.main

import android.graphics.Bitmap
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraMetadata
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.CaptureResult
import android.hardware.camera2.params.MeteringRectangle
import android.hardware.camera2.params.RggbChannelVector
import android.os.Build
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.jvziyaoyao.raw.camera.domain.clean.usecase.SensorUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking

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

data class FocusRequestTrigger(
    var focusRequest: Boolean,
    var focusCancel: Boolean,
)

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
            if (faceDetectModes.isNotEmpty()) {
                currentFaceDetectModeFlow.value = faceDetectModes.last()
            }
            if (oisAvailable) {
                oisEnableFlow.value = true
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

    /**
     *
     * 曝光控制相关
     *
     */

    val aeCompensationFlow = MutableStateFlow(0)

    val focalDistanceFlow = MutableStateFlow<Float?>(null)

    val sensorSensitivityFlow = MutableStateFlow<Int?>(null)

    val sensorExposureTimeFlow = MutableStateFlow<Long?>(null)

    val customTemperatureFlow = MutableStateFlow<Int?>(null)

    val zoomRatioFlow = MutableStateFlow(1F)

    val oisEnableFlow = MutableStateFlow(false)

    val currentSceneModeFlow = MutableStateFlow<SceneMode?>(null)

    val focusPointRectFlow = MutableStateFlow<androidx.compose.ui.geometry.Rect?>(null)

    val focusRequestTriggerFlow = MutableStateFlow<FocusRequestTrigger?>(null)

    val currentFaceDetectModeFlow = MutableStateFlow<FaceDetectMode?>(null)

    val focusRequestOrientation = mutableStateOf<FocusRequestOrientation?>(null)

    val manualSensorParamsFlow = combine(
        listOf(
            aeCompensationFlow,
            sensorSensitivityFlow,
            sensorExposureTimeFlow,
            focalDistanceFlow,
            customTemperatureFlow,
            oisEnableFlow,
            currentSceneModeFlow,
            zoomRatioFlow,
            focusRequestTriggerFlow,
            currentFaceDetectModeFlow,
        )
    ) { list -> list }

    val afEnableFlow = focalDistanceFlow.map { it == null }

    val aeEnableFlow = combine(
        sensorSensitivityFlow,
        sensorExposureTimeFlow,
    ) { sensorSensitivity, sensorExposureTime ->
        sensorSensitivity == null && sensorExposureTime == null
    }

    val awbEnableFlow = customTemperatureFlow.map { it == null }

    fun setAfEnable() {
        focalDistanceFlow.value = null
    }

    fun setAeEnable() {
        sensorSensitivityFlow.value = null
        sensorExposureTimeFlow.value = null
    }

    fun setAwbEnable() {
        customTemperatureFlow.value = null
    }

    fun setCurrentCaptureParams(builder: CaptureRequest.Builder) {
        Log.i(TAG, "setCurrentCaptureParams: ${sensorSensitivityFlow.value}")
        val cameraPair = currentCameraPairFlow.value
        val characteristics = cameraPair?.second
        builder.apply {
            val captureMode = captureModeFlow.value

            if (captureMode == CaptureMode.MANUAL) {
                val afEnable = runBlocking { afEnableFlow.first() }
                val aeEnable = runBlocking { aeEnableFlow.first() }
                val awbEnable = runBlocking { awbEnableFlow.first() }

                if (afEnable) {
                    set(
                        CaptureRequest.CONTROL_AF_MODE,
                        CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE
                    )
                } else {
                    set(
                        CaptureRequest.CONTROL_AF_MODE,
                        CaptureRequest.CONTROL_AF_MODE_OFF
                    )
                    val focalDistance = focalDistanceFlow.value
                    set(CaptureRequest.LENS_FOCUS_DISTANCE, focalDistance)
                }

                if (aeEnable) {
                    set(
                        CaptureRequest.CONTROL_AE_MODE,
                        CaptureRequest.CONTROL_AE_MODE_ON
                    )
                } else {
                    set(
                        CaptureRequest.CONTROL_AE_MODE,
                        CaptureRequest.CONTROL_AE_MODE_OFF
                    )
                    val sensorSensitivity = sensorSensitivityFlow.value
                    val sensorExposureTime = sensorExposureTimeFlow.value
                    set(CaptureRequest.SENSOR_EXPOSURE_TIME, sensorExposureTime)
                    set(CaptureRequest.SENSOR_SENSITIVITY, sensorSensitivity)
                }

                if (awbEnable) {
                    set(
                        CaptureRequest.CONTROL_AWB_MODE,
                        CaptureResult.CONTROL_AWB_MODE_AUTO,
                    )
                } else {
                    set(
                        CaptureRequest.CONTROL_AWB_MODE,
                        CaptureResult.CONTROL_AWB_MODE_OFF,
                    )
                    val customTemperature = customTemperatureFlow.value
                    val rggbChannelVector = calcTemperature(customTemperature!!)
                    set(
                        CaptureRequest.COLOR_CORRECTION_MODE,
                        CameraMetadata.COLOR_CORRECTION_MODE_TRANSFORM_MATRIX
                    )
                    set(CaptureRequest.COLOR_CORRECTION_GAINS, rggbChannelVector)
                }
            } else {
                val aeCompensation = aeCompensationFlow.value
                set(
                    CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION,
                    aeCompensation
                )
            }

            val currentSceneMode = currentSceneModeFlow.value
            if (currentSceneMode != null) {
                set(
                    CaptureRequest.CONTROL_SCENE_MODE,
                    currentSceneMode.code,
                )
            }

            if (characteristics?.oisAvailable == true) {
                set(
                    CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE,
                    if (oisEnableFlow.value) CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE_ON
                    else CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE_OFF,
                )
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                set(
                    CaptureRequest.CONTROL_ZOOM_RATIO,
                    zoomRatioFlow.value
                )
            }

            currentFaceDetectModeFlow.value?.let {
                set(
                    CaptureRequest.STATISTICS_FACE_DETECT_MODE,
                    it.code
                )
            }

            focusRequestTriggerFlow.value?.apply {
                Log.i(
                    TAG,
                    "setCurrentCaptureParams: onCaptureCompleted focusRequestTriggerFlow $this"
                )
                if (focusRequest) {
                    focusPointRectFlow.value?.let { focusPointRect ->
                        val flipHorizontal = characteristics?.isFrontCamera == true
                        val sensorSize = characteristics?.sensorSize
                        if (sensorSize != null) {
                            val rect = composeRect2SensorRect(
                                rect = focusPointRect,
                                rotationOrientation = rotationOrientation.value,
                                flipHorizontal = flipHorizontal,
                                sensorWidth = sensorSize.width(),
                                sensorHeight = sensorSize.height(),
                            )
                            val meteringRectangle =
                                MeteringRectangle(rect, MeteringRectangle.METERING_WEIGHT_MAX)

                            set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_AUTO)
                            set(CaptureRequest.CONTROL_AF_REGIONS, arrayOf(meteringRectangle))
                            set(
                                CaptureRequest.CONTROL_AF_TRIGGER,
                                CameraMetadata.CONTROL_AF_TRIGGER_START
                            )

                            set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON)
                            set(CaptureRequest.CONTROL_AE_REGIONS, arrayOf(meteringRectangle))
                            set(
                                CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER,
                                CameraMetadata.CONTROL_AE_PRECAPTURE_TRIGGER_START
                            )
                        }
                    }
                    focusRequest = false
                }
                if (focusCancel) {
                    set(
                        CaptureRequest.CONTROL_AF_TRIGGER,
                        CameraMetadata.CONTROL_AF_TRIGGER_IDLE
                    )
                    set(CaptureRequest.CONTROL_AF_REGIONS, null)
                    set(
                        CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER,
                        CameraMetadata.CONTROL_AE_PRECAPTURE_TRIGGER_IDLE
                    )
                    set(CaptureRequest.CONTROL_AE_REGIONS, null)
                }
            }
        }
    }

    fun focusRequest(rect: androidx.compose.ui.geometry.Rect) {
        focusRequestOrientation.value = FocusRequestOrientation(
            pitch = pitchFlow.value,
            roll = rollFlow.value,
            yaw = yawFlow.value,
        )
        focusPointRectFlow.value = rect
        focusRequestTriggerFlow.value = FocusRequestTrigger(
            focusRequest = true,
            focusCancel = false
        )
    }

    fun focusCancel() {
        focusRequestOrientation.value = null
        focusRequestTriggerFlow.value = FocusRequestTrigger(
            focusRequest = false,
            focusCancel = true,
        )
        Log.i(TAG, "focusCancel: 取消对焦～")
    }

}

fun calcTemperature(factor: Int): RggbChannelVector {
    return RggbChannelVector(
        0.635f + (0.0208333f * factor),
        1.0f,
        1.0f,
        3.7420394f + (-0.0287829f * factor)
    )
}