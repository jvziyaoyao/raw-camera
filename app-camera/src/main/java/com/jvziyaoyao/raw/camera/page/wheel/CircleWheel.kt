package com.jvziyaoyao.raw.camera.page.wheel

import android.util.Log
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FloatExponentialDecaySpec
import androidx.compose.animation.core.generateDecayAnimationSpec
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.math.absoluteValue
import kotlin.math.atan
import kotlin.math.pow
import kotlin.math.sqrt

data class Point(val x: Double, val y: Double)

fun angleBetweenOriginAndPoint(point: Point): Double {
    // 计算斜率
    val slope = if (point.x != 0.0) point.y / point.x else Double.POSITIVE_INFINITY

    // 使用反三角函数计算角度值
    val angle = atan(slope)

    // 将弧度转换为角度
    var degrees = Math.toDegrees(angle)

    // 调整角度值范围为 [0, 360)
    if (point.x < 0) {
        degrees += 180
    } else if (point.y < 0) {
        degrees += 360
    }

    return degrees
}

fun angleBetweenTwoPoints(point1: Point, point2: Point): Double {
    // 计算两个点与原点的角度
    val angle1 = angleBetweenOriginAndPoint(point1)
    val angle2 = angleBetweenOriginAndPoint(point2)

    // 计算两个角度之间的差值
    var angle = angle2 - angle1

    // 将角度值调整为 [0, 360)
    if (angle < 0) {
        angle += 360
    }

    return angle
}

val decay = FloatExponentialDecaySpec(
    frictionMultiplier = 1.8f,
).generateDecayAnimationSpec<Float>()

fun calculateOppositeSide(hypotenuse: Double, adjacentSide: Double): Double {
    val oppositeSideSquared = hypotenuse.pow(2) - adjacentSide.pow(2)
    return sqrt(oppositeSideSquared)
}

// 计算圆上的点的坐标
private fun calculatePoint(
    centerX: Float,
    centerY: Float,
    radius: Float,
    angleDegrees: Float
): Offset {
    val angleRadians = Math.toRadians(angleDegrees.toDouble())
    val x = centerX + radius * Math.cos(angleRadians).toFloat()
    val y = centerY + radius * Math.sin(angleRadians).toFloat()
    return Offset(x, y)
}

class CircleWheelState<T>(
    val items: List<CircularItem<T>>,
    useBound: Boolean = true,
    defaultItem: CircularItem<T>? = null,
) {

    val rotationAnimation = Animatable(0F)

    val pressed = mutableStateOf(false)

    val slowingDown = mutableStateOf(false)

    val currentItem = mutableStateOf<CircularItem<T>?>(defaultItem)

    init {
        runBlocking {
            defaultItem?.let {
                var defaultAngle = getRotationValue(it.angle)
                if (defaultAngle == 0F) defaultAngle = 360F
                rotationAnimation.snapTo(defaultAngle)
            }
            if (useBound) {
                val angle01 = getTargetRotationValue(items.first().angle)
                val angle02 = getTargetRotationValue(items.last().angle)
                rotationAnimation.updateBounds(
                    lowerBound = (if (angle01 < angle02) angle01 else angle02),
                    upperBound = (if (angle01 > angle02) angle01 else angle02),
                )
                Log.i(
                    "TAG",
                    "rotationAnimation.updateBounds: ${rotationAnimation.lowerBound} ~ ${rotationAnimation.upperBound}"
                )
            }
        }
    }

    fun getTargetRotationValue(rotation: Float): Float {
        return 360 - rotation
    }

    fun getRotationValue(rotation: Float): Float {
        return (rotation % 360).let {
            if (it < 0) -it else 360 - it
        }.let {
            if (it == 360F) 0F else it
        }
    }

    suspend fun animateToItem(item: CircularItem<T>) {
        var nextRotation = getTargetRotationValue(item.angle)
        if (nextRotation == 0F) nextRotation = 360F
        val originalRotation = rotationAnimation.value
        var snapRotation = originalRotation % 360
        val deltaRotation = nextRotation - snapRotation
        if (deltaRotation > 300) {
            if (snapRotation < 0) snapRotation += 360
        }
        if (snapRotation == 0F) snapRotation = 360F
        rotationAnimation.snapTo(snapRotation)
        Log.i("TAG", "animateToItem: nextRotation $snapRotation $nextRotation")
        rotationAnimation.animateTo(nextRotation)
    }

    suspend fun snapToItem(item: CircularItem<T>) {
        val nextRotation = getTargetRotationValue(item.angle)
        rotationAnimation.snapTo(nextRotation)
    }

}

