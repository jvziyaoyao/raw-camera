package com.jvziyaoyao.raw.camera.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.jvziyaoyao.raw.camera.domain.model.MediaQueryEntity
import java.io.File

/**
 * @program: ImageGallery
 *
 * @description:
 *
 * @author: JVZIYAOYAO
 *
 * @create: 2023-06-06 11:46
 **/

internal const val authority = "com.jvziyaoyao.raw.camera.fileprovider"

fun shareItems(
    context: Context,
    items: List<MediaQueryEntity>,
    title: String = "图片分享～",
) {
    if (items.isNotEmpty()) {
        var intent: Intent? = null
        if (items.size > 1) {

            val uris = ArrayList<Uri>()
            items.forEach { mediaQueryEntity ->
                val path = mediaQueryEntity.path
                val file = File(path!!)
                val uri = FileProvider.getUriForFile(
                    context,
                    authority,
                    file
                )
                uris.add(uri)
            }

            intent = Intent(Intent.ACTION_SEND_MULTIPLE)
            intent.type = "*/*"
            intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
        } else if (items.size == 1) {
            val mediaQueryEntity = items.first()
            val path = mediaQueryEntity.path
            val file = File(path!!)
            val uri = FileProvider.getUriForFile(
                context,
                authority,
                file
            )

            intent = Intent(Intent.ACTION_SEND)
            intent.type = file.getMimeType()
            intent.putExtra(Intent.EXTRA_STREAM, uri)
        }

        intent?.let {
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            context.startActivity(Intent.createChooser(intent, title))
        }
    }
}