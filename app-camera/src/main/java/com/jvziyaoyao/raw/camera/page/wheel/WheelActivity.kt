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
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
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
import kotlin.math.pow
import kotlin.math.sqrt

class WheelActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setBasicContent {
            WheelBody()
        }
    }

}

fun getWheelItems01(): List<CircularItem> {
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
    return items
}

fun getWheelItems02(): List<CircularItem> {
    val items = mutableListOf<CircularItem>()
    val numTicks = 6
    val anglePerTick = 180f / numTicks
    for (i in 0..numTicks) {
        val angle = i * anglePerTick
        items.add(
            CircularItem(
                angle = angle,
                label = "$angle",
                primary = true,
                value = angle.toInt(),
            )
        )
        if (i == numTicks) break
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
    return items
}

fun getWheelItems03(): List<CircularItem> {
    val items = mutableListOf<CircularItem>()
    val numTicks = 12
    val anglePerTick = 120f / numTicks
    for (i in 0..numTicks) {
        val angle = i * anglePerTick
        items.add(
            CircularItem(
                angle = angle,
                label = "${angle.toInt()}",
                primary = true,
                value = angle.toInt(),
            )
        )
        if (i == numTicks) break
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
    return items
}


@Composable
fun WheelBody() {
    LocalDensity.current.apply {
        val debugMode = true
        Box(modifier = Modifier.fillMaxSize()) {

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .align(Alignment.BottomCenter)
            ) {
                HalfWheelWrap(debugMode)
            }

            Box(
                modifier = Modifier
                    .align(Alignment.Center)
            ) {
                CircleWheelWrap(debugMode)
            }
        }
    }
}

@Composable
fun HalfWheelWrap(debugMode: Boolean) {
    val defaultItem = remember { mutableStateOf<CircularItem?>(null) }
    val items = remember {
        getWheelItems03().apply {
            forEach {
                if (it.angle == 60F) defaultItem.value = it
            }
        }
    }
    val circleWheelState = remember {
        CircleWheelState(
            items = items,
            defaultItem = defaultItem.value,
        )
    }
    HalfCircleWheel(circleWheelState = circleWheelState, debugMode = debugMode)
}

@Composable
fun CircleWheelWrap(debugMode: Boolean) {
    val defaultItem = remember { mutableStateOf<CircularItem?>(null) }
    val items = remember {
        getWheelItems01().apply {
            forEach {
                if (it.angle == 60F) defaultItem.value = it
            }
        }
    }
    val circleWheelState = remember {
        CircleWheelState(
            items = items,
            useBound = false,
            defaultItem = defaultItem.value,
        )
    }
    CircleWheel(
        modifier = Modifier,
        state = circleWheelState,
        debugMode = debugMode,
        wheelBackground = Color.Gray.copy(0.2F),
    ) {
        CircularScale(circleWheelState.items, debugMode = debugMode)
    }
}