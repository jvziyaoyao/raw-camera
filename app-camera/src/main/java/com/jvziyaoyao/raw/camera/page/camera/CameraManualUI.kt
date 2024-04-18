package com.jvziyaoyao.raw.camera.page.camera

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CenterFocusWeak
import androidx.compose.material.icons.filled.SportsHandball
import androidx.compose.material.icons.filled.Texture
import androidx.compose.material.icons.filled.WbIncandescent
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.vector.ImageVector
import com.jvziyaoyao.camera.raw.holder.camera.focalDistanceRange
import com.jvziyaoyao.raw.camera.page.wheel.CircleWheelState
import com.jvziyaoyao.raw.camera.page.wheel.CircularItem
import com.jvziyaoyao.raw.camera.page.wheel.HalfCircleWheel
import com.jvziyaoyao.raw.camera.page.wheel.getWheelItems03
import com.jvziyaoyao.raw.camera.ui.theme.Layout
import org.koin.androidx.compose.koinViewModel

enum class ManualControlItem(
    val label: String,
    val icon: ImageVector,
) {
    ShutterSpeed(
        label = "快门速度",
        icon = Icons.Filled.SportsHandball,
    ),
    Sensitivity(
        label = "感光度",
        icon = Icons.Filled.Texture,
    ),
    FocalDistance(
        label = "对焦",
        icon = Icons.Filled.CenterFocusWeak,
    ),
    WhiteBalance(
        label = "白平衡",
        icon = Icons.Filled.WbIncandescent,
    ),
    ;
}

fun getFocalDistanceWheelItems(lower: Float, upper: Float): List<CircularItem<Float>> {
    val items = mutableListOf<CircularItem<Float>>()
    val fullAngle = 100F
    val numTicks = 10
    val anglePerTick = fullAngle / numTicks
    val allWheelsValue = upper - lower
    fun getWheelValue(angle: Double): Float {
        return (lower + allWheelsValue.times(1 - angle.div(fullAngle))).toFloat()
    }
    for (i in 0..numTicks) {
        val angle = i * anglePerTick
        items.add(
            CircularItem(
                angle = angle,
                label = if (i == 0) "自动" else if (i % 2 == 0) "${angle.toInt()}" else null,
                primary = true,
                value = if (i == 0) -1F else getWheelValue(angle.toDouble()),
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
                    value = if (e % 2 == 0) getWheelValue(innerAngle.toDouble()) else null,
                )
            )
        }
    }
    return items
}

@Composable
fun CameraManualLayer() {
    val viewModel: CameraViewModel = koinViewModel()
    Column(modifier = Modifier.fillMaxSize()) {
        Spacer(modifier = Modifier.weight(1F))
        val actionBackgroundColor = MaterialTheme.colorScheme.background.copy(0.2F)

        val currentCharacteristic =
            viewModel.currentCameraCharacteristicsFlow.collectAsState(initial = null)

        val selectedManualItem = remember { mutableStateOf<ManualControlItem?>(null) }

        currentCharacteristic.value?.apply {
            focalDistanceRange?.let {
                AnimatedVisibility(
                    visible = selectedManualItem.value == ManualControlItem.FocalDistance,
                    enter = scaleIn(
                        animationSpec = tween(200),
                        transformOrigin = TransformOrigin(0.5F, 1F)
                    ) + fadeIn(),
                    exit = scaleOut(
                        animationSpec = tween(200),
                        transformOrigin = TransformOrigin(0.5F, 1F)
                    ) + fadeOut(),
                ) {
                    val items = remember { getFocalDistanceWheelItems(it.lower, it.upper) }
                    val circleWheelState =
                        remember { CircleWheelState(items = items, defaultItem = items.first()) }
                    LaunchedEffect(circleWheelState.currentItem.value) {
                        val focalDistance = circleWheelState.currentItem.value?.value
                        viewModel.captureController.focalDistanceFlow.value =
                            if (focalDistance == -1F) null else focalDistance
                    }
                    HalfCircleWheel(
                        circleWheelState = circleWheelState,
                        indicatorColor = MaterialTheme.colorScheme.primary,
                        wheelBackground = actionBackgroundColor,
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(actionBackgroundColor)
        ) {
            @Composable
            fun RowButton(
                selected: Boolean,
                manualControlItem: ManualControlItem,
            ) {
                Column(
                    modifier = Modifier
                        .weight(1F)
                        .clickable {
                            selectedManualItem.value =
                                if (selectedManualItem.value == manualControlItem) null
                                else manualControlItem
                        }
                        .padding(vertical = Layout.padding.ps),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    val contentColor =
                        if (selected) MaterialTheme.colorScheme.primary else LocalContentColor.current
                    Icon(
                        imageVector = manualControlItem.icon,
                        contentDescription = null,
                        tint = contentColor
                    )
                    Spacer(modifier = Modifier.height(Layout.padding.pxxs))
                    Text(
                        text = manualControlItem.label,
                        fontSize = Layout.fontSize.fxxs,
                        color = contentColor
                    )
                }
            }

            ManualControlItem.entries.forEach { manualControlItem ->
                RowButton(
                    selected = selectedManualItem.value == manualControlItem,
                    manualControlItem = manualControlItem
                )
            }
        }
    }
}