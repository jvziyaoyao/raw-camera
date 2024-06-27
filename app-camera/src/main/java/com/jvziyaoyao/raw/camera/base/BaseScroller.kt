package com.jvziyaoyao.raw.camera.base

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.jvziyaoyao.raw.camera.ui.theme.Layout
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Date
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

/**
 * @program: ImageGallery
 *
 * @description:
 *
 * @author: JVZIYAOYAO
 *
 * @create: 2023-05-23 10:28
 **/

val MONTH_SHORT =
    arrayOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sept", "Oct", "Nov", "Dec")

const val DAY_LENGTH = 1000 * 60 * 60 * 24

fun getTimeLabel(timeStamp: Long): String {
    val calendar = Calendar.getInstance()
    calendar.set(Calendar.HOUR_OF_DAY, 0)
    calendar.set(Calendar.MINUTE, 0)
    calendar.set(Calendar.SECOND, 0)
    calendar.set(Calendar.MILLISECOND, 0)
    val thisYear = calendar.get(Calendar.YEAR)
    val todayZero = calendar.time.time
    if (timeStamp - todayZero >= 0) return "Today"
    if (timeStamp - todayZero + DAY_LENGTH > 0) return "Yesterday"
    val date = Date(timeStamp)
    calendar.time = date
    val year = calendar.get(Calendar.YEAR)
    val month = calendar.get(Calendar.MONTH)
    val day = calendar.get(Calendar.DATE)
    val res = StringBuilder()
    if (thisYear != year) res.append("$year ")
    res.append("${MONTH_SHORT[month]} ")
    res.append("$day ")
    return res.toString()
}


fun formatTime(duration: Long): String {
    val hours = TimeUnit.MILLISECONDS.toHours(duration)
    val minutes = TimeUnit.MILLISECONDS.toMinutes(duration) % 60
    val seconds = TimeUnit.MILLISECONDS.toSeconds(duration) % 60
    var res = String.format("%02d:%02d", minutes, seconds)
    if (hours == 0L) return res
    res = String.format("%02d:$res", hours)
    return res
}

@Composable
fun ScrollBarContainer(
    totalHeight: Float,
    index2PositionMap: Map<Int, Float>,
    index2TimeMap: Map<Int, Long?>,
    lazyGridState: LazyGridState,
    content: @Composable () -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        // 内容放在这里
        content()
        // 下面是滚动的逻辑
        val scope = rememberCoroutineScope()
        var scrollBarPressed by remember { mutableStateOf(false) }

        val progress = lazyGridState.run {
            val itemPosition = index2PositionMap[firstVisibleItemIndex] ?: 0F
            val position = itemPosition + firstVisibleItemScrollOffset
            position.div(totalHeight)
        }

        val labelText = lazyGridState.run {
            // 设置侧边的时间戳
            val timeStamp = index2TimeMap[firstVisibleItemIndex]
            if (timeStamp != null) {
                getTimeLabel(timeStamp)
            } else {
                ""
            }
        }

        val noMore = lazyGridState.run {
            if (layoutInfo.visibleItemsInfo.isEmpty()) true else {
                // 判断当前是否已经滚到到底
                val totalItemCount = layoutInfo.totalItemsCount
                val lastVisibleItem = layoutInfo.visibleItemsInfo.last()
                (totalItemCount == lastVisibleItem.index + 1)
                        && ((lastVisibleItem.size.height + lastVisibleItem.offset.y) == (layoutInfo.viewportSize.height))
            }
        }

        val showScrollBar by remember {
            derivedStateOf { scrollBarPressed || lazyGridState.isScrollInProgress }
        }
        val scrollerBarAlpha = animateFloatAsState(
            targetValue = if (showScrollBar) 1F else 0F,
            animationSpec = tween(delayMillis = if (showScrollBar) 0 else 2000)
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .alpha(scrollerBarAlpha.value),
            contentAlignment = Alignment.CenterEnd
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight(0.8F)
                    .padding(end = Layout.padding.pm)
            ) {
                ScrollerBar(
                    progress = progress,
                    labelText = labelText,
                    canScrollDown = !noMore,
                    onPressChanged = { pressed ->
                        scrollBarPressed = pressed
                    }) {
                    scope.launch {
                        val nextPosition = totalHeight * it
                        for (pair in index2PositionMap.toSortedMap().toList()) {
                            if (pair.second >= nextPosition) {
                                lazyGridState.scrollToItem(
                                    pair.first,
                                    (nextPosition - pair.second).roundToInt()
                                )
                                break
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ScrollerBar(
    progress: Float = 0F,
    canScrollDown: Boolean = true,
    labelText: String = "",
    onPressChanged: (Boolean) -> Unit = {},
    onProgressChanged: (Float) -> Unit = {},
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxHeight()
    ) {
        val density = LocalDensity.current
        val lineHeightPx = remember { density.run { maxHeight.toPx() } }
        var blockHeight by remember { mutableStateOf(0) }
        val allLength by remember { derivedStateOf { lineHeightPx - blockHeight } }
        var offsetY by remember { mutableStateOf(0F) }
        var pressed by remember { mutableStateOf(false) }
        LaunchedEffect(key1 = pressed, key2 = progress) {
            if (progress <= 1) {
                if (!pressed) offsetY = progress * allLength
            }
        }
        LaunchedEffect(pressed) {
            onPressChanged(pressed)
        }
        Row(
            modifier = Modifier
                .height(40.dp)
                .offset {
                    IntOffset(0, offsetY.roundToInt())
                }
                .onGloballyPositioned {
                    blockHeight = it.size.height
                },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.End,
        ) {
            val showLabelText by remember(
                key1 = labelText,
                key2 = pressed
            ) { derivedStateOf { pressed && labelText.isNotEmpty() } }
            AnimatedVisibility(visible = showLabelText, enter = fadeIn(), exit = fadeOut()) {
                Box(
                    modifier = Modifier
                        .clip(Layout.roundShape.rs)
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(
                            horizontal = Layout.padding.pm,
                            vertical = Layout.padding.pxs,
                        )
                ) {
                    Text(
                        text = labelText,
                        fontSize = Layout.fontSize.fs,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            }
            Spacer(modifier = Modifier.width(Layout.padding.pm))
            Box(
                modifier = Modifier
                    .shadow(elevation = 4.dp)
                    .clip(Layout.roundShape.rs)
                    .width(20.dp)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(Layout.padding.pxxs)
                    .draggable(orientation = Orientation.Vertical,
                        onDragStarted = {
                            pressed = true
                        },
                        onDragStopped = {
                            pressed = false
                        },
                        state = rememberDraggableState(
                            onDelta = { deltaY ->
                                if (deltaY > 0) {
                                    if (!canScrollDown) return@rememberDraggableState
                                }
                                offsetY += deltaY
                                if (offsetY < 0) offsetY = 0F
                                if (blockHeight + offsetY > lineHeightPx) {
                                    offsetY = allLength
                                }
                                val nextPercent = offsetY.div(lineHeightPx - blockHeight)
                                onProgressChanged(nextPercent)
                            }
                        )
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.Menu,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onBackground.copy(0.2f),
                )
            }
        }
    }
}