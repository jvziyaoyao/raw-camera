package com.jvziyaoyao.raw.camera.page.camera

import android.opengl.GLSurfaceView
import android.os.Build
import android.os.Bundle
import android.view.Surface
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.AreaChart
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.DataSaverOn
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Texture
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.jvziyaoyao.camera.raw.holder.camera.cameraRequirePermissions
import com.jvziyaoyao.camera.raw.holder.camera.defaultSensorAspectRatio
import com.jvziyaoyao.camera.raw.holder.camera.outputSupportedMode
import com.jvziyaoyao.camera.raw.holder.camera.sensorAspectRatio
import com.jvziyaoyao.raw.camera.base.BaseActivity
import com.jvziyaoyao.raw.camera.base.CommonPermissions
import com.jvziyaoyao.raw.camera.base.DynamicStatusBarColor
import com.jvziyaoyao.raw.camera.base.FlatActionSheet
import com.jvziyaoyao.raw.camera.base.LocalPopupState
import com.jvziyaoyao.raw.camera.base.animateRotationAsState
import com.jvziyaoyao.raw.camera.ui.theme.Layout
import dev.chrisbanes.snapper.ExperimentalSnapperApi
import dev.chrisbanes.snapper.rememberSnapperFlingBehavior
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel
import kotlin.math.absoluteValue

class CameraActivity : BaseActivity() {

    private val mViewModel by viewModel<CameraViewModel>()

    private val displayRotation: Int
        get() = when (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            display
        } else {
            windowManager.defaultDisplay
        }?.rotation) {
            Surface.ROTATION_90 -> 90
            Surface.ROTATION_180 -> 180
            Surface.ROTATION_270 -> 270
            else -> 0
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mViewModel.setupSensor()
        mViewModel.setupCamera(displayRotation)

        setBasicContent {
            DynamicStatusBarColor(dark = false)
            CommonPermissions(
                permissions = cameraRequirePermissions,
                onPermissionChange = {
                    mViewModel.onPermissionChanged(it)
                }
            ) {
                CameraBody()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        mViewModel.startSensor()
        mViewModel.resumeCamera()
    }

    override fun onPause() {
        super.onPause()
        mViewModel.stopSensor()
        mViewModel.pauseCamera()
    }

}

@Composable
fun CameraBody() {
    val viewModel: CameraViewModel = koinViewModel()
    val popupState = LocalPopupState.current
    popupState.popupMap["camera_body"] = {
        CameraSettingActionSheet()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Spacer(modifier = Modifier.statusBarsPadding())
        CameraActionHeader()

        CameraPreviewLayer(
            foreground = {
                if (viewModel.gridEnable.value) CameraGridIndicator()
                CameraCaptureInfoLayer()
                if (viewModel.levelIndicatorEnable.value) CameraSeaLevelIndicator()
            }
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1F)
        ) {
            CameraActionFooter()
        }

        Spacer(modifier = Modifier.navigationBarsPadding())
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
                .fillMaxHeight(0.68F)
                .clip(Layout.roundShape.rl)
                .background(MaterialTheme.colorScheme.background)
                .padding(
                    start = Layout.padding.pl,
                    end = Layout.padding.pl,
                    top = Layout.padding.pl,
                )
        ) {
            val buttonBackground = MaterialTheme.colorScheme.onBackground.copy(0.2F)
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
                                        color = contentColor.copy(0.6F)
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

@Composable
fun CameraActionHeader() {
    val viewModel: CameraViewModel = koinViewModel()
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(Layout.padding.pxs)
    ) {
        IconButton(
            modifier = Modifier.align(Alignment.CenterEnd),
            onClick = {
                viewModel.showSetting()
            },
        ) {
            Icon(
                imageVector = Icons.Filled.Menu,
                contentDescription = null
            )
        }
    }
}

@Composable
fun CameraActionFooter() {
    val scope = rememberCoroutineScope()
    val viewModel: CameraViewModel = koinViewModel()
    Column(
        modifier = Modifier
            .fillMaxSize()
    ) {
        Spacer(modifier = Modifier.height(Layout.padding.ps))
        CameraPictureModeRow()

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1F)
                .padding(horizontal = Layout.padding.pxl),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceAround,
        ) {
            @Composable
            fun SideCircleWrap(
                onClick: () -> Unit,
                content: @Composable () -> Unit,
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.onBackground.copy(0.2F))
                        .clickable(
                            onClick = onClick,
                        )
                ) {
                    content()
                }
            }
            SideCircleWrap(
                onClick = {
                    viewModel.switchCamera()
                }
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Icon(
                        modifier = Modifier
                            .fillMaxSize(0.6F)
                            .align(Alignment.Center),
                        imageVector = Icons.Outlined.Refresh, contentDescription = null)
                }
            }
            Box(
                modifier = Modifier
                    .size(68.dp)
            ) {
                val borderPadding = 6.dp
                androidx.compose.animation.AnimatedVisibility(
                    visible = viewModel.captureLoading.value,
                    enter = fadeIn(),
                    exit = fadeOut(),
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .fillMaxSize()
                            .align(Alignment.Center),
                        strokeWidth = borderPadding,
                        color = MaterialTheme.colorScheme.onBackground.copy(0.4F),
                    )
                }
                androidx.compose.animation.AnimatedVisibility(
                    visible = !viewModel.captureLoading.value,
                    enter = fadeIn(),
                    exit = fadeOut(),
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .fillMaxSize()
                            .align(Alignment.Center),
                        strokeWidth = borderPadding,
                        progress = 100F,
                        color = MaterialTheme.colorScheme.onBackground.copy(0.4F),
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(borderPadding)
                        .clip(CircleShape)
                        .background(color = MaterialTheme.colorScheme.onBackground)
                        .clickable(
                            enabled = !viewModel.captureLoading.value,
                            interactionSource = remember { MutableInteractionSource() },
                            indication = rememberRipple(color = MaterialTheme.colorScheme.primary)
                        ) {
                            scope.launch {
                                viewModel.captureLoading.value = true
                                try {
                                    viewModel.capture()
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                } finally {
                                    viewModel.captureLoading.value = false
                                }
                            }
                        }
                )
            }
            SideCircleWrap(
                onClick = {

                }
            ) {}
        }
    }
}

