package com.jvziyaoyao.raw.camera.page.wheel

import android.os.Bundle
import android.util.Log
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FloatExponentialDecaySpec
import androidx.compose.animation.core.generateDecayAnimationSpec
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
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
import com.jvziyaoyao.raw.camera.base.BaseActivity
import com.jvziyaoyao.raw.camera.ui.theme.Layout
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.math.absoluteValue
import kotlin.math.atan

class WheelActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setBasicContent {
            WheelBody()
        }
    }

}

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

@Composable
fun WheelBody() {
    val scope = rememberCoroutineScope()
    Box(modifier = Modifier.fillMaxSize()) {
        val items = mutableListOf<CircularItem>()
        val numTicks = 12
        val anglePerTick = 360f / numTicks
        for (i in 0 until numTicks) {
            val angle = i * anglePerTick
            items.add(
                CircularItem(
                    angle = angle,
                    label = "$angle",
                    primary = true,
                    value = angle.toInt(),
                )
            )
            val innerTicks = 6
            val innerPerTick = anglePerTick / innerTicks
            for (e in 1 until innerTicks) {
                val innerAngle = angle + e * innerPerTick
                items.add(
                    CircularItem(
                        angle = innerAngle,
                        label = null,
                        primary = false,
                        value = if (e % 2 == 0) innerAngle.toInt() else null,
                    )
                )
            }
        }

        val circleWheelState = remember {
            CircleWheelState(
                items = items,
                defaultItem = items[12],
            )
        }
        CircleWheel(
            modifier = Modifier.align(Alignment.Center),
            state = circleWheelState,
        ) {
            CircularScale(items)
        }

        Row(
            modifier = Modifier
                .padding(bottom = Layout.padding.pxl)
                .align(Alignment.BottomCenter),
            horizontalArrangement = Arrangement.spacedBy(Layout.padding.pm)
        ) {
            Button(onClick = {
                scope.launch {
                    circleWheelState.animateToItem(items[10])
                }
            }) {
                Text(text = "Scroll")
            }
            Button(onClick = {
                scope.launch {
                    circleWheelState.snapToItem(items[22])
                }
            }) {
                Text(text = "Snap")
            }
        }
    }
}

class CircleWheelState(
    val items: List<CircularItem>,
    defaultItem: CircularItem? = null,
) {

    val rotationAnimation = Animatable(0F)

    val pressed = mutableStateOf(false)

    val slowingDown = mutableStateOf(false)

    val currentItem = mutableStateOf<CircularItem?>(defaultItem)

    init {
        runBlocking {
            defaultItem?.let {
                rotationAnimation.snapTo(getRotationValue(it.angle))
            }
        }
    }

    fun getRotationValue(rotation: Float): Float {
        return (rotation % 360).let {
            if (it < 0) -it else 360 - it
        }.let {
            if (it == 360F) 0F else it
        }
    }

    suspend fun animateToItem(item: CircularItem) {
        val nextRotation = getRotationValue(item.angle)
        val originalRotation = rotationAnimation.value
        var snapRotation = originalRotation % 360
        val deltaRotation = nextRotation - snapRotation
        if (deltaRotation > 300) {
            if (snapRotation < 0) snapRotation += 360
        }
        rotationAnimation.snapTo(snapRotation)
        rotationAnimation.animateTo(nextRotation)
    }

    suspend fun snapToItem(item: CircularItem) {
        val nextRotation = getRotationValue(item.angle)
        rotationAnimation.snapTo(nextRotation)
    }

}

@Composable
fun CircleWheel(
    modifier: Modifier = Modifier,
    state: CircleWheelState,
    content: @Composable () -> Unit,
) {
    state.apply {
        BoxWithConstraints(
            modifier = modifier
                .size(300.dp)
                .clip(CircleShape)
                .background(Color.Gray.copy(0.1F))
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
                    var preItem: CircularItem? = null
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
                                        if (vx.absoluteValue > 100) {
                                            scope.launch {
                                                slowingDown.value = true
                                                rotationAnimation.animateDecay(v.x, decay)
                                                slowingDown.value = false
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
                                    // 添加到手势加速度
                                    velocityTracker.addPosition(
                                        change.uptimeMillis,
                                        Offset(nextAngle, 0F),
                                    )
                                }
                            )
                        }
                ) {
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
                            .background(MaterialTheme.colorScheme.onBackground)
                    )

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
                                    .div(2)
                                    .toDp()
                            )
                            .width(2.dp)
                            .background(Color.Red)
                            .align(Alignment.TopCenter)
                    )
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .background(Color.White)
                ) {
                    val textColor = if (rotationRunning.value) Color.Red else Color.Black
                    Text(text = "${currentItem.value?.value}", color = textColor)
                }
            }
        }
    }
}

data class CircularItem(
    val angle: Float,
    val label: String?,
    val primary: Boolean,
    val value: Int?,
)

@Composable
fun CircularScale(
    items: List<CircularItem>,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1F)
            .background(Color.Gray.copy(0.1F))
    ) {
        LocalDensity.current.apply {
            BoxWithConstraints(
                modifier = Modifier.fillMaxSize()
            ) {
                val maxWidthPx = maxWidth.toPx()
                val maxHeightPx = maxHeight.toPx()
                val centerX = maxWidthPx.div(2)
                val centerY = maxHeightPx.div(2)
                val tickLength = 40f
                val strokeWidth = 4f
                val innerRadius = centerX - tickLength
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
                            val start = calculatePoint(centerX, centerY, centerX, angle)
                            val end = calculatePoint(centerX, centerY, innerRadius, angle)
                            val lineColor = if (index == 0) Color.Red
                            else if (circularItem.primary) Color.Black
                            else Color.Gray
                            drawLine(
                                lineColor,
                                Offset(start.x, start.y),
                                Offset(end.x, end.y),
                                strokeWidth
                            )

                            if (label != null) {
                                val textStyle = TextStyle(fontSize = 16.sp)
                                val textLayoutResult =
                                    textMeasurer.measure(AnnotatedString(label), textStyle)
                                val textOffset = textLayoutResult.size.run {
                                    Offset(
                                        x = centerX - width.div(2),
                                        y = centerY - height.div(2),
                                    )
                                }
                                val start01 =
                                    calculatePoint(centerX, centerY, centerX.times(0.8F), angle)
                                drawLine(
                                    lineColor,
                                    Offset(start01.x, start01.y),
                                    Offset(centerX, centerY),
                                    strokeWidth
                                )
                                translate(
                                    left = start01.x - centerX,
                                    top = start01.y - centerY,
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