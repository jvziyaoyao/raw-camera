package com.jvziyaoyao.raw.camera.page.camera

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jvziyaoyao.camera.flow.holder.camera.zoomRatioRange
import com.jvziyaoyao.raw.camera.base.FadeAnimatedVisibility
import com.jvziyaoyao.raw.camera.base.ScaleAnimatedVisibility
import com.jvziyaoyao.raw.camera.page.wheel.CircleWheelState
import com.jvziyaoyao.raw.camera.page.wheel.HalfCircleWheel
import com.jvziyaoyao.raw.camera.page.wheel.findCircularItemByValue
import com.jvziyaoyao.raw.camera.ui.theme.Layout
import com.jvziyaoyao.raw.camera.util.formatToDecimalPlaces
import org.koin.androidx.compose.koinViewModel

@Composable
fun CameraNormalLayer() {
    val viewModel: CameraViewModel = koinViewModel()
    val zoomRatio = viewModel.captureController.zoomRatioFlow.collectAsState()

    val currentCharacteristic =
        viewModel.currentCameraCharacteristicsFlow.collectAsState(initial = null)
    currentCharacteristic.value?.apply {
        Box(
            modifier = Modifier
                .fillMaxSize()
        ) {

            val actionBackgroundColor = MaterialTheme.colorScheme.background.copy(0.2F)
            val showZoomRatioWheel = remember { mutableStateOf(false) }

            FadeAnimatedVisibility(
                visible = !showZoomRatioWheel.value
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier
                            .padding(bottom = Layout.padding.pm)
                            .align(Alignment.BottomCenter),
                    ) {
                        CircleLabelText(
                            label = "${zoomRatio.value.formatToDecimalPlaces(1)}X",
                            onClick = {
                                showZoomRatioWheel.value = true
                            },
                        )
                    }
                }
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
                LaunchedEffect(showZoomRatioWheel.value) {
                    if (showZoomRatioWheel.value) {
                        val currentZoomRatio = zoomRatio.value
                        findCircularItemByValue(currentZoomRatio, items)?.let {
                            snapToItem(it)
                            currentItem.value = it
                        }
                    }
                }
                ScaleAnimatedVisibility(
                    visible = showZoomRatioWheel.value,
                ) {
                    BackHandler {
                        showZoomRatioWheel.value = false
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(Unit) {
                                detectTapGestures {
                                    showZoomRatioWheel.value = false
                                }
                            },
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        LaunchedEffect(currentItem.value) {
                            viewModel.captureController.zoomRatioFlow.value =
                                currentItem.value?.value?.value ?: 1F
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
}

@Composable
fun CircleLabelText(
    modifier: Modifier = Modifier,
    label: String,
    onClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.background.copy(0.36F))
            .clickable { onClick() }
    ) {
        Text(
            modifier = Modifier.align(Alignment.Center),
            text = label,
            fontSize = Layout.fontSize.fxs,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}