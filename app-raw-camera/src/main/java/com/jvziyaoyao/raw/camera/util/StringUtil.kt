package com.jvziyaoyao.raw.camera.util

import android.annotation.SuppressLint
import java.text.SimpleDateFormat
import java.util.Date
import java.util.concurrent.TimeUnit

/**
 * @program: WePrompter
 *
 * @description:
 *
 * @author: JVZIYAOYAO
 *
 * @create: 2023-07-10 16:52
 **/
object StringUtil {

    fun formatTime(duration: Long): String {
        val hours = TimeUnit.MILLISECONDS.toHours(duration)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(duration) % 60
        val seconds = TimeUnit.MILLISECONDS.toSeconds(duration) % 60
        var res = String.format("%02d:%02d", minutes, seconds)
        if (hours == 0L) return res
        res = String.format("%02d:$res", hours)
        return res
    }

    @SuppressLint("SimpleDateFormat")
    fun formatDate(date: Date, pattern: String = "yyyy-MM-dd HH:mm:ss"): String {
        val simpleDateFormat = SimpleDateFormat(pattern)
        return simpleDateFormat.format(date)
    }

}

fun Float.formatToDecimalPlaces(places: Int): String {
    return String.format("%.${places}f", this)
}