@Composable
fun <T> HalfCircleWheel(
    circleWheelState: CircleWheelState<T>,
    indicatorColor: Color = Color.Red,
    wheelBackground: Color = Color.Gray.copy(0.2F),
    debugMode: Boolean = false,
) {
    LocalDensity.current.apply {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val maxWidthPx = maxWidth.toPx()
            val circularSizePx = maxWidthPx.times(1.1F)
            val radius = circularSizePx.div(2)
            val adjacentSide = maxWidthPx.div(2)
            val offsetSide =
                calculateOppositeSide(radius.toDouble(), adjacentSide.toDouble())
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(
                        (radius - offsetSide)
                            .toFloat()
                            .toDp()
                    )
                    .horizontalScroll(
                        rememberScrollState(
                            (circularSizePx - maxWidthPx)
                                .div(2)
                                .toInt()
                        ),
                        enabled = false
                    )
                    .verticalScroll(rememberScrollState(), enabled = false)
            ) {
                CircleWheel(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(circularSizePx.toDp()),
                    state = circleWheelState,
                    indicatorColor = indicatorColor,
                    wheelBackground = wheelBackground,
                    debugMode = debugMode,
                ) {
                    CircularScale(circleWheelState.items, debugMode = debugMode)
                }
            }
        }
    }
}

@Composable
fun <T> CircleWheel(
    modifier: Modifier = Modifier,
    state: CircleWheelState<T>,
    indicatorColor: Color = Color.Red,
    wheelBackground: Color = Color.Transparent,
    debugMode: Boolean = false,
    content: @Composable () -> Unit,
) {
    state.apply {
        BoxWithConstraints(
            modifier = modifier
                .size(300.dp)
                .clip(CircleShape)
                .background(wheelBackground)
        ) {
            LocalDensity.current.apply {
                val context = LocalContext.current
                val vibratorHelper = remember { VibratorHelper(context) }
                val scope = rememberCoroutineScope()
                val maxWidthPx = maxWidth.toPx()
                val maxHeightPx = maxHeight.toPx()
                val centerX = maxWidthPx.div(2)
                val centerY = maxHeightPx.div(2)
                val currentPosition = remember { mutableStateOf(Offset.Zero) }

                val rotationRunning = remember(slowingDown.value, pressed.value) {
                    derivedStateOf { slowingDown.value || pressed.value }
                }

                LaunchedEffect(rotationRunning.value) {
                    if (!rotationRunning.value) {
                        currentItem.value?.let { animateToItem(it) }
                    }
                }
                LaunchedEffect(rotationAnimation.value) {
                    val rotation = getRotationValue(rotationAnimation.value)
                    var preItem: CircularItem<T>? = null
                    for ((index, circularItem) in items.withIndex()) {
                        if (circularItem.value == null) continue
                        if (index == 0) {
                            preItem = circularItem
                            continue
                        }
                        if (preItem != null) {
                            if (rotation in preItem.angle..circularItem.angle) {
                                val nextItem =
                                    if ((preItem.angle - rotation).absoluteValue < (circularItem.angle - rotation).absoluteValue)
                                        preItem else circularItem
                                if (nextItem != currentItem.value) {
                                    currentItem.value = nextItem
                                    vibratorHelper.playWheelVibrate()
                                }
                                break
                            }
                        }
                        preItem = circularItem
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            val velocityTracker = VelocityTracker()
                            var lastChangedAngle = 0F
                            detectDragGestures(
                                onDragStart = {
                                    slowingDown.value = false
                                    pressed.value = true
                                    velocityTracker.resetTracking()
                                },
                                onDragEnd = {
                                    pressed.value = false
                                    val velocity = try {
                                        velocityTracker.calculateVelocity()
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                        null
                                    }
                                    velocity?.let { v ->
                                        val vx = v.x
                                        if ((lastChangedAngle > 0 && vx < 0)
                                            || (lastChangedAngle < 0 && vx > 0)
                                        ) {
                                            if (vx.absoluteValue > 100) {
                                                scope.launch {
                                                    slowingDown.value = true
                                                    rotationAnimation.animateDecay(v.x, decay)
                                                    slowingDown.value = false
                                                }
                                            }
                                        }
                                    }
                                },
                                onDrag = { change, dragAmount ->
                                    currentPosition.value = change.position
                                    var angle = angleBetweenTwoPoints(
                                        Point(
                                            x = change.position.x.toDouble() - centerX,
                                            y = change.position.y.toDouble() - centerY,
                                        ),
                                        Point(
                                            x = change.previousPosition.x.toDouble() - centerX,
                                            y = change.previousPosition.y.toDouble() - centerY,
                                        ),
                                    )
                                    if (angle > 180) angle -= 360
                                    val nextAngle = rotationAnimation.value - angle.toFloat()
                                    scope.launch {
                                        rotationAnimation.snapTo(nextAngle)
                                    }
                                    lastChangedAngle = angle.toFloat()
                                    // 添加到手势加速度
                                    velocityTracker.addPosition(
                                        change.uptimeMillis,
                                        Offset(nextAngle, 0F),
                                    )
                                }
                            )
                        }
                ) {
                    if (debugMode) {
                        val botWidth = 20.dp
                        Box(
                            modifier = Modifier
                                .graphicsLayer {
                                    transformOrigin = TransformOrigin.Center
                                    translationX = currentPosition.value.x - botWidth
                                        .div(2)
                                        .toPx()
                                    translationY = currentPosition.value.y - botWidth
                                        .div(2)
                                        .toPx()
                                }
                                .size(botWidth)
                                .clip(CircleShape)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                rotationZ = rotationAnimation.value
                                transformOrigin = TransformOrigin.Center
                            }
                    ) {
                        content()
                    }

                    Box(
                        modifier = Modifier
                            .height(
                                maxHeightPx
                                    .times(0.04F)
                                    .toDp()
                            )
                            .width(2.dp)
                            .background(indicatorColor)
                            .align(Alignment.TopCenter)
                    )

                    if (debugMode) {
                        Box(
                            modifier = Modifier
                                .height(
                                    maxHeightPx
                                        .div(2)
                                        .toDp()
                                )
                                .width(2.dp)
                                .background(Color.Red.copy(0.4F))
                                .align(Alignment.TopCenter)
                        )
                    }
                }

                if (debugMode) {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .background(Color.White),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        val textColor = if (rotationRunning.value) Color.Red else Color.Black
                        Text(text = "${currentItem.value?.value}", color = textColor)
                        Text(text = "${rotationAnimation.value}", color = textColor)
                    }
                }
            }
        }
    }
}

