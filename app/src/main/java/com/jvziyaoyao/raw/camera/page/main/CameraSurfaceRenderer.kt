package com.jvziyaoyao.raw.camera.page.main

import android.opengl.GLES10
import android.opengl.GLES30
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import android.util.Log
import com.jvziyaoyao.raw.camera.R
import com.jvziyaoyao.raw.camera.util.ContextUtil
import com.jvziyaoyao.raw.camera.util.compileShader
import com.jvziyaoyao.raw.camera.util.linkProgram
import com.jvziyaoyao.raw.camera.util.readResourceAsString
import org.opencv.core.Mat
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.properties.Delegates


/**
 * @program: TestFocusable
 *
 * @description:
 *
 * @author: JVZIYAOYAO
 *
 * @create: 2024-02-24 23:09
 **/

val TEX_VERTEX_MAT_BACK_0 = floatArrayOf(
    0.5f, 0.5f, //纹理坐标V0
    1f, 0f,     //纹理坐标V4
    0f, 0f,     //纹理坐标V1
    0f, 1.0f,   //纹理坐标V2
    1f, 1.0f,    //纹理坐标V3
)

val TEX_VERTEX_MAT_BACK_90 = floatArrayOf(
    0.5f, 0.5f, //纹理坐标V0
    0f, 0f,     //纹理坐标V1
    0f, 1.0f,   //纹理坐标V2
    1f, 1.0f,    //纹理坐标V3
    1f, 0f,     //纹理坐标V4
)

val TEX_VERTEX_MAT_BACK_180 = floatArrayOf(
    0.5f, 0.5f, //纹理坐标V0
    0f, 1.0f,   //纹理坐标V2
    1f, 1.0f,    //纹理坐标V3
    1f, 0f,     //纹理坐标V4
    0f, 0f,     //纹理坐标V1
)

val TEX_VERTEX_MAT_BACK_270 = floatArrayOf(
    0.5f, 0.5f, //纹理坐标V0
    1f, 1.0f,    //纹理坐标V3
    1f, 0f,     //纹理坐标V4
    0f, 0f,     //纹理坐标V1
    0f, 1.0f,   //纹理坐标V2
)

val TEX_VERTEX_MAT_FRONT_0 = floatArrayOf(
    0.5f, 0.5f, //纹理坐标V0
    0f, 0f,     //纹理坐标V4
    1f, 0f,     //纹理坐标V1
    1f, 1.0f,    //纹理坐标V2
    0f, 1.0f,   //纹理坐标V3
)

val TEX_VERTEX_MAT_FRONT_90 = floatArrayOf(
    0.5f, 0.5f, //纹理坐标V0
    1f, 0f,     //纹理坐标V1
    1f, 1.0f,    //纹理坐标V2
    0f, 1.0f,   //纹理坐标V3
    0f, 0f,     //纹理坐标V4
)

val TEX_VERTEX_MAT_FRONT_180 = floatArrayOf(
    0.5f, 0.5f, //纹理坐标V0
    1f, 1.0f,    //纹理坐标V2
    0f, 1.0f,   //纹理坐标V3
    0f, 0f,     //纹理坐标V4
    1f, 0f,     //纹理坐标V1
)

val TEX_VERTEX_MAT_FRONT_270 = floatArrayOf(
    0.5f, 0.5f, //纹理坐标V0
    0f, 1.0f,   //纹理坐标V3
    0f, 0f,     //纹理坐标V4
    1f, 0f,     //纹理坐标V1
    1f, 1.0f,    //纹理坐标V2
)

data class YUVRenderData(
    val width: Int,
    val height: Int,
    val yByteArray: ByteBuffer,
    val uByteArray: ByteBuffer,
    val vByteArray: ByteBuffer,
)

