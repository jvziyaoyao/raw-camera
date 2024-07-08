package com.jvziyaoyao.camera.flow.holder.camera.filter

import android.opengl.GLES30
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import com.jvziyaoyao.camera.flow.R
import com.jvziyaoyao.camera.flow.holder.camera.render.TEX_VERTEX_MAT_0
import com.jvziyaoyao.camera.flow.holder.camera.render.imageFilterReplacement
import com.jvziyaoyao.camera.flow.util.ContextUtil
import com.jvziyaoyao.camera.flow.util.compileShader
import com.jvziyaoyao.camera.flow.util.fillMatTexture
import com.jvziyaoyao.camera.flow.util.linkProgram
import com.jvziyaoyao.camera.flow.util.loadEmptyMatTexture
import com.jvziyaoyao.camera.flow.util.readResourceAsString
import org.opencv.core.Mat
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.properties.Delegates

data class ImageFilter(
    val name: String,
    val shaderStr: String,
)

val context by lazy { ContextUtil.getApplicationByReflect() }

val defaultImageFilterList by lazy {
    listOf(
        ImageFilter(
            name = "无",
            shaderStr = imageFilterReplacement,
        ),
        ImageFilter(
            name = "蓝调",
            shaderStr = readResourceAsString(context, R.raw.image_filter_blue),
        ),
        ImageFilter(
            name = "对比",
            shaderStr = readResourceAsString(context, R.raw.image_filter_contrast),
        ),
        ImageFilter(
            name = "暖色",
            shaderStr = readResourceAsString(context, R.raw.image_filter_warm),
        ),
        ImageFilter(
            name = "冷色",
            shaderStr = readResourceAsString(context, R.raw.image_filter_cold),
        ),
        ImageFilter(
            name = "朦胧",
            shaderStr = readResourceAsString(context, R.raw.image_filter_glass),
        ),
        ImageFilter(
            name = "复古",
            shaderStr = readResourceAsString(context, R.raw.image_filter_old),
        ),
        ImageFilter(
            name = "灰度",
            shaderStr = readResourceAsString(context, R.raw.image_filter_gray),
        ),
        ImageFilter(
            name = "黑白",
            shaderStr = readResourceAsString(context, R.raw.image_filter_black),
        ),
    )
}

