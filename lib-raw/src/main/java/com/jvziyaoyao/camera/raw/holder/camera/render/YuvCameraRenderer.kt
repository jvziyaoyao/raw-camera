package com.jvziyaoyao.camera.raw.holder.camera.render

import android.media.Image
import android.opengl.GLSurfaceView
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import com.jvziyaoyao.camera.raw.holder.camera.CameraSurfaceRenderer
import com.jvziyaoyao.camera.raw.holder.camera.TEX_VERTEX_MAT_0
import com.jvziyaoyao.camera.raw.holder.camera.TEX_VERTEX_MAT_180
import com.jvziyaoyao.camera.raw.holder.camera.TEX_VERTEX_MAT_270
import com.jvziyaoyao.camera.raw.holder.camera.TEX_VERTEX_MAT_90
import com.jvziyaoyao.camera.raw.holder.camera.YUVRenderData
import com.jvziyaoyao.camera.raw.holder.camera.getHistogramData
import com.jvziyaoyao.camera.raw.holder.camera.markOverExposedRegions
import com.jvziyaoyao.camera.raw.holder.camera.markShapeImageRegions
import com.jvziyaoyao.camera.raw.holder.camera.preMultiplyAlpha
import com.jvziyaoyao.camera.raw.holder.camera.vertexHorizontalFlip
import com.jvziyaoyao.camera.raw.util.testTime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import org.opencv.android.OpenCVLoader
import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import java.nio.ByteBuffer

class YuvCameraRenderer : CoroutineScope by MainScope() {

    private val TAG = YuvCameraRenderer::class.java.name

    private var glSurfaceViewFlow = MutableStateFlow<GLSurfaceView?>(null)

    private val cameraSurfaceRender = CameraSurfaceRenderer(TEX_VERTEX_MAT_0)

    private var frameCount = 0

    private val yuvDataFlow = MutableStateFlow<YUVRenderData?>(null)

    private val grayMatFlow = MutableStateFlow<Mat?>(null)

    private val focusPeakingMatFlow = MutableStateFlow<Mat?>(null)

    private val brightnessPeakingMatFlow = MutableStateFlow<Mat?>(null)

    val captureFrameRate = mutableStateOf(0)

    val rendererFrameRate = mutableStateOf(0)

    val focusPeakingEnableFlow = MutableStateFlow(false)

    val brightnessPeakingEnableFlow = MutableStateFlow(false)

    val exposureHistogramEnableFlow = MutableStateFlow(true)

    val exposureHistogramDataFlow = MutableStateFlow<FloatArray?>(null)

    val resumeTimestampFlow = MutableStateFlow<Long?>(null)

    init {
        // 初始化OpenCV
        OpenCVLoader.initLocal()
    }

    fun setSurfaceView(glSurfaceView: GLSurfaceView) {
        glSurfaceViewFlow.value = glSurfaceView
        glSurfaceView.setEGLContextClientVersion(3)
        glSurfaceView.setRenderer(cameraSurfaceRender)
    }

    fun upDateVertex(isFrontCamera: Boolean, rotationOrientation: Int) {
        var nextTextureVertex = when (rotationOrientation) {
            0 -> TEX_VERTEX_MAT_0
            90 -> TEX_VERTEX_MAT_90
            180 -> TEX_VERTEX_MAT_180
            270 -> TEX_VERTEX_MAT_270
            else -> TEX_VERTEX_MAT_90
        }
        if (isFrontCamera) nextTextureVertex =
            vertexHorizontalFlip(nextTextureVertex)
        cameraSurfaceRender.currentYuvData = null
        cameraSurfaceRender.updateTextureBuffer(nextTextureVertex)
    }

    fun setupRenderer() {
        launch {
            var job: Job? = null
            resumeTimestampFlow.collectLatest {
                launch {
                    val resumeTimestamp = resumeTimestampFlow.value
                    if (resumeTimestamp != null) {
                        job?.cancel()
                        job = launch(Dispatchers.IO) {
                            var currentCount = frameCount
                            var currentRenderCount = cameraSurfaceRender.frameCount
                            while (resumeTimestampFlow.value != null) {
                                delay(1000)
                                Log.i(
                                    TAG,
                                    "onCreate: calc frame 计算帧率： $frameCount - ${cameraSurfaceRender.frameCount}"
                                )
                                captureFrameRate.value = frameCount - currentCount
                                rendererFrameRate.value =
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
            combine(grayMatFlow, exposureHistogramEnableFlow) { t01, t02 ->
                arrayOf(t01, t02)
            }.collectLatest {
                val grayMat = grayMatFlow.value
                val histogram = exposureHistogramEnableFlow.value
                exposureHistogramDataFlow.value =
                    if (histogram && grayMat != null) getHistogramData(grayMat) else null
            }
        }

        // 填充峰值亮度数据
        launch(Dispatchers.IO) {
            val zebraOffsetArr = arrayOf(0).toIntArray()
            combine(grayMatFlow, brightnessPeakingEnableFlow) { t01, t02 ->
                arrayOf(t01, t02)
            }.collectLatest {
                val grayMat = grayMatFlow.value
                val enable = brightnessPeakingEnableFlow.value
                brightnessPeakingMatFlow.value =
                    if (grayMat != null && enable) markOverExposedRegions(
                        zebraOffsetArr,
                        grayMat
                    ) else null
            }
        }

        // 填充峰值对焦数据
        launch(Dispatchers.IO) {
            combine(grayMatFlow, focusPeakingEnableFlow) { t01, t02 ->
                arrayOf(t01, t02)
            }.collectLatest {
                val time = testTime {
                    val grayMat = grayMatFlow.value
                    val enable = focusPeakingEnableFlow.value
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
    }

    fun processImage(image: Image) {
        frameCount++
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

}