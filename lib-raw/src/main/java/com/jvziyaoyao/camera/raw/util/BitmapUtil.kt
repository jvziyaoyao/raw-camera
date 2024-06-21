package com.jvziyaoyao.camera.raw.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

fun saveBitmapToFile(
    bitmap: Bitmap,
    file: File,
    format: Bitmap.CompressFormat = Bitmap.CompressFormat.JPEG,
    quality: Int = 100
) {
    var out: FileOutputStream? = null
    try {
        out = FileOutputStream(file)
        bitmap.compress(format, quality, out)
    } catch (e: IOException) {
        e.printStackTrace()
    } finally {
        try {
            out?.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
}

suspend fun saveBitmapWithExif(srcFile: File, destFile: File, modify: suspend (Bitmap) -> Bitmap) {
    try {
        val originalBitmap = BitmapFactory.decodeFile(srcFile.absolutePath)
        val exif = ExifInterface(srcFile.absolutePath)
        val modifiedBitmap = modify(originalBitmap)
        var out: FileOutputStream? = null
        try {
            out = FileOutputStream(destFile)
            modifiedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
        } finally {
            out?.close()
        }
        val newExif = ExifInterface(destFile.absolutePath)
        copyExif(exif, newExif)
        newExif.saveAttributes()

    } catch (e: IOException) {
        e.printStackTrace()
    }
}

fun copyExif(oldExif: ExifInterface, newExif: ExifInterface) {
    val fields = ExifInterface::class.java.declaredFields
    for (field in fields) {
        if (field.name.startsWith("TAG_")) {
            try {
                val tag = field.get(null) as? String
                tag?.let {
                    oldExif.getAttribute(it)?.let { value ->
                        newExif.setAttribute(it, value)
                    }
                }
            } catch (e: IllegalAccessException) {
                e.printStackTrace()
            }
        }
    }
}