class ImageFilterRenderer(
    private var textureVertex: FloatArray = TEX_VERTEX_MAT_0,
    private var imageFilterStr: String = imageFilterReplacement,
) : GLSurfaceView.Renderer {

    /**
     * 顶点坐标
     * (x,y,z)
     */
    private val POSITION_VERTEX = floatArrayOf(
        0f, 0f, 0f,     //顶点坐标V0
        1f, 1f, 0f,     //顶点坐标V1
        -1f, 1f, 0f,    //顶点坐标V2
        -1f, -1f, 0f,   //顶点坐标V3
        1f, -1f, 0f     //顶点坐标V4
    )

    /**
     * 绘制顺序索引
     */
    private val VERTEX_INDEX = shortArrayOf(
        0, 1, 2,  //V0,V1,V2 三个顶点组成一个三角形
        0, 2, 3,  //V0,V2,V3 三个顶点组成一个三角形
        0, 3, 4,  //V0,V3,V4 三个顶点组成一个三角形
        0, 4, 1   //V0,V4,V1 三个顶点组成一个三角形
    )

    // 变换矩阵
    private var uMatrixLocation by Delegates.notNull<Int>()

    // 位置
    private var aPositionLocation by Delegates.notNull<Int>()

    // 纹理
    private var aTextureLocation by Delegates.notNull<Int>()

    private var additionalSamplerLocation by Delegates.notNull<Int>()

    private var additionalTextureId by Delegates.notNull<Int>()

    private var viewPortWidth = 0

    private var viewPortHeight = 0

    // 着色器id
    private var programId by Delegates.notNull<Int>()

    var currentAdditionalMat: Mat? = null

    private val vertexBuffer: FloatBuffer =
        ByteBuffer.allocateDirect(POSITION_VERTEX.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .apply {
                put(POSITION_VERTEX)
                position(0)
            }

    private val vertexIndexBuffer: ShortBuffer =
        ByteBuffer.allocateDirect(VERTEX_INDEX.size * 2)
            .order(ByteOrder.nativeOrder())
            .asShortBuffer()
            .apply {
                put(VERTEX_INDEX)
                position(0)
            }

    private var textureBuffer: FloatBuffer = getNewTextureBuffer(textureVertex)

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {

        GLES30.glEnable(GLES30.GL_BLEND)
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA)

        // 将背景设置为白色
//        GLES30.glClearColor(1.0f, 1.0f, 1.0f, 1.0f)
        // 将背景设置为透明
        GLES30.glClearColor(0.0f, 0.0f, 0.0f, 0.0f)

        // 创建着色器
        createShaderProgram(imageFilterStr)

        // 加载纹理
        additionalTextureId = loadEmptyMatTexture()
    }

    private fun createShaderProgram(
        imageFilterStr: String = imageFilterReplacement,
    ) {
        val context = ContextUtil.getApplicationByReflect()
        //编译顶点着色程序
        val vertexShaderStr = readResourceAsString(context, R.raw.block_mat_vertex_shader)
        val vertexShaderId = compileShader(GLES30.GL_VERTEX_SHADER, vertexShaderStr)
        //编译片段着色程序
        var fragmentShaderStr = readResourceAsString(context, R.raw.block_mat_fragment_shader)
        fragmentShaderStr = fragmentShaderStr.replace(imageFilterReplacement, imageFilterStr)
        val fragmentShaderId = compileShader(GLES30.GL_FRAGMENT_SHADER, fragmentShaderStr)
        //连接程序
        programId = linkProgram(vertexShaderId, fragmentShaderId)
        //在OpenGLES环境中使用程序
        GLES30.glUseProgram(programId)
        uMatrixLocation = GLES30.glGetUniformLocation(programId, "uTextureMatrix")
        aPositionLocation = GLES30.glGetAttribLocation(programId, "vPosition")
        aTextureLocation = GLES30.glGetAttribLocation(programId, "aTextureCoord")
        //获取Shader中定义的变量在program中的位置
        additionalSamplerLocation = GLES30.glGetUniformLocation(programId, "uTextureUnit")
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        //设置绘制窗口
        GLES30.glViewport(0, 0, width, height)
        viewPortWidth = width
        viewPortHeight = height
    }

    override fun onDrawFrame(gl: GL10?) {
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT)
        drawAdditionalLayer()
    }

    fun updateTextureBuffer(nextTextureVertex: FloatArray) {
        textureVertex = nextTextureVertex
        val oldTextureBuffer = textureBuffer
        textureBuffer = getNewTextureBuffer(nextTextureVertex)
        oldTextureBuffer.clear()
    }

    private fun getNewTextureBuffer(nextTextureVertex: FloatArray): FloatBuffer {
        return ByteBuffer.allocateDirect(nextTextureVertex.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .apply {
                put(nextTextureVertex)
                position(0)
            }
    }

    private fun getBitmapMVPMatrix(
        bitmapWidth: Int,
        bitmapHeight: Int,
        screenWidth: Int,
        screenHeight: Int
    ): FloatArray {
        val resultMatrix = FloatArray(16)
        val viewMatrix = FloatArray(16)
        val projectionMatrix = FloatArray(16)

        // 计算图片和屏幕的宽高比
        val bitmapAspectRatio = bitmapWidth.toFloat() / bitmapHeight
        val screenAspectRatio = screenWidth.toFloat() / screenHeight

        // 根据宽高比调整正交投影矩阵，使图片尽可能填满屏幕
        if (bitmapAspectRatio > screenAspectRatio) {
            val scale = screenAspectRatio / bitmapAspectRatio
            Matrix.orthoM(projectionMatrix, 0, -1f, 1f, -scale, scale, 3f, 7f)
        } else {
            val scale = bitmapAspectRatio / screenAspectRatio
            Matrix.orthoM(projectionMatrix, 0, -scale, scale, -1f, 1f, 3f, 7f)
        }

        // 设置相机位置
        Matrix.setLookAtM(viewMatrix, 0, 0f, 0f, 7.0f, 0f, 0f, 0f, 0f, 1.0f, 0.0f)

        // 计算变换矩阵
        Matrix.multiplyMM(resultMatrix, 0, projectionMatrix, 0, viewMatrix, 0)

        return resultMatrix
    }

    private fun drawAdditionalLayer() {
        currentAdditionalMat?.apply {
            // 这个控制是否开启
            GLES30.glActiveTexture(GLES30.GL_TEXTURE3)
            GLES30.glUniform1i(additionalSamplerLocation, 3)
            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, additionalTextureId)
            fillMatTexture(this)
            val matrix = getBitmapMVPMatrix(width(), height(), viewPortWidth, viewPortHeight)
            draw2DTexture(matrix)
        }
    }

    private fun draw2DTexture(matrix: FloatArray) {
        //将变换矩阵传入顶点渲染器
        GLES30.glUniformMatrix4fv(uMatrixLocation, 1, false, matrix, 0)
        //启用顶点坐标属性
        GLES30.glEnableVertexAttribArray(aPositionLocation)
        GLES30.glVertexAttribPointer(
            aPositionLocation,
            3,
            GLES30.GL_FLOAT,
            false,
            0,
            vertexBuffer
        )
        //启用纹理坐标属性
        GLES30.glEnableVertexAttribArray(aTextureLocation)
        GLES30.glVertexAttribPointer(
            aTextureLocation,
            2,
            GLES30.GL_FLOAT,
            false,
            0,
            textureBuffer
        );
        // 绘制
        GLES30.glDrawElements(
            GLES30.GL_TRIANGLES,
            VERTEX_INDEX.size,
            GLES30.GL_UNSIGNED_SHORT,
            vertexIndexBuffer
        )
        //禁止顶点数组的句柄
        GLES30.glDisableVertexAttribArray(aPositionLocation)
        GLES30.glDisableVertexAttribArray(aTextureLocation)
    }
}