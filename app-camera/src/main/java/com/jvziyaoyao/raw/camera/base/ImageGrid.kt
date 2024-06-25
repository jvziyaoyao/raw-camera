package com.jvziyaoyao.raw.camera.base

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.jvziyaoyao.raw.camera.domain.model.MediaQueryEntity
import com.jvziyaoyao.raw.camera.ui.theme.Layout
import com.jvziyaoyao.scale.zoomable.previewer.PreviewerState
import com.jvziyaoyao.scale.zoomable.previewer.TransformItemView
import com.jvziyaoyao.scale.zoomable.previewer.rememberTransformItemState

/**
 * @program: ImageGallery
 *
 * @description:
 *
 * @author: JVZIYAOYAO
 *
 * @create: 2023-05-21 15:25
 **/

@Composable
fun ImageGrid(
    mediaQueryEntity: MediaQueryEntity,
    selectedMode: Boolean,
    previewerState: PreviewerState,
    selected: Boolean,
    onPress: () -> Unit,
    onLongPress: () -> Unit,
    onSelected: () -> Unit,
    onUnselected: () -> Unit,
) {
    val path = mediaQueryEntity.path ?: ""
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.onBackground.copy(0.1F)),
        contentAlignment = Alignment.Center
    ) {
        ScaleGrid(
            detectGesture = DetectScaleGridGesture(
                onPress = onPress,
                onLongPress = onLongPress
            ),
        ) {
            val painter = rememberGridImageLoader(
                model = path
            )
            val itemState =
                rememberTransformItemState(intrinsicSize = painter.intrinsicSize)
            TransformItemView(
                key = path,
                itemState = itemState,
                transformState = previewerState,
            ) {
                Image(
                    modifier = Modifier.fillMaxSize(),
                    painter = painter,
                    contentScale = ContentScale.Crop,
                    contentDescription = null,
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxSize(),
                contentAlignment = Alignment.BottomStart
            ) {
                if (mediaQueryEntity.isVideo == true) {
                    val duration = mediaQueryEntity.duration
                    if (duration != null) {
                        val durationLabel = remember { formatTime(duration) }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(2.4F)
                                .background(
                                    brush = Brush.verticalGradient(
                                        colors = listOf(
                                            Color.Transparent,
                                            MaterialTheme.colorScheme.onBackground,
                                        )
                                    )
                                )
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(Layout.padding.pxxs),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val labelColor = MaterialTheme.colorScheme.surface.copy(0.8f)
                            Icon(
                                modifier = Modifier.size(20.dp),
                                imageVector = Icons.Filled.PlayCircle,
                                contentDescription = null,
                                tint = labelColor,
                            )
                            Spacer(modifier = Modifier.width(Layout.padding.pxxs))
                            Text(
                                text = durationLabel,
                                fontSize = Layout.fontSize.fs,
                                color = labelColor
                            )
                        }
                    }
                }
                AnimatedVisibility(
                    visible = selectedMode,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Box(
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.onSurface.copy(0.2F))
                            .fillMaxSize()
                            .pointerInput(selected) {
                                detectTapGestures {
                                    if (selected) {
                                        onUnselected()
                                    } else {
                                        onSelected()
                                    }
                                }
                            },
                        contentAlignment = Alignment.TopCenter
                    ) {
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.TopEnd
                        ) {
                            CheckButton(
                                check = selected,
                                key = path,
                                modifier = Modifier
                                    .size(32.dp)
                                    .padding(top = 4.dp, end = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CheckButton(
    modifier: Modifier = Modifier,
    check: Boolean,
    hideCircle: Boolean = false,
    checkColor: Color = MaterialTheme.colorScheme.surface,
    uncheckColor: Color = MaterialTheme.colorScheme.surface,
    key: Any = Unit,
    onChangeAction: (() -> Unit)? = null,
) {
    Box(
        modifier = modifier
            .pointerInput(key) {
                if (onChangeAction != null) {
                    detectTapGestures {
                        onChangeAction()
                    }
                }
            },
        contentAlignment = Alignment.TopEnd,
    ) {
        AnimatedVisibility(
            visible = check,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = null,
                tint = checkColor,
                modifier = Modifier
                    .size(18.dp)
            )
        }
        AnimatedVisibility(
            visible = !check && !hideCircle,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .size(18.dp)
                    .border(1.dp, uncheckColor, CircleShape),
            )
        }
    }
}