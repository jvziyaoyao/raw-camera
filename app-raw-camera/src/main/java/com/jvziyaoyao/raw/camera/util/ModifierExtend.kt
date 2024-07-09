package com.jvziyaoyao.raw.camera.util

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput

/**
 * @program: ImageGallery
 *
 * @description:
 *
 * @author: JVZIYAOYAO
 *
 * @create: 2022-08-05 10:03
 **/

fun Modifier.preventPointerInput(): Modifier = this.run {
    pointerInput(Unit) {
        detectTapGestures { }
    }
}