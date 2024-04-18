package com.jvziyaoyao.raw.camera.page.camera

import android.util.Log
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
    val fullAngle = 120F
    val numTicks = 12
    val anglePerTick = fullAngle / numTicks
    val allWheelsValue = upper - lower
    fun getWheelValue(angle: Double): Float {
        return (lower + allWheelsValue.times(angle.div(fullAngle))).toFloat()
    }
    for (i in 0..numTicks) {
        val angle = i * anglePerTick
        items.add(
            CircularItem(
                angle = angle,
                label = "${angle.toInt()}",
                primary = true,
                value = getWheelValue(angle.toDouble()),
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

        val currentCharacteristic =
            viewModel.currentCameraCharacteristicsFlow.collectAsState(initial = null)

        currentCharacteristic.value?.apply {
            focalDistanceRange?.let {
                val items = remember { getFocalDistanceWheelItems(it.lower, it.upper) }
                val circleWheelState = remember { CircleWheelState(items = items) }
                LaunchedEffect(circleWheelState.currentItem.value) {
                    Log.i(
                        "TAG",
                        "CameraManualLayer: circleWheelState ${circleWheelState.currentItem.value}"
                    )
                    viewModel.captureController.focalDistanceFlow.value = circleWheelState.currentItem.value?.value?.toFloat()
                }
                HalfCircleWheel(circleWheelState = circleWheelState)
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background.copy(0.2F))
        ) {
            val selectedManualItem = remember { mutableStateOf<ManualControlItem?>(null) }

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