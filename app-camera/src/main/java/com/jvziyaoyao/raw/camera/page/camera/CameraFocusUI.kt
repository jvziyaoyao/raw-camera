package com.jvziyaoyao.raw.camera.page.camera

import android.hardware.camera2.CameraMetadata
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculateCentroidSize
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateRotation
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastAny
import com.jvziyaoyao.camera.raw.holder.camera.aeState
import com.jvziyaoyao.camera.raw.holder.camera.afState
import com.jvziyaoyao.camera.raw.holder.camera.getFingerPointRect
import com.jvziyaoyao.camera.raw.holder.camera.rectFromNormalized
import com.jvziyaoyao.camera.raw.holder.camera.rectNormalized
import com.jvziyaoyao.camera.raw.holder.camera.zoomRatioRange
import com.jvziyaoyao.raw.camera.ui.theme.Layout
import com.jvziyaoyao.raw.camera.util.formatToDecimalPlaces
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.absoluteValue

@Composable
fun FocusCornerContent(
    modifier: Modifier = Modifier,
    color: Color = Color.Cyan,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .drawWithContent {
                val strokeWidth = size.width.div(10)
                drawLine(
                    color = color,
                    start = Offset(size.width - strokeWidth.div(2), 0F),
                    end = Offset(size.width - strokeWidth.div(2), size.height),
                    strokeWidth = strokeWidth,
                )
                drawLine(
                    color = color,
                    start = Offset(0F, size.height - strokeWidth.div(2)),
                    end = Offset(size.width, size.height - strokeWidth.div(2)),
                    strokeWidth = strokeWidth,
                )
            }
    )
}

@Composable
fun FocusRectContent(
    modifier: Modifier = Modifier,
    color: Color = Color.Cyan,
) {
    BoxWithConstraints(
        modifier = modifier.fillMaxSize(),
    ) {
        var cornerSize = maxWidth.div(3)
        if (cornerSize > 20.dp) cornerSize = 20.dp
        FocusCornerContent(
            modifier = Modifier
                .size(cornerSize)
                .align(Alignment.BottomEnd),
            color = color,
        )
        FocusCornerContent(
            modifier = Modifier
                .size(cornerSize)
                .rotate(-90F)
                .align(Alignment.TopEnd),
            color = color,
        )
        FocusCornerContent(
            modifier = Modifier
                .size(cornerSize)
                .rotate(-180F)
                .align(Alignment.TopStart),
            color = color,
        )
        FocusCornerContent(
            modifier = Modifier
                .size(cornerSize)
                .rotate(90F)
                .align(Alignment.BottomStart),
            color = color,
        )
    }
}

data class FocusRequestOrientation(
    var pitch: Float,
    var roll: Float,
    var yaw: Float,
    var timestamp: Long = System.currentTimeMillis(),
)