enum class PictureMode(
    val label: String,
) {
    Normal(
        label = "拍照",
    ),
    Manual(
        label = "手动",
    ),
    ;
}

@OptIn(ExperimentalSnapperApi::class)
@Composable
fun CameraPictureModeRow() {
    val scope = rememberCoroutineScope()
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val itemCount = 5
        val itemEmptyCount = itemCount / 2
        val itemWidth = maxWidth.div(itemCount)
        val lazyListState = rememberLazyListState()
        val centerIndex by remember {
            derivedStateOf {
                lazyListState.firstVisibleItemIndex
            }
        }
        LazyRow(
            state = lazyListState,
            flingBehavior = rememberSnapperFlingBehavior(lazyListState),
        ) {
            items(itemEmptyCount) {
                Spacer(modifier = Modifier.width(itemWidth))
            }
            PictureMode.entries.forEachIndexed { index, pictureMode ->
                item {
                    Text(
                        modifier = Modifier
                            .width(itemWidth)
                            .padding(horizontal = Layout.padding.pxs)
                            .clip(Layout.roundShape.rs)
                            .clickable {
                                scope.launch {
                                    lazyListState.animateScrollToItem(index)
                                }
                            }
                            .padding(vertical = Layout.padding.ps)
                            .align(Alignment.Center),
                        text = pictureMode.label,
                        textAlign = TextAlign.Center,
                        color = if (centerIndex == index) MaterialTheme.colorScheme.primary else LocalContentColor.current,
                    )
                }
            }
            items(itemEmptyCount) {
                Spacer(modifier = Modifier.width(itemWidth))
            }
        }
    }
}

@Composable
fun CameraPreviewLayer(
    foreground: @Composable () -> Unit,
) {
    val viewModel: CameraViewModel = koinViewModel()
    val currentCameraPair = viewModel.currentCameraPairFlow.collectAsState()
    val cameraCharacteristics = currentCameraPair.value?.second
    val imageAspectRatio =
        cameraCharacteristics?.sensorAspectRatio ?: defaultSensorAspectRatio
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1.div(imageAspectRatio)),
    ) {
        AndroidView(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { clip = true },
            factory = { ctx ->
                GLSurfaceView(ctx).apply {
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    viewModel.setSurfaceView(this)
                }
            }
        )
        foreground()
    }
}