class CameraSurfaceRenderer(
    private var textureVertex: FloatArray,
) : GLSurfaceView.Renderer {

    private val TAG = CameraSurfaceRenderer::class.java.name

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
    private var ySamplerLocation by Delegates.notNull<Int>()
    private var uSamplerLocation by Delegates.notNull<Int>()
    private var vSamplerLocation by Delegates.notNull<Int>()
    private var useAdditionalTexture by Delegates.notNull<Int>()

    private var additionalTextureId by Delegates.notNull<Int>()
    private var yTextureId by Delegates.notNull<Int>()
    private var uTextureId by Delegates.notNull<Int>()
    private var vTextureId by Delegates.notNull<Int>()

    private val resultMatrix = getBitmapMVPMatrix()

    // 着色器id
    private var programId by Delegates.notNull<Int>()

    var currentYuvData: YUVRenderData? = null

    var currentAdditionalMat: Mat? = null

    var frameCount = 0

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
        val extensions = GLES10.glGetString(GL10.GL_EXTENSIONS)
        val isLuminanceAlphaSupported =
            extensions.contains("GL_EXT_texture_format_BGRA8888") || extensions.contains("GL_APPLE_texture_format_BGRA8888")
        Log.i(TAG, "onSurfaceCreated: isLuminanceAlphaSupported $isLuminanceAlphaSupported")

        val context = ContextUtil.getApplicationByReflect()

        GLES30.glEnable(GLES30.GL_BLEND)
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA)

        //将背景设置为白色
        GLES30.glClearColor(1.0f, 1.0f, 1.0f, 1.0f)
        //编译顶点着色程序
        val vertexShaderStr = readResourceAsString(context, R.raw.camera_mat_vertex_shader)
        val vertexShaderId = compileShader(GLES30.GL_VERTEX_SHADER, vertexShaderStr)
        //编译片段着色程序
        val fragmentShaderStr = readResourceAsString(context, R.raw.camera_mat_fragment_shader)
        val fragmentShaderId = compileShader(GLES30.GL_FRAGMENT_SHADER, fragmentShaderStr)
        //连接程序
        programId = linkProgram(vertexShaderId, fragmentShaderId)
        //在OpenGLES环境中使用程序
        GLES30.glUseProgram(programId)
        uMatrixLocation = GLES30.glGetUniformLocation(programId, "uTextureMatrix")
        aPositionLocation = GLES30.glGetAttribLocation(programId, "vPosition")
        aTextureLocation = GLES30.glGetAttribLocation(programId, "aTextureCoord")
        //获取Shader中定义的变量在program中的位置
        additionalSamplerLocation = GLES30.glGetUniformLocation(programId, "additionalTexture")
        ySamplerLocation = GLES30.glGetUniformLocation(programId, "yTexture")
        uSamplerLocation = GLES30.glGetUniformLocation(programId, "uTexture")
        vSamplerLocation = GLES30.glGetUniformLocation(programId, "vTexture")
        useAdditionalTexture = GLES30.glGetUniformLocation(programId, "useAdditionalTexture")
        // 加载纹理
        yTextureId = loadEmptyYuvTexture()
        uTextureId = loadEmptyYuvTexture()
        vTextureId = loadEmptyYuvTexture()
        additionalTextureId = loadEmptyMatTexture()
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        //设置绘制窗口
        GLES30.glViewport(0, 0, width, height)
    }

    override fun onDrawFrame(gl: GL10?) {
        frameCount++
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT)
        currentYuvData?.apply {
            drawCameraLayer(this)
            GLES30.glUniform1i(useAdditionalTexture, 0)
            drawAdditionalLayer(this)
        }
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

    private fun loadEmptyMatTexture(): Int {
        val textureIds = IntArray(1)
        //创建一个纹理对象
        GLES30.glGenTextures(1, textureIds, 0)
        if (textureIds[0] == 0) return 0
        //绑定纹理到OpenGL
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textureIds[0])
        //设置默认的纹理过滤参数
        GLES30.glTexParameteri(
            GLES30.GL_TEXTURE_2D,
            GLES30.GL_TEXTURE_MIN_FILTER,
            GLES30.GL_NEAREST
        )
        GLES30.glTexParameteri(
            GLES30.GL_TEXTURE_2D,
            GLES30.GL_TEXTURE_MAG_FILTER,
            GLES30.GL_NEAREST
        )
        GLES30.glTexParameteri(
            GLES30.GL_TEXTURE_2D,
            GLES30.GL_TEXTURE_WRAP_S,
            GLES30.GL_CLAMP_TO_EDGE
        )
        GLES30.glTexParameteri(
            GLES30.GL_TEXTURE_2D,
            GLES30.GL_TEXTURE_WRAP_T,
            GLES30.GL_CLAMP_TO_EDGE
        )
        //取消绑定纹理
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, 0)
        return textureIds[0]
    }

    private fun loadEmptyYuvTexture(): Int {
        val textureIds = IntArray(1)
        //创建一个纹理对象
        GLES30.glGenTextures(1, textureIds, 0)
        if (textureIds[0] == 0) return 0
        //绑定纹理到OpenGL
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textureIds[0])
        //设置默认的纹理过滤参数
        //环绕（超出纹理坐标范围）  （s==x t==y GL_REPEAT 重复）
        GLES30.glTexParameteri(
            GLES30.GL_TEXTURE_2D,
            GLES30.GL_TEXTURE_WRAP_S,
            GLES30.GL_REPEAT
        )
        GLES30.glTexParameteri(
            GLES30.GL_TEXTURE_2D,
            GLES30.GL_TEXTURE_WRAP_T,
            GLES30.GL_REPEAT
        )
        //过滤（纹理像素映射到坐标点）  （缩小、放大：GL_LINEAR线性）
        GLES30.glTexParameteri(
            GLES30.GL_TEXTURE_2D,
            GLES30.GL_TEXTURE_MIN_FILTER,
            GLES30.GL_LINEAR
        )
        GLES30.glTexParameteri(
            GLES30.GL_TEXTURE_2D,
            GLES30.GL_TEXTURE_MAG_FILTER,
            GLES30.GL_LINEAR
        )
        //取消绑定纹理
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, 0)
        return textureIds[0]
    }

    private fun fillMatTexture(mat: Mat) {
        val matData = ByteArray(mat.width() * mat.height() * 4)
        mat.get(0, 0, matData)
        GLES30.glTexImage2D(
            GLES30.GL_TEXTURE_2D,
            0,
            GLES30.GL_RGBA,
            mat.cols(),
            mat.rows(),
            0,
            GLES30.GL_RGBA,
            GLES30.GL_UNSIGNED_BYTE,
            ByteBuffer.wrap(matData),
        )
        GLES30.glGenerateMipmap(GLES30.GL_TEXTURE_2D)
    }

    private fun getBitmapMVPMatrix(): FloatArray {
        val resultMatrix = FloatArray(16)
        val viewMatrix = FloatArray(16)
        val projectionMatrix = FloatArray(16)
        Matrix.orthoM(
            projectionMatrix, 0, -1F, 1F, -1F, 1F, 3F, 7F
        )
        //设置相机位置
        Matrix.setLookAtM(viewMatrix, 0, 0F, 0F, 7.0f, 0f, 0f, 0f, 0f, 1.0f, 0.0f)
        //计算变换矩阵
        Matrix.multiplyMM(resultMatrix, 0, projectionMatrix, 0, viewMatrix, 0)
        return resultMatrix
    }

    private fun drawCameraLayer(yuvRenderData: YUVRenderData) {
        yuvRenderData.apply {
            GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
            GLES30.glUniform1i(ySamplerLocation, 0)
            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, yTextureId)
            GLES30.glTexImage2D(
                GLES30.GL_TEXTURE_2D,
                0,
                GLES30.GL_LUMINANCE,
                width,
                height,
                0,
                GLES30.GL_LUMINANCE,
                GLES30.GL_UNSIGNED_BYTE,
                yByteArray
            )
            draw2DTexture()

            GLES30.glActiveTexture(GLES30.GL_TEXTURE1)
            GLES30.glUniform1i(uSamplerLocation, 1)
            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, uTextureId)
            GLES30.glTexImage2D(
                GLES30.GL_TEXTURE_2D,
                0,
                GLES30.GL_LUMINANCE_ALPHA,
                width / 2,
                height / 2,
                0,
                GLES30.GL_LUMINANCE_ALPHA,
                GLES30.GL_UNSIGNED_BYTE,
                uByteArray
            )
            draw2DTexture()

            GLES30.glActiveTexture(GLES30.GL_TEXTURE2)
            GLES30.glUniform1i(vSamplerLocation, 2)
            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, vTextureId)
            GLES30.glTexImage2D(
                GLES30.GL_TEXTURE_2D,
                0,
                GLES30.GL_LUMINANCE_ALPHA,
                width / 2,
                height / 2,
                0,
                GLES30.GL_LUMINANCE_ALPHA,
                GLES30.GL_UNSIGNED_BYTE,
                vByteArray
            )
            draw2DTexture()
        }
    }

    private fun drawAdditionalLayer(yuvRenderData: YUVRenderData) {
        currentAdditionalMat?.apply {
            // 这个控制是否开启
            GLES30.glUniform1i(useAdditionalTexture, 1)
            GLES30.glActiveTexture(GLES30.GL_TEXTURE3)
            GLES30.glUniform1i(additionalSamplerLocation, 3)
            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, additionalTextureId)
            fillMatTexture(this)
            draw2DTexture()
        }
    }

    private fun draw2DTexture() {
        //将变换矩阵传入顶点渲染器
        GLES30.glUniformMatrix4fv(uMatrixLocation, 1, false, resultMatrix, 0)
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