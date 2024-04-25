package com.jvziyaoyao.camera.raw.holder.camera

import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CaptureResult
import android.hardware.camera2.params.Face
import android.hardware.camera2.params.MeteringRectangle
import android.os.Build

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

// 曝光时间
val CaptureResult.sensorExposureTime: Long?
    get() = get(CaptureResult.SENSOR_EXPOSURE_TIME)

// 感光度ISO
val CaptureResult.sensorSensitivity: Int?
    get() = get(CaptureResult.SENSOR_SENSITIVITY)

// 电子变焦
val CaptureResult.zoomRatio: Float?
    get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        get(CaptureResult.CONTROL_ZOOM_RATIO)
    } else null

// 相机场景模式
val CaptureResult.sceneMode: Int?
    get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        get(CaptureResult.CONTROL_SCENE_MODE)
    } else null

// 光学防抖模式
val CaptureResult.oisMode: Int?
    get() = get(CaptureResult.LENS_OPTICAL_STABILIZATION_MODE)

val CaptureResult.oisEnable: Boolean
    get() = oisMode == CaptureResult.LENS_OPTICAL_STABILIZATION_MODE_ON

val CaptureResult.afTrigger: Int?
    get() = get(CaptureResult.CONTROL_AF_TRIGGER)

val CaptureResult.aeTrigger: Int?
    get() = get(CaptureResult.CONTROL_AE_PRECAPTURE_TRIGGER)

val CaptureResult.afMode: Int?
    get() = get(CaptureResult.CONTROL_AF_MODE)

val CaptureResult.afState: Int?
    get() = get(CaptureResult.CONTROL_AF_STATE)

val CaptureResult.aeState: Int?
    get() = get(CaptureResult.CONTROL_AE_STATE)

val CaptureResult.awbState: Int?
    get() = get(CaptureResult.CONTROL_AWB_STATE)

val CaptureResult.afRegions: Array<MeteringRectangle>?
    get() = get(CaptureResult.CONTROL_AF_REGIONS)

val CaptureResult.aeRegions: Array<MeteringRectangle>?
    get() = get(CaptureResult.CONTROL_AE_REGIONS)

val CaptureResult.awbRegions: Array<MeteringRectangle>?
    get() = get(CaptureResult.CONTROL_AWB_REGIONS)

val CaptureResult.faceDetectResult: Array<Face>?
    get() = get(CaptureResult.STATISTICS_FACES)