@Composable
fun CameraCaptureInfoLayer() {
    val viewModel: CameraViewModel = koinViewModel()
    val currentOutputItem = viewModel.currentOutputItemFlow.collectAsState()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(Layout.padding.ps)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
        ) {
            val backgroundColor = MaterialTheme.colorScheme.onBackground.copy(0.4F)
            val lineColor = MaterialTheme.colorScheme.background.copy(0.6F)

            CompositionLocalProvider(LocalContentColor provides lineColor) {
                currentOutputItem.value?.apply {
                    Column(
                        modifier = Modifier
                            .clip(Layout.roundShape.rs)
                            .background(backgroundColor)
                            .clickable {
                                viewModel.showSetting()
                            }
                            .padding(
                                horizontal = Layout.padding.ps,
                                vertical = Layout.padding.pxs,
                            )
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(text = outputMode.label, color = LocalContentColor.current)
                            Spacer(modifier = Modifier.width(Layout.padding.pxxs))
                            Icon(
                                modifier = Modifier.size(12.dp),
                                imageVector = Icons.Filled.ArrowDropDown,
                                contentDescription = null
                            )
                        }
                        Spacer(modifier = Modifier.height(Layout.padding.pxxs))
                        Text(
                            text = "${bestSize.width}x${bestSize.height}",
                            fontSize = Layout.fontSize.fxs,
                            color = LocalContentColor.current.copy(0.6F),
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.weight(1F))

            val exposureHistogramData = viewModel.exposureHistogramDataFlow.collectAsState()
            if (exposureHistogramData.value != null) {
                Canvas(
                    modifier = Modifier
                        .clip(Layout.roundShape.rs)
                        .background(backgroundColor)
                        .padding(Layout.padding.ps)
                        .width(100.dp)
                        .height(50.dp),
                    onDraw = {
                        val histData = exposureHistogramData.value!!
                        val histSize = histData.size
                        val maxValue = histData.maxOrNull() ?: 1.0f // 避免除以零
                        val scaleY = size.height / maxValue
                        val binWidth = size.width / histSize
                        var prevX = 0f
                        var prevY = size.height
                        for (i in 0 until histSize) {
                            val x = i * binWidth
                            val y = size.height - histData[i] * scaleY
                            drawLine(
                                color = lineColor,
                                strokeWidth = 2F,
                                start = Offset(prevX, prevY),
                                end = Offset(x, y)
                            )
                            prevX = x
                            prevY = y
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun CameraGridIndicator() {
    val density = LocalDensity.current
    val borderDarkWidth = density.run { 1F.toDp() }
    val borderDarkColor = Color.Black.copy(0.28F)
    val borderLightWidth = density.run { 2F.toDp() }
    val borderLightColor = Color.White.copy(0.46F)
    Box(modifier = Modifier.fillMaxSize()) {
        Row(modifier = Modifier.fillMaxSize()) {
            @Composable
            fun BorderVertical() {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(borderDarkWidth)
                        .background(borderDarkColor)
                )
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(borderLightWidth)
                        .background(borderLightColor)
                )
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(borderDarkWidth)
                        .background(borderDarkColor)
                )
            }
            Spacer(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(1F)
            )
            BorderVertical()
            Spacer(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(1F)
            )
            BorderVertical()
            Spacer(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(1F)
            )
        }
        Column(modifier = Modifier.fillMaxSize()) {
            @Composable
            fun BorderHorizontal() {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(borderDarkWidth)
                        .background(borderDarkColor)
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(borderLightWidth)
                        .background(borderLightColor)
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(borderDarkWidth)
                        .background(borderDarkColor)
                )
            }
            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1F)
            )
            BorderHorizontal()
            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1F)
            )
            BorderHorizontal()
            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1F)
            )
        }
    }
}

