package com.jvziyaoyao.raw.camera.page.main

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CameraMetadata
import android.hardware.camera2.CaptureFailure
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.CaptureResult
import android.hardware.camera2.TotalCaptureResult
import android.hardware.camera2.params.OutputConfiguration
import android.hardware.camera2.params.SessionConfiguration
import android.os.Build
import android.os.Handler
import android.util.Range
import android.util.Size
import android.view.PixelCopy
import android.view.Surface
import android.view.SurfaceView
import java.util.concurrent.Executors
import java.util.stream.Collectors
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import kotlin.math.abs


/**
 * @program: TestFocusable
 *
 * @description:
 *
 * @author: JVZIYAOYAO
 *
 * @create: 2024-02-06 14:10
 **/
suspend fun SurfaceView.getTempBitmap() = suspendCoroutine<Bitmap?> { c ->
    if (width <= 0 && height <= 0) {
        c.resume(null)
        return@suspendCoroutine
    }
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    PixelCopy.request(
        holder.surface, bitmap,
        {
            if (PixelCopy.SUCCESS == it) {
                c.resume(bitmap)
            } else {
                c.resume(null)
            }
        }, handler
    )
}

fun SurfaceView.getScreenShot(): Bitmap {
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    draw(canvas)
    return bitmap
}

fun SurfaceView.getLockCanvas(): Bitmap {
    val canvas = holder.lockCanvas()
    val bitmap = Bitmap.createBitmap(canvas.width, canvas.height, Bitmap.Config.ARGB_8888)
    canvas.setBitmap(bitmap)
    holder.unlockCanvasAndPost(canvas)
    return bitmap
}

// 同步拍摄
suspend fun CameraCaptureSession.captureAsync(request: CaptureRequest, handler: Handler) =
    suspendCoroutine<CaptureResult> { c ->
        capture(request, object : CameraCaptureSession.CaptureCallback() {
            override fun onCaptureCompleted(
                session: CameraCaptureSession,
                request: CaptureRequest,
                result: TotalCaptureResult
            ) {
                super.onCaptureCompleted(session, request, result)
                c.resume(result)
            }

            override fun onCaptureFailed(
                session: CameraCaptureSession,
                request: CaptureRequest,
                failure: CaptureFailure
            ) {
                super.onCaptureFailed(session, request, failure)
                c.resumeWithException(RuntimeException("拍摄失败:[${failure}]"))
            }
        }, handler)
    }

suspend fun CameraDevice.createCameraSessionAsync(handler: Handler, surfaceList: List<Surface>) =
    suspendCoroutine<CameraCaptureSession> { c ->
        val cameraSessionStateCallback = object : CameraCaptureSession.StateCallback() {
            override fun onConfigured(session: CameraCaptureSession) {
                c.resume(session)
            }

            override fun onConfigureFailed(session: CameraCaptureSession) {
                c.resumeWithException(RuntimeException("CameraCaptureSession创建失败 $surfaceList"))
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val outputConfigurations = surfaceList.stream()
                .map { OutputConfiguration(it) }
                .collect(Collectors.toList())

            val sessionConfiguration = SessionConfiguration(
                SessionConfiguration.SESSION_REGULAR,
                outputConfigurations,
                Executors.newCachedThreadPool(),
                cameraSessionStateCallback
            )

            createCaptureSession(sessionConfiguration)
        } else {
            createCaptureSession(
                surfaceList,
                cameraSessionStateCallback,
                handler
            )
        }
    }

typealias CameraPair = Pair<String, CameraCharacteristics>

val Context.cameraManager: CameraManager
    get() = getSystemService(Context.CAMERA_SERVICE) as CameraManager

// 获取摄像头列表
fun Context.fetchCameraPairList(): List<CameraPair> {
    val cameraIdList = cameraManager.cameraIdList
    val nextCameraPairList = cameraIdList.map {
        val cameraCharacteristics = cameraManager.getCameraCharacteristics(it)
        Pair(it, cameraCharacteristics)
    }.filter {
        val capabilities = it.second.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)
        // 过滤出向后兼容(又叫向下兼容，兼容旧代码)的功能集
        capabilities?.contains(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_BACKWARD_COMPATIBLE)
            ?: false
    }
    return nextCameraPairList
}

