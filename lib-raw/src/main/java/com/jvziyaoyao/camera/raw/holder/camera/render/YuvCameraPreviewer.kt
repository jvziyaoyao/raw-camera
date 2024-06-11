package com.jvziyaoyao.camera.raw.holder.camera.render

import android.graphics.ImageFormat
import android.hardware.camera2.CameraCharacteristics
import android.media.Image
import android.media.ImageReader
import android.os.Handler
import android.view.Surface
import com.jvziyaoyao.camera.raw.holder.camera.findBestSize
import com.jvziyaoyao.camera.raw.holder.camera.getSizeByAspectRatio
import com.jvziyaoyao.camera.raw.holder.camera.scaleStreamMap
import com.jvziyaoyao.camera.raw.holder.camera.sensorAspectRatio

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
        cameraCharacteristics: CameraCharacteristics,
        handler: Handler
    ): Surface? {
        val scaleStreamConfigurationMap = cameraCharacteristics.scaleStreamMap
        val sensorAspectRatio = cameraCharacteristics.sensorAspectRatio
        val outputSizeList = scaleStreamConfigurationMap?.getOutputSizes(ImageFormat.YUV_420_888)
        val bestPreviewSize = outputSizeList?.run {
            val aspectList = getSizeByAspectRatio(this, sensorAspectRatio)
            return@run findBestSize(aspectList.toTypedArray(), 1280)
        }
        return if (bestPreviewSize != null) {
            imagePreviewReader?.close()
            imagePreviewReader = ImageReader.newInstance(
                bestPreviewSize.width,
                bestPreviewSize.height,
                ImageFormat.YUV_420_888,
                2
            )
            imagePreviewReader!!.setOnImageAvailableListener(previewImageAvailableListener, handler)
            imagePreviewReader!!.surface
        } else null
    }


}