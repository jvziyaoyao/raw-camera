package com.jvziyaoyao.raw.camera.page.camera

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.Color
import com.jvziyaoyao.raw.camera.base.rememberCoilImagePainter
import com.jvziyaoyao.scale.zoomable.previewer.Previewer
import com.jvziyaoyao.scale.zoomable.previewer.PreviewerState
import com.jvziyaoyao.scale.zoomable.previewer.TransformLayerScope
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun CameraPreviewer(
    images: List<File>,
    previewerState: PreviewerState
) {
    val scope = rememberCoroutineScope()
    if (previewerState.canClose || previewerState.animating) {
        BackHandler {
            scope.launch {
                previewerState.exitTransform()
            }
        }
    }
    Previewer(
        state = previewerState,
        previewerLayer = TransformLayerScope(
            background = {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(0.8F))
                )
            }
        ),
        zoomablePolicy = { index ->
            val painter = rememberCoilImagePainter(image = images[index])
            if (painter.intrinsicSize.isSpecified) {
                ZoomablePolicy(intrinsicSize = painter.intrinsicSize) {
                    Image(
                        modifier = Modifier.fillMaxSize(),
                        painter = painter,
                        contentDescription = null,
                    )
                }
            } else {
                Box(modifier = Modifier.fillMaxSize()) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
            }
            painter.intrinsicSize.isSpecified
        }
    )
}