// 选择一个默认的摄像头
fun chooseDefaultCameraPair(list: List<CameraPair>): CameraPair? {
    val frontCameraList = list.filter {
        val facing =
            it.second.get(CameraCharacteristics.LENS_FACING)
                ?: CameraMetadata.LENS_FACING_FRONT
        facing == CameraMetadata.LENS_FACING_BACK
//        facing == CameraMetadata.LENS_FACING_FRONT
    }
    return if (frontCameraList.isNotEmpty()) {
        frontCameraList.first()
    } else null
}

// 获取最合适的预览帧率
fun findBestFpgRange(rangeList: Array<Range<Int>>?): Range<Int>? {
    if (rangeList == null) return null
    return rangeList.toList().stream()
        .sorted { o2, o1 -> o1.lower.times(o1.upper).compareTo(o2.lower.times(o2.upper)) }
        .findFirst()
        .orElse(null)
}

// 获取面积最大的预览大小
fun findBestSize(sizeList: Array<Size>?, maxWidth: Int? = null): Size? {
    if (sizeList == null) return null
    return sizeList.toList().stream()
        .run {
            if (maxWidth != null) {
                filter { it.width <= maxWidth }
            } else this
        }
        .sorted { o2, o1 -> o1.width.times(o1.height).compareTo(o2.width.times(o2.height)) }
        .findFirst()
        .orElse(null)
}

fun getSizeByAspectRatio(outputSizeList: Array<Size>, targetAspectRatio: Float): List<Size> {
    return outputSizeList.toList().stream().filter { size ->
        val aspectRatio = size.width.toFloat() / size.height.toFloat()
        val aspectRatioDiff = abs(aspectRatio - targetAspectRatio)
        aspectRatioDiff == 0F
    }.collect(Collectors.toList())
}

fun configureTransform(displayOrientation: Int, width: Int, height: Int): Matrix {
    val matrix = Matrix()
    if (displayOrientation % 180 == 90) {
        // Rotate the camera preview when the screen is landscape.
        matrix.setPolyToPoly(
            floatArrayOf(
                0f, 0f,  // top left
                width.toFloat(), 0f,  // top right
                0f, height.toFloat(),  // bottom left
                width.toFloat(), height.toFloat()
            ), 0,
            if (displayOrientation == 90) floatArrayOf(
                0f, height.toFloat(),  // top left
                0f, 0f,  // top right
                width.toFloat(), height.toFloat(),  // bottom left
                width.toFloat(), 0f
            ) else floatArrayOf(
                width.toFloat(), 0f,  // top left
                width.toFloat(), height.toFloat(),  // top right
                0f, 0f,  // bottom left
                0f, height.toFloat()
            ), 0,
            4
        )
    } else if (displayOrientation == 180) {
        matrix.postRotate(180F, width.toFloat() / 2, height.toFloat() / 2)
    }
    return matrix
}

// 设置旋转角度
fun calculateOrientationOffsetToSensor(
    point: Pair<Float, Float>,
    orientation: Int,
    cameraFacing: Int,
): Pair<Float, Float> {
    val x = point.first
    val y = point.second
    return if (cameraFacing == CameraMetadata.LENS_FACING_FRONT) {
        when (orientation) {
            90 -> Pair(1 - y, x)
            180 -> point
            270 -> Pair(y, 1 - x)
            else -> Pair(y, x)
        }
    } else {
        when (orientation) {
            90 -> Pair(y, 1 - x)
            180 -> Pair(y, x)
            270 -> Pair(1 - y, x)
            else -> point
        }
    }
}

fun calculateOrientationOffsetFromSensor(
    point: Pair<Float, Float>,
    orientation: Int,
    cameraFacing: Int,
): Pair<Float, Float> {
    val x = point.first
    val y = point.second
    return if (cameraFacing == CameraMetadata.LENS_FACING_FRONT) {
        when (orientation) {
            90 -> Pair(y, 1 - x)
            180 -> point
            270 -> Pair(1 - y, x)
            else -> Pair(y, x)
        }
    } else {
        when (orientation) {
            90 -> Pair(1 - y,  x)
            180 -> Pair(y, x)
            270 -> Pair(y, 1 - x)
            else -> point
        }
    }
}