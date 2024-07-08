package com.jvziyaoyao.camera.flow.holder.camera.off

import android.content.Context
import android.graphics.Bitmap
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

fun getGLFilterBitmap(
    context: Context,
    filter: String,
    bitmap: Bitmap,
    callback: (Bitmap) -> Unit
) {
    val eglSurface = OffScreenEGLSurface(context)
    val render = OffScreenRender(context, filter, bitmap) { callback(it) }
    eglSurface.init(render)
    eglSurface.requestRender()
}

suspend fun getGLFilterBitmapAsync(context: Context, filter: String, bitmap: Bitmap) =
    suspendCoroutine<Bitmap> { c ->
        val eglSurface = OffScreenEGLSurface(context)
        val render = OffScreenRender(context, filter, bitmap) { c.resume(it) }
        eglSurface.init(render)
        eglSurface.requestRender()
    }