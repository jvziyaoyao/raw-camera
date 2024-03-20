package com.jvziyaoyao.raw.camera.page.main

import android.graphics.Bitmap
import android.graphics.ImageFormat
import android.graphics.Rect
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraMetadata
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.CaptureResult
import android.hardware.camera2.params.Face
import android.hardware.camera2.params.MeteringRectangle
import android.hardware.camera2.params.RggbChannelVector
import android.os.Build
import android.util.Log
import android.util.Range
import android.util.Size
import android.view.SurfaceHolder
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import java.util.Arrays

/**
 * @program: TestFocusable
 *
 * @description:
 *
 * @author: JVZIYAOYAO
 *
 * @create: 2024-02-02 11:48
 **/

enum class OutputMode(
    val label: String,
    val imageFormat: Int,
) {
    JPEG(
        label = "JPEG",
        imageFormat = ImageFormat.JPEG,
    ),
    HEIC(
        label = "HEIC",
        imageFormat = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) ImageFormat.HEIC else -1,
    ),
    RAW(
        label = "RAW",
        imageFormat = ImageFormat.RAW_SENSOR,
    ),
    ;
}

enum class SceneMode(
    val code: Int,
) {
    FACE_PRIORITY(
        code = CameraCharacteristics.CONTROL_SCENE_MODE_FACE_PRIORITY,
    ),
    ACTION(
        code = CameraCharacteristics.CONTROL_SCENE_MODE_ACTION,
    ),
    PORTRAIT(
        code = CameraCharacteristics.CONTROL_SCENE_MODE_PORTRAIT,
    ),
    LANDSCAPE(
        code = CameraCharacteristics.CONTROL_SCENE_MODE_LANDSCAPE,
    ),
    NIGHT(
        code = CameraCharacteristics.CONTROL_SCENE_MODE_NIGHT,
    ),
    NIGHT_PORTRAIT(
        code = CameraCharacteristics.CONTROL_SCENE_MODE_NIGHT_PORTRAIT,
    ),
    THEATRE(
        code = CameraCharacteristics.CONTROL_SCENE_MODE_THEATRE,
    ),
    BEACH(
        code = CameraCharacteristics.CONTROL_SCENE_MODE_BEACH,
    ),
    SNOW(
        code = CameraCharacteristics.CONTROL_SCENE_MODE_SNOW,
    ),
    SUNSET(
        code = CameraCharacteristics.CONTROL_SCENE_MODE_SUNSET,
    ),
    STEADYPHOTO(
        code = CameraCharacteristics.CONTROL_SCENE_MODE_STEADYPHOTO,
    ),
    FIREWORKS(
        code = CameraCharacteristics.CONTROL_SCENE_MODE_FIREWORKS,
    ),
    SPORTS(
        code = CameraCharacteristics.CONTROL_SCENE_MODE_SPORTS,
    ),
    PARTY(
        code = CameraCharacteristics.CONTROL_SCENE_MODE_PARTY,
    ),
    CANDLELIGHT(
        code = CameraCharacteristics.CONTROL_SCENE_MODE_CANDLELIGHT,
    ),
    BARCODE(
        code = CameraCharacteristics.CONTROL_SCENE_MODE_BARCODE,
    ),
    HIGH_SPEED_VIDEO(
        code = CameraCharacteristics.CONTROL_SCENE_MODE_HIGH_SPEED_VIDEO,
    ),
    HDR(
        code = CameraCharacteristics.CONTROL_SCENE_MODE_HDR,
    ),
    ;

    companion object {
        fun getByCode(code: Int): SceneMode? {
            for (value in entries) {
                if (value.code == code) return value
            }
            return null
        }
    }
}

data class OutputItem(
    val outputMode: OutputMode,
    val bestSize: Size,
)

enum class CaptureMode() {
    AUTO,
    MANUAL,
    ;
}

