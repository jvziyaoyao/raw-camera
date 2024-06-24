package com.jvziyaoyao.raw.camera.domain.repository

import android.content.ContentResolver
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import com.jvziyaoyao.camera.raw.util.ContextUtil
import com.jvziyaoyao.raw.camera.domain.model.MediaQueryEntity
import java.io.File

class ImageRepo {

    val imageList = mutableStateListOf<MediaQueryEntity>()

    val storagePath by lazy {
        val picturesFile =
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).absoluteFile
        val storageFile = File(picturesFile, "yao")
        if (!storageFile.exists()) storageFile.mkdirs()
        storageFile
    }

    fun deleteImage(mediaQueryEntity: MediaQueryEntity) {
        imageList.remove(mediaQueryEntity)
    }

    fun fetchImages() {
        queryImages(storagePath.absolutePath).apply {
            imageList.clear()
            imageList.addAll(this)
        }
    }

    private fun queryImages(
        folderPath: String
    ): List<MediaQueryEntity> {
        val context = ContextUtil.getApplicationByReflect()
        val contentResolver: ContentResolver = context.contentResolver
        val selection = "${MediaStore.Files.FileColumns.DATA} LIKE ?"
        val selectionArgs = arrayOf("$folderPath%")
        val query = contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            null,
            selection,
            selectionArgs,
            MediaStore.MediaColumns.DATE_ADDED + " DESC"
        ) ?: return emptyList()
        val columnNames = query.columnNames
        val list = ArrayList<MediaQueryEntity>()
        while (query.moveToNext()) {
            val mediaQueryEntity = MediaQueryEntity()
            for (name in columnNames) {
                val columnIndex = query.getColumnIndex(name)
                when (name) {
                    MediaStore.Images.Media.DISPLAY_NAME -> {
                        mediaQueryEntity.name = query.getString(columnIndex)
                    }

                    MediaStore.Images.Media.MIME_TYPE -> {
                        mediaQueryEntity.mimeType = query.getString(columnIndex)
                    }

                    MediaStore.Images.Media.WIDTH -> {
                        mediaQueryEntity.width = query.getLong(columnIndex)
                    }

                    MediaStore.Images.Media.HEIGHT -> {
                        mediaQueryEntity.height = query.getLong(columnIndex)
                    }

                    MediaStore.Images.Media.DATA -> {
                        mediaQueryEntity.path = query.getString(columnIndex)
                    }

                    MediaStore.Images.Media.ORIENTATION -> {
                        mediaQueryEntity.orientation = query.getLong(columnIndex)
                    }

                    MediaStore.Images.Media.DURATION -> {
                        mediaQueryEntity.duration = query.getLong(columnIndex)
                    }

                    MediaStore.Images.Media.SIZE -> {
                        mediaQueryEntity.size = query.getLong(columnIndex)
                    }

                    MediaStore.Images.Media.DATE_ADDED -> {
                        mediaQueryEntity.dateAdded = query.getLong(columnIndex)
                    }

                    MediaStore.Images.Media.DATE_MODIFIED -> {
                        mediaQueryEntity.dateModified = query.getLong(columnIndex)
                    }

                    MediaStore.Images.Media._ID -> {
                        mediaQueryEntity.id = query.getLong(columnIndex)
                    }
                }
            }
            list.add(mediaQueryEntity)
        }
        query.close()
        return list
    }

}