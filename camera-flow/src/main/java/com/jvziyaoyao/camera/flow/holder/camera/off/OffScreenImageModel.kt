package com.jvziyaoyao.camera.flow.holder.camera.off

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Point
import android.opengl.GLES30
import android.opengl.GLUtils
import com.jvziyaoyao.camera.flow.R
import com.jvziyaoyao.camera.flow.holder.camera.render.imageFilterReplacement
import com.jvziyaoyao.camera.flow.util.compileShader
import com.jvziyaoyao.camera.flow.util.linkProgram
import com.jvziyaoyao.camera.flow.util.readResourceAsString
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

class OffScreenImageModel(
    private val context: Context,
    private val imageFilterStr: String,
    private val bitmap: Bitmap,
    private val onBitmap: (Bitmap) -> Unit,
) {
    private val mVertex =
        floatArrayOf(-1.0f, 1.0f, 0.0f, -1.0f, -1.0f, 0.0f, 1.0f, 1.0f, 0.0f, 1.0f, -1.0f, 0.0f)
    private val mFboTexture = floatArrayOf(0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 1.0f, 1.0f, 0.0f)
    private var mVertexBuffer: FloatBuffer = getFloatBuffer(mVertex)
    private var mFboTextureBuffer: FloatBuffer = getFloatBuffer(mFboTexture)

    // 帧缓冲对象 - 颜色、深度、模板附着点，纹理对象可以连接到帧缓冲区对象的颜色附着点
    private val mFrameBufferId = IntArray(1)
    private val mTextureId = IntArray(2)
    private var mProgramId = 0
    private val mBitmapSize = Point().apply { set(bitmap.width, bitmap.height) }

    // 模型创建
    fun onModelCreate() {
        //编译顶点着色程序
        val vertexShaderStr = readResourceAsString(context, R.raw.output_vertex_shader)
        val vertexShaderId = compileShader(GLES30.GL_VERTEX_SHADER, vertexShaderStr)
        //编译片段着色程序
        var fragmentShaderStr = readResourceAsString(context, R.raw.output_fragment_shader)
        fragmentShaderStr = fragmentShaderStr.replace(imageFilterReplacement, imageFilterStr)
        val fragmentShaderId = compileShader(GLES30.GL_FRAGMENT_SHADER, fragmentShaderStr)
        //连接程序
        mProgramId = linkProgram(vertexShaderId, fragmentShaderId)
        // 加载材质贴图
        loadTexture(mTextureId, mFrameBufferId)
    }

    // 模型参数变化
    fun onModelChange(width: Int, height: Int) {
        GLES30.glViewport(0, 0, mBitmapSize.x, mBitmapSize.y)
    }

    // 模型绘制
    fun onModelDraw() {
        GLES30.glUseProgram(mProgramId)
        // 准备顶点坐标和纹理坐标
        GLES30.glVertexAttribPointer(0, VERTEX_DIMENSION, GLES30.GL_FLOAT, false, 0, mVertexBuffer)
        GLES30.glVertexAttribPointer(
            1,
            TEXTURE_DIMENSION,
            GLES30.GL_FLOAT,
            false,
            0,
            mFboTextureBuffer
        )
        // 激活纹理
        GLES30.glActiveTexture(GLES30.GL_TEXTURE)
        // 绑定纹理
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, mTextureId[0])
        // 绑定缓存
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, mFrameBufferId[0])
        // 绘制贴图
        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4)
        // 显示图片
        showBitmap()
    }

    private fun showBitmap() {
        // 分配字节缓区大小， 一个像素4个字节
        val byteBuffer = ByteBuffer.allocate(mBitmapSize.x * mBitmapSize.y * Integer.BYTES)
        GLES30.glReadPixels(
            0,
            0,
            mBitmapSize.x,
            mBitmapSize.y,
            GLES30.GL_RGBA,
            GLES30.GL_UNSIGNED_BYTE,
            byteBuffer
        )
        val bitmap = Bitmap.createBitmap(mBitmapSize.x, mBitmapSize.y, Bitmap.Config.ARGB_8888)
        // 从缓存区读二进制缓冲数据
        bitmap.copyPixelsFromBuffer(byteBuffer)
        // 回调
        onBitmap(bitmap)
    }

    // 加载纹理贴图
    private fun loadTexture(
        textureId: IntArray,
        frameBufferId: IntArray
    ) {
        // 生成纹理id
        GLES30.glGenTextures(2, textureId, 0)
        for (i in 0..1) {
            // 绑定纹理到OpenGL
            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textureId[i])
            GLES30.glTexParameterf(
                GLES30.GL_TEXTURE_2D,
                GLES30.GL_TEXTURE_MIN_FILTER,
                GLES30.GL_NEAREST.toFloat()
            )
            GLES30.glTexParameterf(
                GLES30.GL_TEXTURE_2D,
                GLES30.GL_TEXTURE_MAG_FILTER,
                GLES30.GL_LINEAR.toFloat()
            )
            GLES30.glTexParameterf(
                GLES30.GL_TEXTURE_2D,
                GLES30.GL_TEXTURE_WRAP_S,
                GLES30.GL_CLAMP_TO_EDGE.toFloat()
            )
            GLES30.glTexParameterf(
                GLES30.GL_TEXTURE_2D,
                GLES30.GL_TEXTURE_WRAP_T,
                GLES30.GL_CLAMP_TO_EDGE.toFloat()
            )
            if (i == 0) {
                // 第一个纹理对象给渲染管线(加载bitmap到纹理中)
                GLUtils.texImage2D(GLES30.GL_TEXTURE_2D, 0, GLES30.GL_RGBA, bitmap, 0)
            } else {
                // 第二个纹理对象给帧缓冲区
                GLES30.glTexImage2D(
                    GLES30.GL_TEXTURE_2D, 0, GLES30.GL_RGBA, bitmap.width, bitmap.height,
                    0, GLES30.GL_RGBA, GLES30.GL_UNSIGNED_BYTE, null
                )
            }
            // 取消绑定纹理
            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, GLES30.GL_NONE)
        }
        // 创建帧缓存id
        GLES30.glGenFramebuffers(1, frameBufferId, 0)
        // 绑定帧缓存
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, frameBufferId[0])
        // 将第二个纹理附着在帧缓存的颜色附着点上
        GLES30.glFramebufferTexture2D(
            GLES30.GL_FRAMEBUFFER,
            GLES30.GL_COLOR_ATTACHMENT0,
            GLES30.GL_TEXTURE_2D,
            textureId[1],
            0
        )
        // 取消绑定帧缓存
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, GLES30.GL_NONE)
    }

    private fun getFloatBuffer(floatArr: FloatArray): FloatBuffer {
        val fb = ByteBuffer.allocateDirect(floatArr.size * java.lang.Float.BYTES)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
        fb.put(floatArr)
        fb.position(0)
        return fb
    }

    companion object {
        private const val TEXTURE_DIMENSION = 2 // 纹理坐标维度
        private const val VERTEX_DIMENSION = 3 // 顶点坐标维度
    }
}