@Composable
fun CameraSeaLevelIndicator() {
    val density = LocalDensity.current
    val viewModel: CameraViewModel = koinViewModel()
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        val circleSize = 72.dp
        val borderWidth = 1.2.dp
        val downColor = Color.White.copy(0.2F)
        Box(
            modifier = Modifier
                .size(circleSize)
                .clip(CircleShape)
                .border(
                    width = borderWidth,
                    color = downColor,
                    shape = CircleShape,
                )
                .align(Alignment.Center),
        )

        val displayRotation = viewModel.displayRotation
        val gravity = viewModel.gravityFlow.collectAsState()
        val gravityDegreesAnimation =
            animateRotationAsState(targetValue = gravity.value - displayRotation)
        val pitch = viewModel.pitchFlow.collectAsState()
        val roll = viewModel.rollFlow.collectAsState()
        val pitchAnimation = animateFloatAsState(targetValue = pitch.value)
        val rollAnimation = animateFloatAsState(targetValue = roll.value)
        val offsetPY = remember {
            derivedStateOf {
                var p = pitchAnimation.value.div(90F)
                if (p < -1) p = -1F
                if (p > 1) p = 1F
                p
            }
        }
        val offsetPX = remember {
            derivedStateOf {
                var p = -rollAnimation.value.div(90F)
                if (p < -1) p = -1F
                if (p > 1) p = 1F
                p
            }
        }
        val bubbleViewVisible = remember(offsetPX.value, offsetPY.value) {
            derivedStateOf {
                offsetPX.value.absoluteValue < 0.5 && offsetPY.value.absoluteValue < 0.5
            }
        }
        val bubbleAlphaAnimation =
            animateFloatAsState(targetValue = if (bubbleViewVisible.value) 1F else 0F)

        Box(
            modifier = Modifier
                .graphicsLayer {
                    rotationZ = gravityDegreesAnimation.value
                    alpha = 1 - bubbleAlphaAnimation.value
                }
                .width(circleSize.times(1.4F))
                .height(borderWidth)
                .clip(CircleShape)
                .background(color = Color.White.copy(0.8F))
                .align(Alignment.Center)
        )
        val saveImageOrientation = viewModel.saveImageOrientation
        LaunchedEffect(gravityDegreesAnimation.value) {
            val degree = gravityDegreesAnimation.value
            if (bubbleViewVisible.value) return@LaunchedEffect
            if (degree in 0F..30F || degree in 330F..360F) {
                if (saveImageOrientation.value != 0) saveImageOrientation.value = 0
            } else if (degree in 60F..120F) {
                if (saveImageOrientation.value != 90) saveImageOrientation.value = 90
            } else if (degree in 150F..210F) {
                if (saveImageOrientation.value != 180) saveImageOrientation.value = 180
            } else if (degree in 240F..300F) {
                if (saveImageOrientation.value != 270) saveImageOrientation.value = 270
            }
        }
        val saveImageOrientationAnimation = animateFloatAsState(
            targetValue = saveImageOrientation.value.toFloat(),
            animationSpec = tween(durationMillis = 600, easing = LinearEasing)
        )
        val crossLength = circleSize.times(0.48F)

        Box(
            modifier = Modifier
                .width(crossLength)
                .height(borderWidth)
                .background(downColor)
                .align(Alignment.Center)
        )
        Box(
            modifier = Modifier
                .height(crossLength)
                .width(borderWidth)
                .background(downColor)
                .align(Alignment.Center)
        )
        Box(
            modifier = Modifier
                .rotate(saveImageOrientationAnimation.value)
                .size(circleSize.times(1.4F))
                .align(Alignment.Center),
        ) {
            Box(
                modifier = Modifier
                    .width(circleSize.times(0.2F))
                    .height(borderWidth)
                    .background(downColor)
                    .align(Alignment.CenterStart)
            )
            Box(
                modifier = Modifier
                    .width(circleSize.times(0.2F))
                    .height(borderWidth)
                    .background(downColor)
                    .align(Alignment.CenterEnd)
            )
            Box(
                modifier = Modifier
                    .size(crossLength)
                    .align(Alignment.Center)
            ) {
                Box(
                    modifier = Modifier
                        .graphicsLayer {}
                        .width(borderWidth)
                        .height(crossLength.div(2))
                        .clip(CircleShape)
                        .background(color = MaterialTheme.colorScheme.error)
                        .align(Alignment.TopCenter)
                )
            }
        }

        BoxWithConstraints(
            modifier = Modifier
                .graphicsLayer {
                    rotationZ = -displayRotation.toFloat()
                }
                .fillMaxWidth()
                .aspectRatio(1F)
                .align(Alignment.Center)
        ) {
            val offsetX = remember(offsetPX.value) {
                derivedStateOf {
                    density.run { maxWidth.toPx().div(2).times(offsetPX.value) }
                }
            }
            val offsetY = remember(offsetPY.value) {
                derivedStateOf {
                    density.run { maxHeight.toPx().div(2).times(offsetPY.value) }
                }
            }
            Box(
                modifier = Modifier
                    .graphicsLayer {
                        translationX = offsetX.value
                        translationY = offsetY.value
                        alpha = bubbleAlphaAnimation.value
                    }
                    .size(circleSize)
                    .clip(CircleShape)
                    .border(
                        width = borderWidth,
                        color = Color.White.copy(0.48F),
                        shape = CircleShape
                    )
                    .align(Alignment.Center)
            )
        }
    }
}
