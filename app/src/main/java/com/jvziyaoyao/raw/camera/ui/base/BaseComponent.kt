package com.jvziyaoyao.raw.camera.ui.base

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import com.jvziyaoyao.raw.camera.ui.theme.Layout

/**
 * @program: TestFocusable
 *
 * @description:
 *
 * @author: JVZIYAOYAO
 *
 * @create: 2024-01-24 15:31
 **/

@Composable
fun CommonSurfaceCard(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(Layout.roundShape.rm)
            .background(MaterialTheme.colorScheme.surface)
            .padding(Layout.padding.pm)
    ) {
        content()
    }
}