package com.jvziyaoyao.raw.camera.page.camera

import android.hardware.camera2.params.Face
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.jvziyaoyao.camera.flow.holder.camera.availableFaceDetectMaxCount
import com.jvziyaoyao.camera.flow.holder.camera.faceDetectResult
import com.jvziyaoyao.camera.flow.holder.camera.isFrontCamera
import com.jvziyaoyao.camera.flow.holder.camera.sensorDetectRect2ComposeRect
import com.jvziyaoyao.camera.flow.holder.camera.sensorSize
import org.koin.androidx.compose.koinViewModel

@Composable
fun DetectCornerContent(
    modifier: Modifier = Modifier,
    color: Color = Color.Cyan,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .drawWithContent {
                val strokeWidth = size.width.div(10)
                drawArc(
                    color = color,
                    startAngle = 0F,
                    sweepAngle = 90F,
                    size = Size(size.width - strokeWidth, height = size.height - strokeWidth),
                    topLeft = Offset(strokeWidth.div(2), strokeWidth.div(2)),
                    useCenter = false,
                    style = Stroke(width = strokeWidth)
                )
                drawLine(
                    color = color,
                    start = Offset(size.width - strokeWidth.div(2), 0F),
                    end = Offset(size.width - strokeWidth.div(2), size.height.div(2)),
                    strokeWidth = strokeWidth,
                )
                drawLine(
                    color = color,
                    start = Offset(0F, size.height - strokeWidth.div(2)),
                    end = Offset(size.width.div(2), size.height - strokeWidth.div(2)),
                    strokeWidth = strokeWidth,
                )
            }
    )
}

@Composable
fun DetectRectContent(
    modifier: Modifier = Modifier,
    color: Color = Color.Cyan,
) {
    BoxWithConstraints(
        modifier = modifier.fillMaxSize(),
    ) {
        var cornerSize = maxWidth.div(3)
        if (cornerSize > 20.dp) cornerSize = 20.dp
        DetectCornerContent(
            modifier = Modifier
                .size(cornerSize)
                .align(Alignment.BottomEnd),
            color = color,
        )
        DetectCornerContent(
            modifier = Modifier
                .size(cornerSize)
                .rotate(-90F)
                .align(Alignment.TopEnd),
            color = color,
        )
        DetectCornerContent(
            modifier = Modifier
                .size(cornerSize)
                .rotate(-180F)
                .align(Alignment.TopStart),
            color = color,
        )
        DetectCornerContent(
            modifier = Modifier
                .size(cornerSize)
                .rotate(90F)
                .align(Alignment.BottomStart),
            color = color,
        )
    }
}

@Composable
fun CameraFaceDetectLayer() {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val viewModel: CameraViewModel = koinViewModel()
        val currentCameraPair = viewModel.currentCameraPairFlow.collectAsState()
        val cameraCharacteristics = currentCameraPair.value?.second
        val captureResult = viewModel.captureResultFlow.collectAsState()

        val faceMaxCount = cameraCharacteristics?.availableFaceDetectMaxCount ?: 0
        val faceList = remember(faceMaxCount) { Array<Face?>(faceMaxCount) { null } }
        LaunchedEffect(captureResult.value) {
            captureResult.value?.faceDetectResult?.let { currentList ->
                for (i in faceList.indices) {
                    if (i > currentList.lastIndex) {
                        faceList[i] = null
                    } else {
                        faceList[i] = currentList[i]
                    }
                }
            }
        }

        density.run {
            val size = Size(maxWidth.toPx(), maxHeight.toPx())
            cameraCharacteristics?.apply {
                if (sensorSize != null) {
                    val sensorWidth = sensorSize!!.width()
                    val sensorHeight = sensorSize!!.height()
                    faceList.forEach { face ->
                        val faceRect = remember { mutableStateOf<Rect?>(null) }
                        if (face != null) {
                            faceRect.value = sensorDetectRect2ComposeRect(
                                rect = face.bounds,
                                rotationOrientation = viewModel.rotationOrientation.value,
                                flipHorizontal = isFrontCamera,
                                size = size,
                                sensorWidth = sensorWidth,
                                sensorHeight = sensorHeight,
                            )
                        }

                        faceRect.value?.let {
                            val animationSpec = spring<Float>()
                            val offsetXAnimation =
                                animateFloatAsState(
                                    targetValue = it.left,
                                    animationSpec = animationSpec,
                                )
                            val offsetYAnimation =
                                animateFloatAsState(
                                    targetValue = it.top,
                                    animationSpec = animationSpec,
                                )
                            val widthAnimation =
                                animateFloatAsState(
                                    targetValue = it.width,
                                    animationSpec = animationSpec,
                                )
                            val heightAnimation =
                                animateFloatAsState(
                                    targetValue = it.height,
                                    animationSpec = animationSpec,
                                )
                            val alphaAnimation =
                                animateFloatAsState(targetValue = if (face == null) 0F else 1F)

                            DetectRectContent(
                                modifier = Modifier
                                    .graphicsLayer {
                                        alpha = alphaAnimation.value
                                        translationX = offsetXAnimation.value
                                        translationY = offsetYAnimation.value
                                    }
                                    .size(
                                        width = widthAnimation.value.toDp(),
                                        height = heightAnimation.value.toDp(),
                                    ),
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
            }
        }
    }
}