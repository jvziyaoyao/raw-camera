package com.jvziyaoyao.raw.camera.page.main

import android.graphics.ImageFormat
import android.graphics.Rect
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.params.StreamConfigurationMap
import android.os.Build
import android.util.Range
import android.util.Size

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

data class OutputItem(
    val outputMode: OutputMode,
    val bestSize: Size,
)

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

val CameraCharacteristics.scaleStreamMap: StreamConfigurationMap?
    get() = this[CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP]

val CameraCharacteristics.outputSupportedMode: List<OutputItem>
    get() = mutableListOf<OutputItem>().apply om@{
        scaleStreamMap?.apply {
            OutputMode.entries.forEach { outputMode ->
                if (outputFormats.contains(outputMode.imageFormat)) {
                    val outputSizes = getOutputSizes(outputMode.imageFormat)
                    val aspectList = getSizeByAspectRatio(outputSizes, sensorAspectRatio)
                    val bestSize = findBestSize(aspectList.toTypedArray())
                    if (bestSize != null) {
                        this@om.add(OutputItem(outputMode, bestSize))
                    }
                }
            }
        }
    }

val CameraCharacteristics.aeCompensationRange: Range<Int>?
    get() = this[CameraCharacteristics.CONTROL_AE_COMPENSATION_RANGE]

val CameraCharacteristics.minimumFocusDistance: Float?
    get() = this[CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE]

val CameraCharacteristics.focalDistanceRange: Range<Float>?
    get() = minimumFocusDistance?.run { Range(0.0F, this@run) }

val CameraCharacteristics.sensorSensitivityRange: Range<Int>?
    get() = this[CameraCharacteristics.SENSOR_INFO_SENSITIVITY_RANGE]

val CameraCharacteristics.sensorExposureTimeRange: Range<Long>?
    get() = this[CameraCharacteristics.SENSOR_INFO_EXPOSURE_TIME_RANGE]

val CameraCharacteristics.zoomRatioRange: Range<Float>?
    get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        this[CameraCharacteristics.CONTROL_ZOOM_RATIO_RANGE]
    } else null

val CameraCharacteristics.oisMode: IntArray?
    get() = this[CameraCharacteristics.LENS_INFO_AVAILABLE_OPTICAL_STABILIZATION]

val CameraCharacteristics.oisAvailable: Boolean
    get() = oisMode?.contains(CameraCharacteristics.CONTROL_VIDEO_STABILIZATION_MODE_ON) == true

val CameraCharacteristics.sceneModeAvailableList: IntArray?
    get() = this[CameraCharacteristics.CONTROL_AVAILABLE_SCENE_MODES]

val CameraCharacteristics.sceneModes: List<SceneMode>
    get() = mutableListOf<SceneMode>().apply {
        sceneModeAvailableList?.forEach { m ->
            SceneMode.getByCode(m)?.let { add(it) }
        }
    }

val CameraCharacteristics.availableFaceDetectModes: IntArray?
    get() = this[CameraCharacteristics.STATISTICS_INFO_AVAILABLE_FACE_DETECT_MODES]

val CameraCharacteristics.faceDetectModes: List<FaceDetectMode>
    get() = mutableListOf<FaceDetectMode>().apply {
        availableFaceDetectModes?.forEach { m ->
            FaceDetectMode.getByCode(m)?.let { add(it) }
        }
    }

val CameraCharacteristics.cameraFacing: Int?
    get() = this[CameraCharacteristics.LENS_FACING]

val CameraCharacteristics.isFrontCamera: Boolean
    get() = cameraFacing == CameraCharacteristics.LENS_FACING_FRONT

val CameraCharacteristics.sensorSize: Rect?
    get() = this[CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE]

val CameraCharacteristics.sensorOrientation: Int?
    get() = this[CameraCharacteristics.SENSOR_ORIENTATION]

val CameraCharacteristics.sensorAspectRatio: Float
    get() = if (sensorSize != null) sensorSize!!.run {
        width().toFloat().div(height())
    } else defaultSensorAspectRatio

const val defaultSensorAspectRatio = 4F.div(3F)

//fun StreamConfigurationMap.printOutputFormat(): String {
//    val imageFormatClass = ImageFormat::class.java
//    outputFormats.forEach {
//        imageFormatClass.fields.forEach { field ->
//
//        }
//    }
//}