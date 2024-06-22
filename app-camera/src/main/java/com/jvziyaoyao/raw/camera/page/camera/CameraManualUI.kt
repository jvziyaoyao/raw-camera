package com.jvziyaoyao.raw.camera.page.camera

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CenterFocusWeak
import androidx.compose.material.icons.filled.ControlPoint
import androidx.compose.material.icons.filled.SportsHandball
import androidx.compose.material.icons.filled.Texture
import androidx.compose.material.icons.filled.WbIncandescent
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import com.jvziyaoyao.camera.raw.holder.camera.focalDistanceRange
import com.jvziyaoyao.camera.raw.holder.camera.zoomRatioRange
import com.jvziyaoyao.raw.camera.base.ScaleAnimatedVisibility
import com.jvziyaoyao.raw.camera.page.wheel.CircleWheelState
import com.jvziyaoyao.raw.camera.page.wheel.CircularItem
import com.jvziyaoyao.raw.camera.page.wheel.HalfCircleWheel
import com.jvziyaoyao.raw.camera.page.wheel.ItemValue
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
    Temperature(
        label = "白平衡",
        icon = Icons.Filled.WbIncandescent,
    ),
    ZoomRatio(
        label = "变焦",
        icon = Icons.Filled.ControlPoint,
    ),
    ;
}

fun getZoomRatioWheelItems(lower: Float, upper: Float): List<CircularItem<ItemValue<Float?>>> {
    val items = mutableListOf<CircularItem<ItemValue<Float?>>>()
    val fullAngle = 100F
    val numTicks = 10
    val anglePerTick = fullAngle / numTicks
    val innerTicks = 6
    val innerPerTick = anglePerTick / innerTicks
    val allWheelsValue = upper - lower
    fun getWheelValue(angle: Double): Float {
        return (lower + allWheelsValue.times((angle - anglePerTick).div(fullAngle))).toFloat()
    }
    for (i in 1..numTicks) {
        val angle = i * anglePerTick
        items.add(
            CircularItem(
                angle = angle,
                label = if (i % 2 == 0 || i == 1) "${angle.div(10).toInt()}x" else null,
                primary = true,
                value = ItemValue(getWheelValue(angle.toDouble())),
            )
        )
        if (i == numTicks) break
        for (e in 1 until innerTicks) {
            val innerAngle = angle + e * innerPerTick
            items.add(
                CircularItem(
                    angle = innerAngle,
                    label = null,
                    primary = false,
                    value = if (e % 2 == 0) ItemValue(getWheelValue(innerAngle.toDouble())) else null,
                )
            )
        }
    }
    return items
}

fun getFocalDistanceWheelItems(lower: Float, upper: Float): List<CircularItem<ItemValue<Float?>>> {
    val items = mutableListOf<CircularItem<ItemValue<Float?>>>()
    val fullAngle = 100F
    val numTicks = 10
    val anglePerTick = fullAngle / numTicks
    val innerTicks = 6
    val innerPerTick = anglePerTick / innerTicks
    val allWheelsValue = upper - lower
    fun getWheelValue(angle: Double): Float {
        return (lower + allWheelsValue.times(1 - (angle - anglePerTick).div(fullAngle))).toFloat()
    }
    items.add(
        CircularItem(
            angle = 0F,
            label = "自动",
            primary = true,
            value = ItemValue(null),
        )
    )
    for (e in 1 until innerTicks) {
        val innerAngle = e * innerPerTick
        items.add(
            CircularItem(
                angle = innerAngle,
                label = null,
                primary = false,
                value = null,
            )
        )
    }
    for (i in 1..numTicks) {
        val angle = i * anglePerTick
        items.add(
            CircularItem(
                angle = angle,
                label = if (i == numTicks) "无限远" else if (i % 2 == 0) "${angle.toInt()}" else null,
                primary = true,
                value = ItemValue(getWheelValue(angle.toDouble())),
            )
        )
        if (i == numTicks) break
        for (e in 1 until innerTicks) {
            val innerAngle = angle + e * innerPerTick
            items.add(
                CircularItem(
                    angle = innerAngle,
                    label = null,
                    primary = false,
                    value = if (e % 2 == 0) ItemValue(getWheelValue(innerAngle.toDouble())) else null,
                )
            )
        }
    }
    return items
}

