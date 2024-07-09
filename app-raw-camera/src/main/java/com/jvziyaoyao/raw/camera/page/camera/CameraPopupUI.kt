package com.jvziyaoyao.raw.camera.page.camera

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.AreaChart
import androidx.compose.material.icons.filled.DataSaverOn
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Texture
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.jvziyaoyao.camera.flow.holder.camera.outputSupportedMode
import com.jvziyaoyao.raw.camera.base.FlatActionSheet
import com.jvziyaoyao.raw.camera.base.LocalPopupState
import com.jvziyaoyao.raw.camera.ui.theme.Layout
import org.koin.androidx.compose.koinViewModel

@Composable
fun CameraPopup() {
    val popupState = LocalPopupState.current
    popupState.popupMap["camera_body"] = {
        CameraSettingActionSheet()
    }
}

@Composable
fun CameraSettingActionSheet() {
    val viewModel: CameraViewModel = koinViewModel()
    val showCameraSetting = viewModel.showCameraSetting
    FlatActionSheet(
        showDialog = showCameraSetting.value, onDismissRequest = {
            showCameraSetting.value = false
        }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.44F)
                .clip(Layout.roundShape.rl)
                .background(MaterialTheme.colorScheme.background)
                .padding(
                    start = Layout.padding.pl,
                    end = Layout.padding.pl,
                    top = Layout.padding.pl,
                )
        ) {
            val buttonBackground = MaterialTheme.colorScheme.surface
            val selectedContentColor = MaterialTheme.colorScheme.onPrimary
            val labelColor = LocalContentColor.current.copy(0.6F)
            val labelFontSize = Layout.fontSize.fxs
            val cameraCharacteristics = viewModel.currentCameraCharacteristicsFlow
                .collectAsState(initial = null)
            cameraCharacteristics.value?.apply {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                ) {
                    Spacer(modifier = Modifier.height(Layout.padding.pl))

                    Text(
                        text = "拍摄格式",
                        fontSize = labelFontSize,
                        color = labelColor,
                    )
                    Spacer(modifier = Modifier.height(Layout.padding.ps))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(Layout.roundShape.rm)
                            .background(buttonBackground)
                            .padding(Layout.padding.pxs),
                        horizontalArrangement = Arrangement.spacedBy(Layout.padding.pm)
                    ) {
                        val currentOutputItem = viewModel.currentOutputItemFlow.collectAsState()
                        outputSupportedMode.forEach { outputItem ->
                            val isCurrentItem =
                                outputItem.outputMode == currentOutputItem.value?.outputMode
                            Box(
                                modifier = Modifier
                                    .weight(1F)
                                    .clip(RoundedCornerShape(8.8.dp))
                                    .run {
                                        if (isCurrentItem) {
                                            background(MaterialTheme.colorScheme.primary)
                                        } else this
                                    }
                                    .clickable {
                                        viewModel.currentOutputItemFlow.value = outputItem
                                    }
                                    .padding(vertical = Layout.padding.ps)
                            ) {
                                val contentColor =
                                    if (isCurrentItem) selectedContentColor else LocalContentColor.current
                                Column(modifier = Modifier.align(Alignment.Center)) {
                                    Text(
                                        text = outputItem.outputMode.label,
                                        fontSize = Layout.fontSize.fs,
                                        color = contentColor
                                    )
                                    Text(
                                        text = "${outputItem.bestSize.width}x${outputItem.bestSize.height}",
                                        fontSize = Layout.fontSize.fxs,
                                        color = contentColor
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(Layout.padding.pxl))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(Layout.padding.pm)
                    ) {
                        @Composable
                        fun RowItem(
                            label: String,
                            icon: ImageVector,
                            selected: Boolean,
                            onClick: () -> Unit,
                        ) {
                            Column(
                                modifier = Modifier
                                    .weight(1F),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .aspectRatio(1F)
                                        .clip(Layout.roundShape.rm)
                                        .background(
                                            if (selected) MaterialTheme.colorScheme.primary else buttonBackground
                                        )
                                        .clickable(onClick = onClick)
                                ) {
                                    Icon(
                                        modifier = Modifier.align(Alignment.Center),
                                        imageVector = icon,
                                        contentDescription = null,
                                        tint = if (selected) selectedContentColor else LocalContentColor.current,
                                    )
                                }
                                Spacer(modifier = Modifier.height(Layout.padding.ps))
                                Text(
                                    text = label,
                                    fontSize = labelFontSize,
                                    color = labelColor
                                )
                            }
                        }

                        val gridEnable = viewModel.gridEnable
                        RowItem(
                            label = "网格",
                            icon = Icons.Filled.GridOn,
                            selected = gridEnable.value,
                            onClick = {
                                gridEnable.value = !gridEnable.value
                            }
                        )

                        val levelIndicatorEnable = viewModel.levelIndicatorEnable
                        RowItem(
                            label = "水平仪",
                            icon = Icons.Filled.DataSaverOn,
                            selected = levelIndicatorEnable.value,
                            onClick = {
                                levelIndicatorEnable.value = !levelIndicatorEnable.value
                            }
                        )

                        val exposureHistogramEnable =
                            viewModel.exposureHistogramEnableFlow.collectAsState()
                        RowItem(
                            label = "直方图",
                            icon = Icons.Filled.AreaChart,
                            selected = exposureHistogramEnable.value,
                            onClick = {
                                viewModel.exposureHistogramEnableFlow.value =
                                    !exposureHistogramEnable.value
                            }
                        )

                        val focusPeakingEnable =
                            viewModel.focusPeakingEnableFlow.collectAsState()
                        RowItem(
                            label = "峰值对焦",
                            icon = Icons.Filled.AcUnit,
                            selected = focusPeakingEnable.value,
                            onClick = {
                                viewModel.focusPeakingEnableFlow.value =
                                    !focusPeakingEnable.value
                            }
                        )

                        val brightnessPeakingEnable =
                            viewModel.brightnessPeakingEnableFlow.collectAsState()
                        RowItem(
                            label = "峰值亮度",
                            icon = Icons.Filled.Texture,
                            selected = brightnessPeakingEnable.value,
                            onClick = {
                                viewModel.brightnessPeakingEnableFlow.value =
                                    !brightnessPeakingEnable.value
                            }
                        )
                    }
                }
            }
        }
    }
}