package com.jvziyaoyao.raw.sample.page.hdr

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.Environment
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import com.jvziyaoyao.camera.flow.holder.camera.toMat
import com.jvziyaoyao.raw.sample.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.opencv.android.OpenCVLoader
import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.Scalar
import org.opencv.imgcodecs.Imgcodecs
import org.opencv.photo.Photo
import java.io.File

class HdrActivity : ComponentActivity(), CoroutineScope by MainScope() {

    private val bitmap = mutableStateOf<Bitmap?>(null)

    private fun getStoragePath(): File {
        val picturesFile =
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).absoluteFile
        val storageFile = File(picturesFile, "yao")
        if (!storageFile.exists()) storageFile.mkdirs()
        return storageFile
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            HdrBody(
                bitmap = bitmap.value,
            )
        }

        launch(Dispatchers.IO) {
            // 初始化OpenCV
            OpenCVLoader.initLocal()

            val bitmap01 = BitmapFactory.decodeResource(resources, R.drawable.hdr_01)
            val bitmap02 = BitmapFactory.decodeResource(resources, R.drawable.hdr_02)
            val mat01 = bitmap01.toMat()
            val mat02 = bitmap02.toMat()

            val images: List<Mat> = listOf(mat01, mat02)
            val times: List<Float> = listOf(0.066F, 0.005F)

            val response = Mat()
            val calibrate = Photo.createCalibrateDebevec()
            val matTimes = Mat(times.size, 1, CvType.CV_32F)
            val arrayTimes = FloatArray((matTimes.total() * matTimes.channels()).toInt())
            for (i in times.indices) {
                arrayTimes[i] = times.get(i)
            }
            matTimes.put(0, 0, arrayTimes)
            calibrate.process(images, response, matTimes)

            val hdr = Mat()
            val mergeDebevec = Photo.createMergeDebevec()
            mergeDebevec.process(images, hdr, matTimes)

            val ldr = Mat()
            val tonemap = Photo.createTonemap(2.2f)
            tonemap.process(hdr, ldr)

            val fusion = Mat()
            val mergeMertens = Photo.createMergeMertens()
            mergeMertens.process(images, fusion)

            Core.multiply(fusion, Scalar(255.0, 255.0, 255.0), fusion)
            Core.multiply(ldr, Scalar(255.0, 255.0, 255.0), ldr)


            Imgcodecs.imwrite("fusion.png", fusion)
            Imgcodecs.imwrite("ldr.png", ldr)
            Imgcodecs.imwrite("hdr.hdr", hdr)

//            bitmap.value = fusion.toBitmap()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cancel()
    }

}

@Composable
fun HdrBody(bitmap: Bitmap?) {
    if (bitmap != null) {
        Image(
            modifier = Modifier.fillMaxWidth(),
            bitmap = bitmap.asImageBitmap(), contentDescription = null
        )
    }
}