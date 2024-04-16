package com.jvziyaoyao.camera.raw.holder.camera.off

import android.content.Context
import android.graphics.Bitmap
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

fun getGLFilterBitmap(context: Context, bitmap: Bitmap, callback: (Bitmap) -> Unit) {
    val eglSurface = OffScreenEGLSurface(context)
    val render = OffScreenRender(context, bitmap) { callback(it) }
    eglSurface.init(render)
    eglSurface.requestRender()
}

suspend fun getGLFilterBitmapAsync(context: Context, bitmap: Bitmap) =
    suspendCoroutine<Bitmap> { c ->
        val eglSurface = OffScreenEGLSurface(context)
        val render = OffScreenRender(context, bitmap) { c.resume(it) }
        eglSurface.init(render)
        eglSurface.requestRender()
    }