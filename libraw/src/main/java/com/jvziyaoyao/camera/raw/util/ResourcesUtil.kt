package com.jvziyaoyao.camera.raw.util

import android.content.Context
import android.content.res.Resources.NotFoundException
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader

fun readResourceAsString(context: Context, resourceId: Int): String {
    val builder = StringBuilder()
    try {
        val inputStream = context.resources.openRawResource(resourceId)
        val streamReader = InputStreamReader(inputStream)
        val bufferedReader = BufferedReader(streamReader)
        var textLine: String?
        while (bufferedReader.readLine().also { textLine = it } != null) {
            builder.append(textLine)
            builder.append("\n")
        }
    } catch (e: IOException) {
        e.printStackTrace()
    } catch (e: NotFoundException) {
        e.printStackTrace()
    }
    return builder.toString()
}