data class CircularItem<T>(
    val angle: Float,
    val label: String?,
    val primary: Boolean,
    val value: T?,
)

@Composable
fun <T> CircularScale(
    items: List<CircularItem<T>>,
    debugMode: Boolean = false,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1F)
    ) {
        LocalDensity.current.apply {
            BoxWithConstraints(
                modifier = Modifier.fillMaxSize()
            ) {
                val maxWidthPx = maxWidth.toPx()
                val maxHeightPx = maxHeight.toPx()
                val centerX = maxWidthPx.div(2)
                val centerY = maxHeightPx.div(2)
                val strokeWidth = 4f
                val outerRadius = centerX.times(0.98F)
                val innerRadius = centerX.times(0.92F)
                val textRadius = centerX.times(0.88F)
                val fontSize = centerX.times(0.054F)
                val textMeasurer = rememberTextMeasurer()
                Canvas(
                    modifier = Modifier
                        .graphicsLayer {
                            rotationZ = -90F
                        }
                        .fillMaxSize()
                ) {
                    // 绘制刻度
                    items.forEachIndexed { index, circularItem ->
                        circularItem.apply {
                            val start = calculatePoint(centerX, centerY, outerRadius, angle)
                            val end = calculatePoint(centerX, centerY, innerRadius, angle)
                            val lineColor = if (circularItem.primary) Color.White.copy(0.88F)
                            else Color.White.copy(0.2F)
                            drawLine(
                                lineColor,
                                Offset(start.x, start.y),
                                Offset(end.x, end.y),
                                strokeWidth
                            )

                            if (label != null) {
                                val textStyle = TextStyle(fontSize = fontSize.toSp())
                                val textLayoutResult =
                                    textMeasurer.measure(AnnotatedString(label), textStyle)
                                val textOffset = textLayoutResult.size.run {
                                    Offset(
                                        x = centerX - width.div(2),
                                        y = centerY - height.div(2),
                                    )
                                }
                                val textStart =
                                    calculatePoint(centerX, centerY, textRadius, angle)
                                if (debugMode) {
                                    drawLine(
                                        Color.White.copy(0.2F),
                                        Offset(textStart.x, textStart.y),
                                        Offset(centerX, centerY),
                                        strokeWidth
                                    )
                                }
                                translate(
                                    left = textStart.x - centerX,
                                    top = textStart.y - centerY,
                                ) {
                                    rotate(degrees = angle + 90) {
                                        translate(
                                            left = textOffset.x,
                                            top = textOffset.y,
                                        ) {
                                            drawText(
                                                textLayoutResult = textLayoutResult,
                                                color = lineColor,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

}