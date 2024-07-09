package com.jvziyaoyao.raw.camera.domain.model

import android.annotation.SuppressLint
import androidx.compose.runtime.mutableStateListOf
import androidx.exifinterface.media.ExifInterface
import java.io.Serializable
import java.text.SimpleDateFormat
import java.util.*

data class Exif(
    val aperture: String? = null,
    val datetime: String? = null,
    val exposure_time: String? = null,
    val flash: String? = null,
    val focal_length: String? = null,
    val image_length: String? = null,
    val image_width: String? = null,
    val iso: String? = null,
    val make: String? = null,
    val model: String? = null,
    val orientation: String? = null,
    val white_balance: String? = null,
    val latitude: String? = null,
    val latitude_ref: String? = null,
    val longitude: String? = null,
    val longitude_ref: String? = null,
): Serializable

@SuppressLint("SimpleDateFormat")
private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")

data class MediaQueryEntity(
    var id: Long? = null,
    var mimeType: String? = null,
    var width: Long? = null,
    var height: Long? = null,
    var orientation: Long? = null,
    var name: String? = null,
    var path: String? = null,
    var size: Long? = null,
    var duration: Long? = null,
    var dateAdded: Long? = null,
    var dateModified: Long? = null,
): Serializable {

    val isVideo: Boolean?
        get() = mimeType?.startsWith("video/")

    val timeStamp: Long?
        get() {
            if (dateAdded == null) return null
            return dateAdded!! * 1000
        }

    val calcSize: String?
        get() {
            if (size == null) return null
            return if (size!! < 1024 * 1024) {
                "${size!!.div(1024).toInt()}KB"
            } else if (size!! < 1024 * 1024 * 1024) {
                "${size!!.div(1024 * 1024).toInt()}MB"
            } else {
                "${size!!.div(1024 * 1024 * 1024).toInt()}GB"
            }
        }

    val purePath: String?
        get() {
            if (path.isNullOrEmpty()) return null
            if (name.isNullOrEmpty()) return null
            return path!!.replace("/$name", "")
        }

    val timeAddDate: String?
        get() {
            if (dateAdded == null) return null
            return dateFormat.format(Date(dateAdded!! * 1000))
        }

    val timeModifiedDate: String?
        get() {
            if (dateModified == null) return null
            return dateFormat.format(Date(dateModified!! * 1000))
        }

    private fun getExifInterface(): ExifInterface? {
        if (path.isNullOrEmpty()) return null
        return ExifInterface(path!!)
    }

    suspend fun getExif(): Exif? {
        val exifInterface = getExifInterface() ?: return null
        return Exif(
            aperture = exifInterface.getAttribute(ExifInterface.TAG_APERTURE_VALUE),
            datetime = exifInterface.getAttribute(ExifInterface.TAG_DATETIME),
            exposure_time = exifInterface.getAttribute(ExifInterface.TAG_EXPOSURE_TIME),
            flash = exifInterface.getAttribute(ExifInterface.TAG_FLASH),
            focal_length = exifInterface.getAttribute(ExifInterface.TAG_FOCAL_LENGTH),
            image_length = exifInterface.getAttribute(ExifInterface.TAG_IMAGE_LENGTH),
            image_width = exifInterface.getAttribute(ExifInterface.TAG_IMAGE_WIDTH),
            iso = exifInterface.getAttribute(ExifInterface.TAG_RW2_ISO),
            make = exifInterface.getAttribute(ExifInterface.TAG_MAKE),
            model = exifInterface.getAttribute(ExifInterface.TAG_MODEL),
            orientation = exifInterface.getAttribute(ExifInterface.TAG_ORIENTATION),
            white_balance = exifInterface.getAttribute(ExifInterface.TAG_WHITE_BALANCE),
            latitude = exifInterface.getAttribute(ExifInterface.TAG_GPS_LATITUDE),
            latitude_ref = exifInterface.getAttribute(ExifInterface.TAG_GPS_LATITUDE_REF),
            longitude = exifInterface.getAttribute(ExifInterface.TAG_GPS_LONGITUDE),
            longitude_ref = exifInterface.getAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF),
        )
    }
}

data class AlbumEntity(
    var path: String,
    var name: String,
    var alias: String? = null,
    var snapshotPath: String,
    var list: List<MediaQueryEntity>,
    var albumType: Int = TYPE_NORMAL,
): Serializable {

    companion object {
        const val TYPE_NORMAL = 0
        const val TYPE_ALL = 1
        const val TYPE_OTHER = 2
    }

    val displayName: String
        get() {
            if (alias?.isNotEmpty() == true) return alias!!
            return name
        }

}

data class CameraPhotoEntity(
    var timeStamp: Long
): Serializable {
    val list = mutableStateListOf<MediaQueryEntity>()
}