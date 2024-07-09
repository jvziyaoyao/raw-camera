package com.jvziyaoyao.camera.flow.holder.camera

import android.graphics.Bitmap
import org.opencv.android.Utils
import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.MatOfFloat
import org.opencv.core.MatOfInt
import org.opencv.core.Point
import org.opencv.core.Scalar
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc

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

// 计算峰值对焦区域
fun markShapeImageRegions(grayMat: Mat): Mat {
    // Compute gradient magnitude
    val gradX = Mat()
    val gradY = Mat()
    // Calculate gradient X
    Imgproc.Sobel(grayMat, gradX, CvType.CV_64F, 1, 0)
    Core.convertScaleAbs(gradX, gradX)
    // Calculate gradient Y
    Imgproc.Sobel(grayMat, gradY, CvType.CV_64F, 0, 1)
    Core.convertScaleAbs(gradY, gradY)
    // Combine the gradient images
    val gradientNorm = Mat()
    Core.addWeighted(gradX, 0.5, gradY, 0.5, 0.0, gradientNorm)
    Core.normalize(gradientNorm, gradientNorm, 0.0, 255.0, Core.NORM_MINMAX, CvType.CV_8UC1)
    Imgproc.threshold(gradientNorm, gradientNorm, 100.0, 255.0, Imgproc.THRESH_TOZERO)

    val alphaMat = Mat(gradientNorm.size(), CvType.CV_8UC4)
    val solidMat = Mat.zeros(gradientNorm.size(), CvType.CV_8UC3)
    val solidScalar = Scalar(0.0, 255.0, 0.0)
    solidMat.setTo(solidScalar)
    Core.merge(listOf(solidMat, gradientNorm), alphaMat)

    gradX.release()
    gradY.release()
    gradientNorm.release()
    return alphaMat
}

// 计算峰值亮度
fun markOverExposedRegions(zebraOffsetArr: IntArray, grayMat: Mat): Mat {
    // 将图像转换为灰度图
    val thresholdMat = Mat.zeros(grayMat.size(), CvType.CV_8UC1)
    Imgproc.threshold(grayMat, thresholdMat, 250.0, 255.0, Imgproc.THRESH_BINARY)
    // 创建斑马纹图案
    val zebraPattern = createZebraPattern(zebraOffsetArr, thresholdMat.size())
    // 找到白色部分并替换为斑马纹
    zebraPattern.copyTo(thresholdMat, thresholdMat)
    return thresholdMat
}

// 根据大小获取一个斑马纹的矩形
fun createZebraPattern(zebraOffsetArr: IntArray, size: org.opencv.core.Size): Mat {
    val zebraOffset = zebraOffsetArr[0]
    val zebraPattern = Mat.zeros(size, CvType.CV_8UC4)
    val stripeWidth = 8
    val stripeStep = stripeWidth * 3
    val stripeScalar = Scalar(0.0, 255.0, 255.0, 255.0)
    for (x in zebraOffset until zebraPattern.cols() step stripeStep) {
        val p1 = Point(x.toDouble(), 0.0)
        val p2 = Point(0.0, x.toDouble())
        Imgproc.line(zebraPattern, p1, p2, stripeScalar, stripeWidth, Imgproc.LINE_AA, 0)
    }
    for (y in zebraPattern.cols() + zebraOffset until zebraPattern.rows() step stripeStep) {
        val p1 = Point(0.0, y.toDouble())
        val p2 = Point(y.toDouble(), 0.0)
        Imgproc.line(zebraPattern, p1, p2, stripeScalar, stripeWidth, Imgproc.LINE_AA, 0)
    }
    for (x in zebraOffset until zebraPattern.cols() step stripeStep) {
        val p1 = Point(x.toDouble(), zebraPattern.rows().toDouble())
        val y2 = zebraPattern.rows() - zebraPattern.cols() + x.toDouble()
        val p2 = Point(zebraPattern.cols().toDouble(), y2)
        Imgproc.line(zebraPattern, p1, p2, stripeScalar, stripeWidth, Imgproc.LINE_AA, 0)
    }
    var nextZebraOffset = zebraOffset + 1
    if (nextZebraOffset > stripeStep) nextZebraOffset = 0
    zebraOffsetArr[0] = nextZebraOffset
    return zebraPattern
}

fun resizeMat(srcMat: Mat, newWidth: Int, newHeight: Int): Mat {
    val resizedMat = Mat()
    Imgproc.resize(srcMat, resizedMat, Size(newWidth.toDouble(), newHeight.toDouble()))
    return resizedMat
}