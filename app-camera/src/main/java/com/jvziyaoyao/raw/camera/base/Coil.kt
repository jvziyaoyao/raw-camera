package com.jvziyaoyao.raw.camera.base

import android.os.Build
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import coil.ImageLoader
import coil.compose.rememberAsyncImagePainter
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.decode.SvgDecoder
import coil.decode.VideoFrameDecoder
import coil.request.ImageRequest
import coil.size.Size
import com.jvziyaoyao.camera.raw.util.ContextUtil
import com.jvziyaoyao.raw.camera.R

/**
 * @program: ImageGallery
 *
 * @description:
 *
 * @author: JVZIYAOYAO
 *
 * @create: 2022-07-28 23:09
 **/

// 图片加载器
val imageLoader = getCustomImageLoader()

fun getCustomImageLoader(interceptor: (ImageLoader.Builder) -> Unit = {}): ImageLoader {
    val builder = ImageLoader.Builder(ContextUtil.getApplicationByReflect())
        .components {
            // 增加gif的支持
            if (Build.VERSION.SDK_INT >= 28) {
                add(ImageDecoderDecoder.Factory())
            } else {
                add(GifDecoder.Factory())
            }
            // 增加svg的支持
            add(SvgDecoder.Factory())
            // 增加视频帧的支持
            add(VideoFrameDecoder.Factory())
        }
        .error(R.drawable.ic_error)
    interceptor(builder)
    return builder.build()
}

@Composable
fun rememberCoilImagePainter(
    path: String,
    size: Size = Size.ORIGINAL
): Painter {
    // 加载图片
    val imageRequest = ImageRequest.Builder(LocalContext.current)
        .data(path)
        .size(size)
        .build()
    // 获取图片的初始大小
    return rememberAsyncImagePainter(model = imageRequest, imageLoader = imageLoader)
}

@Composable
fun rememberGridImageLoader(model: Any): Painter {
    return rememberAsyncImagePainter(model = model, imageLoader = imageLoader)
}