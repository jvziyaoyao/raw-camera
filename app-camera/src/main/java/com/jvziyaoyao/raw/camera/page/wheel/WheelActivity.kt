package com.jvziyaoyao.raw.camera.page.wheel

import android.os.Bundle
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import com.jvziyaoyao.raw.camera.base.BaseActivity

class WheelActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setBasicContent {
            WheelBody()
        }
    }

}

fun getWheelItems01(): List<CircularItem<Int>> {
    val items = mutableListOf<CircularItem<Int>>()
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

fun getWheelItems02(): List<CircularItem<Int>> {
    val items = mutableListOf<CircularItem<Int>>()
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

fun getWheelItems03(): List<CircularItem<Int>> {
    val items = mutableListOf<CircularItem<Int>>()
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
        Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
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
    val defaultItem = remember { mutableStateOf<CircularItem<Int>?>(null) }
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
    val defaultItem = remember { mutableStateOf<CircularItem<Int>?>(null) }
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
        )
    }
//    val items = remember { getFocalDistanceWheelItems(0F, 10F) }
//    val circleWheelState = remember {
//        CircleWheelState(items = items)
//    }
    CircleWheel(
        modifier = Modifier,
        state = circleWheelState,
        debugMode = debugMode,
        wheelBackground = Color.Gray.copy(0.2F),
    ) {
        CircularScale(circleWheelState.items, debugMode = debugMode)
    }
}