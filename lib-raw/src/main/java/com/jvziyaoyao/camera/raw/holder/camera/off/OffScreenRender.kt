package com.jvziyaoyao.camera.raw.holder.camera.off

import android.content.Context
import android.graphics.Bitmap
import android.opengl.GLES30

interface Renderer {
    fun onSurfaceCreated()
    fun onSurfaceChanged(width: Int, height: Int)
    fun onDrawFrame()
}

class OffScreenRender(
    context: Context,
    bitmap: Bitmap,
    onBitmap: (Bitmap) -> Unit,
) : Renderer {
    private val mOffScreenImageModel: OffScreenImageModel

    init {
        mOffScreenImageModel = OffScreenImageModel(context, bitmap, onBitmap)
    }

    override fun onSurfaceCreated() {
        //设置背景颜色
        GLES30.glClearColor(0.0f, 0.0f, 0.0f, 0.0f)
        //启动深度测试
        GLES30.glEnable(GLES30.GL_DEPTH_TEST)
        //创建程序id
        mOffScreenImageModel.onModelCreate()
    }

    override fun onSurfaceChanged(width: Int, height: Int) {
        mOffScreenImageModel.onModelChange(width, height)
    }

    override fun onDrawFrame() {
        GLES30.glClearColor(0.5f, 0.7f, 0.3f, 1.0f)
        // 将颜色缓存区设置为预设的颜色
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT or GLES30.GL_DEPTH_BUFFER_BIT)
        // 启用顶点的数组句柄
        GLES30.glEnableVertexAttribArray(0)
        GLES30.glEnableVertexAttribArray(1)
        // 绘制模型
        mOffScreenImageModel.onModelDraw()
        // 禁止顶点数组句柄
        GLES30.glDisableVertexAttribArray(0)
        GLES30.glDisableVertexAttribArray(1)
    }

}