@Composable
fun CameraFocusLayer() {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()

    ) {
        val scope = rememberCoroutineScope()
        val density = LocalDensity.current
        val maxWidthPx = density.run { maxWidth.toPx() }
        val maxHeightPx = density.run { maxHeight.toPx() }
        val viewModel: CameraViewModel = koinViewModel()
        val fingerClick = remember { mutableStateOf(Offset.Zero) }
        val fingerRect = remember { mutableStateOf(Rect.Zero) }
        val focusPointRect = remember { mutableStateOf(Rect.Zero) }
        val focusScale = remember { Animatable(1F) }
        val afLockedState = remember { mutableStateOf(false) }
        val aeLockedState = remember { mutableStateOf(false) }
        val focusRequestOrientation = remember { mutableStateOf<FocusRequestOrientation?>(null) }
        val captureResult = viewModel.captureResultFlow.collectAsState()
        val characteristics =
            viewModel.currentCameraCharacteristicsFlow.collectAsState(initial = null)
        val zoomRatio = viewModel.captureController.zoomRatioFlow.collectAsState()
        val focusRequestTrigger = viewModel.focusRequestTriggerFlow.collectAsState()
        LaunchedEffect(captureResult.value) {
            afLockedState.value =
                captureResult.value?.afState == CameraMetadata.CONTROL_AF_STATE_FOCUSED_LOCKED
            aeLockedState.value =
                captureResult.value?.aeState == CameraMetadata.CONTROL_AE_STATE_CONVERGED
                        || captureResult.value?.aeState == CameraMetadata.CONTROL_AE_STATE_FLASH_REQUIRED
        }
        val currentPitch = viewModel.pitchFlow.collectAsState()
        val currentRoll = viewModel.rollFlow.collectAsState()
        val currentYaw = viewModel.yawFlow.collectAsState()
        fun cancelFocus() {
            scope.launch {
                viewModel.focusCancel()
                delay(2000L)
                if (focusRequestTrigger.value?.focusCancel == true) {
                    viewModel.focusIdle()
                }
            }
        }
        LaunchedEffect(
            currentPitch.value,
            currentRoll.value,
            currentYaw.value,
        ) {
            val srcOrientation = focusRequestOrientation.value
            srcOrientation?.apply {
                val delta = 10F
                val delay = 1000 * 2 // 两秒内不要取消
                if (
                    (System.currentTimeMillis() - timestamp > delay)
                    && ((currentPitch.value - pitch).absoluteValue > delta
                            || (currentRoll.value - roll).absoluteValue > delta
                            || (currentYaw.value - yaw).absoluteValue > delta)
                    && focusRequestTrigger.value?.focusCancel != true
                    && focusRequestTrigger.value?.focusIdle != true
                ) {
                    cancelFocus()
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTransformGestures(
                        onTap = {
                            val fingerSize = maxWidthPx
                                .div(5.4F)
                            val rect = getFingerPointRect(
                                it.x,
                                it.y,
                                maxWidthPx,
                                maxHeightPx,
                                fingerSize
                            )
                            fingerClick.value = it
                            fingerRect.value = rect
                            focusPointRect.value = rectNormalized(rect, maxWidthPx, maxHeightPx)
                            afLockedState.value = false
                            aeLockedState.value = false
                            focusRequestOrientation.value = FocusRequestOrientation(
                                pitch = viewModel.pitchFlow.value,
                                roll = viewModel.rollFlow.value,
                                yaw = viewModel.yawFlow.value,
                            )
                            viewModel.focusRequest(focusPointRect.value)
                            scope.launch {
                                focusScale.snapTo(2F)
                                focusScale.animateTo(1F)
                            }
                        },
                        onGesture = { _, _, zoom, _, _ ->
                            characteristics.value?.zoomRatioRange?.let { zoomRatioRange ->
                                val currentZoomRatio = zoomRatio.value
                                var nextZoomRatio = currentZoomRatio * zoom
                                if (nextZoomRatio > zoomRatioRange.upper) nextZoomRatio =
                                    zoomRatioRange.upper
                                if (nextZoomRatio < zoomRatioRange.lower) nextZoomRatio =
                                    zoomRatioRange.lower
                                viewModel.captureController.zoomRatioFlow.value = nextZoomRatio
                            }
                            true
                        }
                    )
                }
        ) {
            val wrapAlpha =
                animateFloatAsState(
                    targetValue =
                    if (focusRequestTrigger.value?.focusCancel == true) 0.5F
                    else if (focusRequestTrigger.value?.focusIdle != true) 1F
                    else 0F
                )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(wrapAlpha.value)
            ) {
                val pointRect = rectFromNormalized(focusPointRect.value, maxWidthPx, maxHeightPx)
                val rectAlpha =
                    animateFloatAsState(targetValue = if (afLockedState.value) 1F else 0.4F)
                FocusRectContent(
                    modifier = Modifier
                        .graphicsLayer {
                            translationX = pointRect.left
                            translationY = pointRect.top
                            scaleX = focusScale.value
                            scaleY = focusScale.value
                            alpha = rectAlpha.value
                            transformOrigin = TransformOrigin.Center
                        }
                        .size(
                            width = density.run { pointRect.width.toDp() },
                            height = density.run { pointRect.height.toDp() },
                        )

                )

                val iconSize = 20.dp
                val iconAlpha =
                    animateFloatAsState(targetValue = if (aeLockedState.value) 1F else 0.4F)
                Icon(
                    modifier = Modifier
                        .size(iconSize)
                        .graphicsLayer {
                            translationX = if (pointRect.center.x < maxWidthPx.div(2)) {
                                pointRect.right + 20F
                            } else {
                                pointRect.left - 20F - density.run { iconSize.toPx() }
                            }
                            translationY =
                                pointRect.top + pointRect.height.div(2) - density
                                    .run { iconSize.toPx() }
                                    .div(2)
                            alpha = iconAlpha.value
                        },
                    imageVector = Icons.Filled.WbSunny,
                    tint = MaterialTheme.colorScheme.primary,
                    contentDescription = null
                )
            }

            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = Layout.padding.pl)
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onBackground.copy(0.4F))
            ) {
                Text(
                    modifier = Modifier.align(Alignment.Center),
                    text = "${zoomRatio.value.formatToDecimalPlaces(1)}X",
                    fontSize = Layout.fontSize.fxs,
                    color = MaterialTheme.colorScheme.background.copy(0.6F),
                )
            }
        }
    }
}

