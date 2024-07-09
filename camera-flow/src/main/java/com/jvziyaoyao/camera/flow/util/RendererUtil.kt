package com.jvziyaoyao.camera.flow.util

import android.opengl.GLES30
import org.opencv.core.Mat
import java.nio.ByteBuffer

fun loadEmptyMatTexture(): Int {
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

fun fillMatTexture(mat: Mat) {
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