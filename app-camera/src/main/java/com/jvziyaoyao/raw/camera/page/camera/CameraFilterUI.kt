package com.jvziyaoyao.raw.camera.page.camera

import android.opengl.GLSurfaceView
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.jvziyaoyao.camera.flow.holder.camera.filter.ImageFilterRenderer
import com.jvziyaoyao.camera.flow.holder.camera.render.TEX_VERTEX_MAT_90
import com.jvziyaoyao.camera.flow.holder.camera.render.isEmptyImageFilter
import com.jvziyaoyao.raw.camera.ui.theme.Layout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

@Composable
fun CameraFilterLayer() {
    val scope = rememberCoroutineScope()
    val viewModel: CameraViewModel = koinViewModel()
    val showFilterList = viewModel.showFilterList

    BackHandler {
        showFilterList.value = false
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures {
                    showFilterList.value = false
                }
            }
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
        ) {
            Row(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(Layout.padding.ps),
                horizontalArrangement = Arrangement.spacedBy(Layout.padding.ps),
            ) {
                val currentImageFilterStr = viewModel.currentImageFilterFlow.collectAsState()
                val textureVertex = viewModel.textureVertexFlow.collectAsState()
                viewModel.imageFilterList.forEach { imageFilter ->
                    val isEmptyImageFilter = imageFilter.shaderStr.isEmptyImageFilter()
                    val selected = if (isEmptyImageFilter) {
                        currentImageFilterStr.value == null || currentImageFilterStr.value == imageFilter.shaderStr
                    } else {
                        currentImageFilterStr.value == imageFilter.shaderStr
                    }
                    val imageFilterRenderer = remember {
                        ImageFilterRenderer(TEX_VERTEX_MAT_90, imageFilter.shaderStr)
                    }
                    LaunchedEffect(textureVertex.value) {
                        textureVertex.value?.let {
                            imageFilterRenderer.updateTextureBuffer(it)
                        }
                    }

                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(Layout.roundShape.rs)
                            .border(
                                width = 2.dp,
                                shape = Layout.roundShape.rs,
                                color = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
                            )
                            .clickable {
                                viewModel.currentImageFilterFlow.value = imageFilter.shaderStr
                            },
                    ) {
                        AndroidView(
                            modifier = Modifier
                                .fillMaxSize(),
                            factory = { ctx ->
                                GLSurfaceView(ctx).apply {
                                    layoutParams = FrameLayout.LayoutParams(
                                        ViewGroup.LayoutParams.MATCH_PARENT,
                                        ViewGroup.LayoutParams.MATCH_PARENT
                                    )
                                    setEGLContextClientVersion(3)
                                    setRenderer(imageFilterRenderer)
                                    scope.launch(Dispatchers.IO) {
                                        viewModel.filterRendererMatFlow.collectLatest {
                                            imageFilterRenderer.currentAdditionalMat = it
                                        }
                                    }
                                }
                            }
                        )
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surface.copy(0.6F))
                                .padding(vertical = Layout.padding.pxxs),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = imageFilter.name,
                                fontSize = Layout.fontSize.fxxs,
                                color = MaterialTheme.colorScheme.onBackground.copy(0.8F)
                            )
                        }
                    }
                }
            }
        }
    }
}