suspend fun PointerInputScope.detectTransformGestures(
    panZoomLock: Boolean = false,
    gestureStart: () -> Unit = {},
    gestureEnd: (Boolean) -> Unit = {},
    onTap: (Offset) -> Unit = {},
    onDoubleTap: (Offset) -> Unit = {},
    onGesture: (centroid: Offset, pan: Offset, zoom: Float, rotation: Float, event: PointerEvent) -> Boolean,
) {
    var lastReleaseTime = 0L
    var scope: CoroutineScope? = null
    awaitEachGesture {
        var rotation = 0f
        var zoom = 1f
        var pan = Offset.Zero
        var pastTouchSlop = false
        val touchSlop = viewConfiguration.touchSlop
        var lockedToPanZoom = false

        awaitFirstDown(requireUnconsumed = false)
        val t0 = System.currentTimeMillis()
        var releasedEvent: PointerEvent? = null
        var moveCount = 0
        // 这里开始事件
        gestureStart()
        do {
            val event = awaitPointerEvent()
            if (event.type == PointerEventType.Release) releasedEvent = event
            if (event.type == PointerEventType.Move) moveCount++
            val canceled = event.changes.fastAny { it.isConsumed }
            if (!canceled) {
                val zoomChange = event.calculateZoom()
                val rotationChange = event.calculateRotation()
                val panChange = event.calculatePan()

                if (!pastTouchSlop) {
                    zoom *= zoomChange
                    rotation += rotationChange
                    pan += panChange

                    val centroidSize = event.calculateCentroidSize(useCurrent = false)
                    val zoomMotion = abs(1 - zoom) * centroidSize
                    val rotationMotion = abs(rotation * PI.toFloat() * centroidSize / 180f)
                    val panMotion = pan.getDistance()

                    if (zoomMotion > touchSlop ||
                        rotationMotion > touchSlop ||
                        panMotion > touchSlop
                    ) {
                        pastTouchSlop = true
                        lockedToPanZoom = panZoomLock && rotationMotion < touchSlop
                    }
                }
                if (pastTouchSlop) {
                    val centroid = event.calculateCentroid(useCurrent = false)
                    val effectiveRotation = if (lockedToPanZoom) 0f else rotationChange
                    if (effectiveRotation != 0f ||
                        zoomChange != 1f ||
                        panChange != Offset.Zero
                    ) {
                        if (!onGesture(
                                centroid,
                                panChange,
                                zoomChange,
                                effectiveRotation,
                                event
                            )
                        ) break
                    }
                }
            }
        } while (!canceled && event.changes.fastAny { it.pressed })

        var t1 = System.currentTimeMillis()
        val dt = t1 - t0
        val dlt = t1 - lastReleaseTime

        if (moveCount == 0) releasedEvent?.let { e ->
            if (e.changes.isEmpty()) return@let
            val offset = e.changes.first().position
            if (dlt < 272) {
                t1 = 0L
                scope?.cancel()
                onDoubleTap(offset)
            } else if (dt < 200) {
                scope = MainScope()
                scope?.launch(Dispatchers.Main) {
                    delay(272)
                    onTap(offset)
                }
            }
            lastReleaseTime = t1
        }

        // 这里是事件结束
        gestureEnd(moveCount != 0)
    }
}