/**
 *
 * 2800
 * 2800 白炽灯
 * 4100 日光灯
 * 5000 晴天
 * 6500 阴天
 * 10000K
 *
 */
fun getTemperatureWheelItems(): List<CircularItem<ItemValue<Float?>>> {
    val items = mutableListOf<CircularItem<ItemValue<Float?>>>()
    val perAngle = 1.66666F
    var currentAngle = 0F
    fun addStepIndex() {
        currentAngle += perAngle
    }
    items.add(
        CircularItem(
            angle = currentAngle,
            label = "自动",
            primary = true,
            value = ItemValue(null),
        )
    )
    addStepIndex()
    for (i in 0..8) {
        items.add(
            CircularItem(
                angle = currentAngle,
                label = null,
                primary = false,
                value = null,
            )
        )
        addStepIndex()
    }
    for (i in 2800..10000 step 100) {
        val primary = i == 2800 || i == 4100
                || i == 5000 || i == 6500 || i == 10000
        val value = (i - 2800).toFloat().div(10000 - 2800).times(100)
        items.add(
            CircularItem(
                angle = currentAngle,
                label = if (primary) "$i" else null,
                primary = primary,
                value = ItemValue(value),
            )
        )
        addStepIndex()
    }

    return items
}

fun <T> getTypeWheelItems(
    list: List<T>,
    getLabel: (T) -> String,
): List<CircularItem<ItemValue<T?>>> {
    val perAngle = 1.66666F
    val items = mutableListOf<CircularItem<ItemValue<T?>>>()
    var currentAngle = 0F
    var borderIndex = 0
    fun addStepIndex() {
        currentAngle += perAngle
        borderIndex++
        if (borderIndex >= 6) {
            borderIndex = 0
        }
    }

    items.add(
        CircularItem(
            angle = currentAngle,
            label = "自动",
            primary = true,
            value = ItemValue(null),
        )
    )
    addStepIndex()
    for (i in 0..4) {
        items.add(
            CircularItem(
                angle = currentAngle,
                label = null,
                primary = false,
                value = null,
            )
        )
        addStepIndex()
    }

    var showLabel = false
    list.forEach { item ->
        val primary = borderIndex == 0
        var lable: String? = null
        if (primary) {
            if (showLabel) {
                lable = getLabel(item)
            }
            showLabel = !showLabel
        }

        items.add(
            CircularItem(
                angle = currentAngle,
                label = lable,
                primary = primary,
                value = ItemValue(item),
            )
        )
        addStepIndex()
        items.add(
            CircularItem(
                angle = currentAngle,
                label = null,
                primary = false,
                value = null,
            )
        )
        addStepIndex()
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
                Box(modifier = Modifier.fillMaxWidth()) {
                    val shutterWheelState = remember {
                        val items = getTypeWheelItems(ShutterSpeedItem.entries) { it.label }
                        CircleWheelState(
                            items = items,
                            defaultItem = items.first()
                        )
                    }
                    ScaleAnimatedVisibility(
                        visible = selectedManualItem.value == ManualControlItem.ShutterSpeed,
                    ) {
                        LaunchedEffect(shutterWheelState.currentItem.value) {
                            viewModel.captureController.sensorExposureTimeFlow.value =
                                shutterWheelState.currentItem.value?.value?.value?.time
                        }
                        HalfCircleWheel(
                            circleWheelState = shutterWheelState,
                            indicatorColor = MaterialTheme.colorScheme.primary,
                            wheelBackground = actionBackgroundColor,
                        )
                    }

                    val sensitivityWheelState = remember {
                        val items = getTypeWheelItems(ISO_LIST) { "$it" }
                        CircleWheelState(
                            items = items,
                            defaultItem = items.first()
                        )
                    }
                    ScaleAnimatedVisibility(
                        visible = selectedManualItem.value == ManualControlItem.Sensitivity,
                    ) {
                        LaunchedEffect(sensitivityWheelState.currentItem.value) {
                            viewModel.captureController.sensorSensitivityFlow.value =
                                sensitivityWheelState.currentItem.value?.value?.value
                        }
                        HalfCircleWheel(
                            circleWheelState = sensitivityWheelState,
                            indicatorColor = MaterialTheme.colorScheme.primary,
                            wheelBackground = actionBackgroundColor,
                        )
                    }

                    val focalWheelState = remember {
                        val items = getFocalDistanceWheelItems(it.lower, it.upper)
                        CircleWheelState(
                            items = items,
                            defaultItem = items.first()
                        )
                    }
                    ScaleAnimatedVisibility(
                        visible = selectedManualItem.value == ManualControlItem.FocalDistance,
                    ) {
                        LaunchedEffect(focalWheelState.currentItem.value) {
                            viewModel.captureController.focalDistanceFlow.value =
                                focalWheelState.currentItem.value?.value?.value
                        }
                        HalfCircleWheel(
                            circleWheelState = focalWheelState,
                            indicatorColor = MaterialTheme.colorScheme.primary,
                            wheelBackground = actionBackgroundColor,
                        )
                    }

                    val temperatureWheelState = remember {
                        val items = getTemperatureWheelItems()
                        CircleWheelState(
                            items = items,
                            defaultItem = items.first()
                        )
                    }
                    ScaleAnimatedVisibility(
                        visible = selectedManualItem.value == ManualControlItem.Temperature,
                    ) {
                        LaunchedEffect(temperatureWheelState.currentItem.value) {
                            viewModel.captureController.customTemperatureFlow.value =
                                temperatureWheelState.currentItem.value?.value?.value
                        }
                        HalfCircleWheel(
                            circleWheelState = temperatureWheelState,
                            indicatorColor = MaterialTheme.colorScheme.primary,
                            wheelBackground = actionBackgroundColor,
                        )
                    }

                    val zoomRatioWheelState = remember {
                        val zoomRatioRange = zoomRatioRange
                        if (zoomRatioRange != null) {
                            val items =
                                getZoomRatioWheelItems(zoomRatioRange.lower, zoomRatioRange.upper)
                            CircleWheelState(
                                items = items,
                                defaultItem = items.first()
                            )
                        } else null

                    }
                    zoomRatioWheelState?.apply {
                        ScaleAnimatedVisibility(
                            visible = selectedManualItem.value == ManualControlItem.ZoomRatio,
                        ) {
                            LaunchedEffect(currentItem.value) {
                                viewModel.captureController.zoomRatioFlow.value = currentItem.value?.value?.value ?: 1F
                            }
                            HalfCircleWheel(
                                circleWheelState = zoomRatioWheelState,
                                indicatorColor = MaterialTheme.colorScheme.primary,
                                wheelBackground = actionBackgroundColor,
                            )
                        }
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(actionBackgroundColor)
                .padding(horizontal = Layout.padding.pxs)
        ) {
            @Composable
            fun RowButton(
                selected: Boolean,
                manualControlItem: ManualControlItem,
            ) {
                Column(
                    modifier = Modifier
                        .weight(1F)
                        .padding(
                            horizontal = Layout.padding.pxs,
                            vertical = Layout.padding.pxs,
                        )
                        .clip(Layout.roundShape.rs)
                        .clickable {
                            selectedManualItem.value =
                                if (selectedManualItem.value == manualControlItem) null
                                else manualControlItem
                        }
                        .padding(vertical = Layout.padding.pxs),
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