enum class FaceDetectMode(
    val code: Int,
    val label: String,
) {
    OFF(
        code = CameraCharacteristics.STATISTICS_FACE_DETECT_MODE_OFF,
        label = "OFF",
    ),
    SIMPLE(
        code = CameraCharacteristics.STATISTICS_FACE_DETECT_MODE_SIMPLE,
        label = "SIMPLE",
    ),
    FULL(
        code = CameraCharacteristics.STATISTICS_FACE_DETECT_MODE_FULL,
        label = "FULL",
    ),
    ;

    companion object {
        fun getByCode(code: Int): FaceDetectMode? {
            for (value in FaceDetectMode.entries) {
                if (value.code == code) return value
            }
            return null
        }
    }
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

class Camera2ViewModel : ViewModel() {

    private val TAG = Camera2ViewModel::class.java.name

    val imageAspectRatio = 4F.div(3F)

    val resumeTimestampFlow = MutableStateFlow<Long?>(null)

    val cameraPairListFlow =
        MutableStateFlow(emptyList<Pair<String, CameraCharacteristics>>())

    val currentCameraPairFlow = MutableStateFlow<Pair<String, CameraCharacteristics>?>(null)

    val outputSupportedItemList = mutableStateListOf<OutputItem>()

    val currentOutputItemFlow = MutableStateFlow<OutputItem?>(null)

    val captureLoading = mutableStateOf(false)

    val rotationOrientation = mutableStateOf(0)

    val cameraFacing = mutableStateOf(CameraMetadata.LENS_FACING_BACK)

    val sensorSize = mutableStateOf(Rect(0, 0, 4000, 3000))

    val sensorExposureTime = mutableStateOf(0L)

    val sensorSensitivity = mutableStateOf(0)

    val zoomRatioFlow = MutableStateFlow(1F)

    val zoomRatioRange = mutableStateOf<Range<Float>?>(null)

    val previewZoomRatio = mutableStateOf<Float?>(null)

    val oisAvailable = mutableStateOf(false)

    val oisEnableFlow = MutableStateFlow(false)

    val previewOisEnable = mutableStateOf(false)

    val sceneModeAvailableList = mutableStateListOf<SceneMode>()

    val currentSceneModeFlow = MutableStateFlow<SceneMode?>(null)

    val previewSceneMode = mutableStateOf<SceneMode?>(null)

    val allPermissionGrantedFlow = MutableStateFlow(false)

    val captureModeFlow = MutableStateFlow(CaptureMode.MANUAL)

    val frameRate = mutableStateOf(0)

    val rendererFrameRate = mutableStateOf(0)

    val focusPeakingEnableFlow = MutableStateFlow(false)

    val brightnessPeakingEnableFlow = MutableStateFlow(false)

    val exposureHistogramEnableFlow = MutableStateFlow(true)

    val focusPointRectFlow = MutableStateFlow<androidx.compose.ui.geometry.Rect?>(null)

    val previewAFState = mutableStateOf<Int?>(null)

    val previewAEState = mutableStateOf<Int?>(null)

    val previewAWBState = mutableStateOf<Int?>(null)

    val previewAFRegions = mutableStateOf<List<MeteringRectangle>?>(null)

    val previewAERegions = mutableStateOf<List<MeteringRectangle>?>(null)

    val previewAWBRegions = mutableStateOf<List<MeteringRectangle>?>(null)

    val availableFaceDetectModes = mutableStateListOf<FaceDetectMode>()

    val currentFaceDetectModeFlow = MutableStateFlow<FaceDetectMode?>(null)

    val previewFaceDetectResult = mutableStateListOf<Face>()

    fun fetchCurrentSupportedOutput(cameraCharacteristics: CameraCharacteristics) {
        val scaleStreamConfigurationMap = cameraCharacteristics.get(
            CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP
        ) ?: throw RuntimeException("流配置列表为空！")
        val outputFormats = scaleStreamConfigurationMap.outputFormats

        outputSupportedItemList.clear()
        OutputMode.entries.forEach { outputMode ->
            if (outputFormats.contains(outputMode.imageFormat)) {
                val outputSizes = scaleStreamConfigurationMap.getOutputSizes(outputMode.imageFormat)
                val aspectList = getSizeByAspectRatio(outputSizes, imageAspectRatio)
                val bestSize = findBestSize(aspectList.toTypedArray())
                if (bestSize != null) {
                    outputSupportedItemList.add(OutputItem(outputMode, bestSize))
                }
            }
        }
        if (!outputSupportedItemList.isEmpty()) {
            currentOutputItemFlow.value = outputSupportedItemList[0]
        }

        scaleStreamConfigurationMap.apply {
            outputFormats?.forEach { format ->
                val label = when (format) {
                    ImageFormat.JPEG -> "JPEG"
                    ImageFormat.DEPTH_JPEG -> "DEPTH_JPEG"
                    ImageFormat.RAW_SENSOR -> "RAW_SENSOR"
                    ImageFormat.RAW_PRIVATE -> "RAW_PRIVATE"
                    ImageFormat.RAW10 -> "RAW10"
                    ImageFormat.RAW12 -> "RAW12"
                    ImageFormat.NV21 -> "NV21"
                    ImageFormat.NV16 -> "NV16"
                    ImageFormat.YUV_420_888 -> "YUV_420_888"
                    ImageFormat.YUV_422_888 -> "YUV_422_888"
                    ImageFormat.YUV_444_888 -> "YUV_444_888"
                    ImageFormat.PRIVATE -> "PRIVATE"
                    ImageFormat.HEIC -> "HEIC"
                    else -> "OTHER - $format"
                }
                Log.i(TAG, "openCamera: image format label: $label")
                val sizeList = getOutputSizes(format)
                sizeList.forEach { size ->
                    Log.i(TAG, "scaleStreamConfigurationMap getOutputSizes: $size")
                }
            }
            val surfaceHolderOutputSizeList = getOutputSizes(SurfaceHolder::class.java)
            Log.i(TAG, "openCamera: image format label: ${SurfaceHolder::class.java.name}")
            surfaceHolderOutputSizeList.forEach { size ->
                Log.i(TAG, "scaleStreamConfigurationMap: getOutputSizes $size")
            }
        }

        aeCompensationRange.value =
            cameraCharacteristics[CameraCharacteristics.CONTROL_AE_COMPENSATION_RANGE]

        sensorSensitivityRange.value =
            cameraCharacteristics.get(CameraCharacteristics.SENSOR_INFO_SENSITIVITY_RANGE)
        sensorExposureTimeRange.value =
            cameraCharacteristics.get(CameraCharacteristics.SENSOR_INFO_EXPOSURE_TIME_RANGE)

        val minimumFocusDistance =
            cameraCharacteristics[CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE]
        if (minimumFocusDistance != null) {
            focalDistanceRange.value = Range(0.0F, minimumFocusDistance)
        }

        val ois =
            cameraCharacteristics[CameraCharacteristics.LENS_INFO_AVAILABLE_OPTICAL_STABILIZATION]
        oisAvailable.value =
            ois?.contains(CameraCharacteristics.CONTROL_VIDEO_STABILIZATION_MODE_ON) == true
        Log.i(
            TAG,
            "fetchCurrentSupportedOutput: ois ${Arrays.toString(ois)} - ${oisAvailable.value}"
        )

        val sceneModes = cameraCharacteristics[CameraCharacteristics.CONTROL_AVAILABLE_SCENE_MODES]
        sceneModeAvailableList.clear()
        sceneModes?.onEach { m ->
            SceneMode.getByCode(m)?.let { sceneModeAvailableList.add(it) }
        }

        val hasHDRMode = sceneModes?.contains(CameraCharacteristics.CONTROL_SCENE_MODE_HDR) == true
        Log.i(
            TAG,
            "fetchCurrentSupportedOutput: sceneModes ${Arrays.toString(sceneModes)} - $hasHDRMode"
        )
        val availableFocalLengths =
            cameraCharacteristics[CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS]
        Log.i(
            TAG,
            "fetchCurrentSupportedOutput: availableFocalLengths ${
                Arrays.toString(availableFocalLengths)
            }"
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val nextZoomRatioRange =
                cameraCharacteristics[CameraCharacteristics.CONTROL_ZOOM_RATIO_RANGE]
            Log.i(TAG, "fetchCurrentSupportedOutput: nextZoomRatioRange $nextZoomRatioRange")
            zoomRatioRange.value = nextZoomRatioRange
        }
        val maxDigitalZoom =
            cameraCharacteristics[CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM]
        Log.i(TAG, "fetchCurrentSupportedOutput: maxDigitalZoom $maxDigitalZoom")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            Log.i(
                TAG,
                "fetchCurrentSupportedOutput: physicalCameraIds ${cameraCharacteristics.physicalCameraIds}"
            )
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_LOGICAL_MULTI_CAMERA
        }
        CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY
        val hardwareLevel =
            cameraCharacteristics[CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL]
        Log.i(TAG, "fetchCurrentSupportedOutput: hardwareLevel $hardwareLevel")

        val faceDetectModes =
            cameraCharacteristics[CameraCharacteristics.STATISTICS_INFO_AVAILABLE_FACE_DETECT_MODES]
        val maxFaceCount =
            cameraCharacteristics[CameraCharacteristics.STATISTICS_INFO_MAX_FACE_COUNT]
        Log.i(
            TAG,
            "fetchCurrentSupportedOutput: ${Arrays.toString(faceDetectModes)} - $maxFaceCount"
        )
        availableFaceDetectModes.clear()
        faceDetectModes?.forEach {
            FaceDetectMode.getByCode(it)?.let { faceDetectMode ->
                availableFaceDetectModes.add(faceDetectMode)
            }
        }
        if (availableFaceDetectModes.isNotEmpty()) {
            currentFaceDetectModeFlow.value = availableFaceDetectModes.last()
        }
    }

    val surfaceBitmap = mutableStateOf<Bitmap?>(null)

    val exposureHistogramDataFlow = MutableStateFlow<FloatArray?>(null)

    /**
     *
     * 曝光控制相关
     *
     */

    val aeCompensationRange = mutableStateOf<Range<Int>?>(null)

    val aeCompensationFlow = MutableStateFlow(0)

    val focalDistanceRange = mutableStateOf<Range<Float>?>(null)

    val focalDistanceFlow = MutableStateFlow<Float?>(null)

    val sensorSensitivityRange = mutableStateOf<Range<Int>?>(null)

    val sensorSensitivityFlow = MutableStateFlow<Int?>(null)

    val sensorExposureTimeRange = mutableStateOf<Range<Long>?>(null)

    val sensorExposureTimeFlow = MutableStateFlow<Long?>(null)

    val customTemperatureFlow = MutableStateFlow<Int?>(null)

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
            focusPointRectFlow,
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

    fun setAfEnable(enable: Boolean) {
        if (!enable) return
        focalDistanceFlow.value = null
    }

    fun setAeEnable(enable: Boolean) {
        if (!enable) return
        sensorSensitivityFlow.value = null
        sensorExposureTimeFlow.value = null
    }

    fun setAwbEnable(enable: Boolean) {
        if (!enable) return
        customTemperatureFlow.value = null
    }

    fun setCurrentCaptureParams(builder: CaptureRequest.Builder) {
        builder.apply {
            val captureMode = captureModeFlow.value

            if (captureMode == CaptureMode.MANUAL) {
                val afEnable = runBlocking { afEnableFlow.first() }
                val aeEnable = runBlocking { aeEnableFlow.first() }
                val awbEnable = runBlocking { awbEnableFlow.first() }

                if (afEnable) {
                    set(
                        CaptureRequest.CONTROL_AF_MODE,
                        CaptureRequest.CONTROL_AF_MODE_AUTO
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

            if (oisAvailable.value) {
                set(
                    CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE,
                    if (oisEnableFlow.value) CameraCharacteristics.LENS_OPTICAL_STABILIZATION_MODE_ON
                    else CameraCharacteristics.LENS_OPTICAL_STABILIZATION_MODE_OFF,
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

            // TODO 合焦后要更改为连续对焦，否则会一直对焦
            focusPointRectFlow.value?.let {
                val rect = composeRect2SensorRect(
                    rect = it,
                    rotationOrientation = rotationOrientation.value,
                    cameraFacing = cameraFacing.value,
                    sensorWidth = sensorSize.value.width(),
                    sensorHeight = sensorSize.value.height(),
                )
                val meteringRectangle =
                    MeteringRectangle(rect, MeteringRectangle.METERING_WEIGHT_MAX)
                set(CaptureRequest.CONTROL_AF_REGIONS, arrayOf(meteringRectangle))
                set(CaptureRequest.CONTROL_AE_REGIONS, arrayOf(meteringRectangle))
                set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_AUTO)
                set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_START)
                set(
                    CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER,
                    CameraMetadata.CONTROL_AE_PRECAPTURE_TRIGGER_START
                )
            }

        }
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