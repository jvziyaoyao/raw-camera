package com.jvziyaoyao.camera.raw.holder.camera.render

import android.graphics.ImageFormat
import android.media.Image
import android.media.ImageReader
import android.os.Handler
import android.util.Size
import android.view.Surface

class YuvCameraPreviewer(
    val onImage: (Image) -> Unit,
) {

    private var imagePreviewReader: ImageReader? = null

    private val previewImageAvailableListener = object : ImageReader.OnImageAvailableListener {
        override fun onImageAvailable(reader: ImageReader?) {
            val image = reader?.acquireNextImage() ?: return
            onImage.invoke(image)
            image.close()
        }
    }

    fun getPreviewSurface(
        previewSize: Size,
        handler: Handler
    ): Surface? {
        imagePreviewReader?.close()
        imagePreviewReader = ImageReader.newInstance(
            previewSize.width,
            previewSize.height,
            ImageFormat.YUV_420_888,
            2
        )
        imagePreviewReader!!.setOnImageAvailableListener(previewImageAvailableListener, handler)
        return imagePreviewReader!!.surface
    }


}