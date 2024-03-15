package com.jvziyaoyao.raw.camera.page.main

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ImageFormat
import android.graphics.Paint
import android.media.Image
import org.opencv.android.Utils
import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.MatOfFloat
import org.opencv.core.MatOfInt
import org.opencv.core.Scalar
import org.opencv.imgproc.Imgproc
import java.nio.ByteBuffer

/**
 * @program: TestFocusable
 *
 * @description:
 *
 * @author: JVZIYAOYAO
 *
 * @create: 2024-02-19 14:38
 **/

fun Bitmap.toMat(): Mat {
    // 将 Bitmap 转换为 OpenCV 的 Mat 对象
    val mat = Mat(height, width, CvType.CV_8UC4)
    Utils.bitmapToMat(this, mat)
    return mat
}

fun Mat.toBitmap(): Bitmap {
    val bitmap = Bitmap.createBitmap(cols(), rows(), Bitmap.Config.ARGB_8888)
    Utils.matToBitmap(this, bitmap)
    return bitmap
}

fun Mat.preMultiplyAlpha() {
    val rgbaChannels = mutableListOf<Mat>()
    Core.split(this, rgbaChannels)
    val alphaMat = rgbaChannels[3]
    for (i in 0 until 3) {
        Core.multiply(rgbaChannels[i], alphaMat, rgbaChannels[i], 1.0 / 255.0)
    }
    Core.merge(rgbaChannels, this)
    rgbaChannels.clear()
}

// 计算直方图
fun getHistogramData(grayMat: Mat): FloatArray {
    val hist = Mat()
    val ranges = MatOfFloat(0f, 256f) // 色彩范围
    val histSize = MatOfInt(256) // 直方图尺寸
    Imgproc.calcHist(listOf(grayMat), MatOfInt(0), Mat(), hist, histSize, ranges)
    val histRows = hist.rows()
    val histData = FloatArray(histRows)
    hist.get(0, 0, histData)
    return histData
}