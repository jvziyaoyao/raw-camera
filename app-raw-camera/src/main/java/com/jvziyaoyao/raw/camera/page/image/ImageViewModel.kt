package com.jvziyaoyao.raw.camera.page.image

import androidx.lifecycle.ViewModel
import com.jvziyaoyao.raw.camera.domain.model.AlbumEntity
import com.jvziyaoyao.raw.camera.domain.model.CameraPhotoEntity
import com.jvziyaoyao.raw.camera.domain.model.MediaQueryEntity
import com.jvziyaoyao.raw.camera.domain.repository.ImageRepo
import java.util.Calendar
import java.util.Date
import java.util.HashMap
import java.util.stream.Collectors

class ImageViewModel(
    private val imageRepo: ImageRepo,
) : ViewModel() {

    val imageList
        get() = imageRepo.imageList

    fun fetchImages() = imageRepo.fetchImages()

}

/**
 * 相机相册列表平铺
 */
fun filterCameraPhotoList(cameraList: List<MediaQueryEntity>): List<CameraPhotoEntity> {
    val dayMap = HashMap<Long, MutableList<MediaQueryEntity>>()
    var minTime = System.currentTimeMillis()
    cameraList.forEach { mediaQueryEntity ->
        val time = mediaQueryEntity.timeStamp ?: return@forEach
        minTime = time.coerceAtMost(minTime)
    }
    cameraList.forEach { mediaQueryEntity ->
        val time = mediaQueryEntity.timeStamp ?: minTime
        val date = Date(time)
        val calendar = Calendar.getInstance()
        calendar.time = date
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val timeStamp = calendar.time.time
        var dayList = dayMap[timeStamp]
        if (dayList == null) {
            dayList = mutableListOf()
            dayMap[timeStamp] = dayList
        }
        dayList.add(mediaQueryEntity)
    }
    return dayMap.entries
        .stream().map {
            CameraPhotoEntity(
                timeStamp = it.key,
            ).apply {
                list.clear()
                list.addAll(it.value)
            }
        }.sorted { e1, e2 -> e2.timeStamp.compareTo(e1.timeStamp) }
        .collect(Collectors.toList())
}