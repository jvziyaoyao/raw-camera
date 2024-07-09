package com.jvziyaoyao.raw.camera.util

import java.io.File
import java.net.URLConnection

fun File.getMimeType(): String {
    val fileNameMap = URLConnection.getFileNameMap()
    return fileNameMap.getContentTypeFor(this.name)
}
