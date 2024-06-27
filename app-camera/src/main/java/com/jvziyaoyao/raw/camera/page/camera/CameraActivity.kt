package com.jvziyaoyao.raw.camera.page.camera

import android.content.Intent
import android.opengl.GLSurfaceView
import android.os.Build
import android.os.Bundle
import android.view.Surface
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandIn
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.FlashAuto
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.outlined.Refresh
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.jvziyaoyao.camera.raw.holder.camera.FlashMode
import com.jvziyaoyao.camera.raw.holder.camera.OutputMode
import com.jvziyaoyao.camera.raw.holder.camera.cameraRequirePermissions
import com.jvziyaoyao.camera.raw.holder.camera.defaultSensorAspectRatio
import com.jvziyaoyao.camera.raw.holder.camera.render.isEmptyImageFilter
import com.jvziyaoyao.camera.raw.holder.camera.sensorAspectRatio
import com.jvziyaoyao.raw.camera.base.BaseActivity
import com.jvziyaoyao.raw.camera.base.CameraCommonPreviewer
import com.jvziyaoyao.raw.camera.base.CommonPermissions
import com.jvziyaoyao.raw.camera.base.DynamicStatusBarColor
import com.jvziyaoyao.raw.camera.base.FadeAnimatedVisibility
import com.jvziyaoyao.raw.camera.base.ScaleAnimatedVisibility
import com.jvziyaoyao.raw.camera.base.animateRotationAsState
import com.jvziyaoyao.raw.camera.base.rememberCoilImagePainter
import com.jvziyaoyao.raw.camera.page.image.ImageActivity
import com.jvziyaoyao.raw.camera.page.wheel.LocalVibratorHelper
import com.jvziyaoyao.raw.camera.page.wheel.VibratorHelper
import com.jvziyaoyao.raw.camera.ui.theme.Layout
import com.jvziyaoyao.scale.zoomable.previewer.PreviewerState
import com.jvziyaoyao.scale.zoomable.previewer.TransformItemView
import com.jvziyaoyao.scale.zoomable.previewer.VerticalDragType
import com.jvziyaoyao.scale.zoomable.previewer.rememberPreviewerState
import com.jvziyaoyao.scale.zoomable.previewer.rememberTransformItemState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
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
                val vibratorHelper = remember { VibratorHelper(this) }
                CompositionLocalProvider(LocalVibratorHelper provides vibratorHelper) {
                    CameraBody()
                }
            }
        }

        // 刷新图片列表
        launch {
            mViewModel.fetchImages()
        }
    }

    override fun onResume() {
        super.onResume()
        mViewModel.resume()
    }

    override fun onPause() {
        super.onPause()
        mViewModel.pause()
    }

}

@Composable
fun CameraBody() {
    val context = LocalContext.current
    val viewModel: CameraViewModel = koinViewModel()
    CameraPopup()

    val scope = rememberCoroutineScope()
    val images = viewModel.imageList
    val previewerState = rememberPreviewerState(
        verticalDragType = VerticalDragType.Down,
        pageCount = { images.size },
        getKey = { images[it].path!! },
    )
    LaunchedEffect(previewerState.visible) {
        viewModel.previewerVisibleFlow.value = previewerState.visible
    }
    LaunchedEffect(previewerState.visibleTarget) {
        viewModel.previewerVisibleTargetFlow.value = previewerState.visibleTarget
    }


    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Spacer(modifier = Modifier.statusBarsPadding())
        CameraActionHeader()

        val pictureMode = viewModel.pictureModeFlow.collectAsState()
        val showFilterList = viewModel.showFilterList

        CameraPreviewLayer(
            foreground = {
                if (viewModel.gridEnable.value) CameraGridIndicator()
                CameraFocusLayer()
                CameraFaceDetectLayer()

                ScaleAnimatedVisibility(
                    visible = pictureMode.value == PictureMode.Manual && !showFilterList.value,
                ) {
                    CameraManualLayer()
                }

                FadeAnimatedVisibility(
                    visible = pictureMode.value == PictureMode.Normal && !showFilterList.value,
                ) {
                    CameraNormalLayer()
                }

                ScaleAnimatedVisibility(
                    visible = showFilterList.value,
                ) {
                    CameraFilterLayer()
                }

                CameraCaptureInfoLayer()
                if (viewModel.levelIndicatorEnable.value) CameraSeaLevelIndicator()
            }
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1F)
        ) {
            Spacer(modifier = Modifier.height(Layout.padding.ps))

            if (showFilterList.value) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    CameraModeTextButton(
                        label = "滤镜",
                        selected = true
                    ) {
                        showFilterList.value = false
                    }
                }
            } else {
                CameraPictureModeRow()
            }

            CameraActionFooter(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1F),
                previewerState = previewerState
            )
        }

        Spacer(modifier = Modifier.navigationBarsPadding())
    }

    CameraCommonPreviewer(
        images = images,
        previewerState = previewerState,
        onDelete = {
            viewModel.deleteImage(it)
        },
        endOfNav = {
            Text(
                modifier = Modifier
                    .clip(Layout.roundShape.rs)
                    .clickable {
                        context.startActivity(Intent(context, ImageActivity::class.java))
                        MainScope().launch(Dispatchers.IO) {
                            delay(1000)
                            previewerState.close()
                        }
                    }
                    .padding(
                        horizontal = Layout.padding.pm,
                        vertical = Layout.padding.ps,
                    ),
                text = "全部图片",
                color = MaterialTheme.colorScheme.primary,
            )
        }
    )

}

enum class FlashLightMode(
    val flashMode: FlashMode,
    val icon: ImageVector,
) {
    OFF(
        flashMode = FlashMode.OFF,
        icon = Icons.Filled.FlashOff,
    ),
    AUTO(
        flashMode = FlashMode.AUTO,
        icon = Icons.Filled.FlashAuto,
    ),
    ON(
        flashMode = FlashMode.ON,
        icon = Icons.Filled.FlashOn,
    ),
    ALWAYS_ON(
        flashMode = FlashMode.ALWAYS_ON,
        icon = Icons.Filled.FlashlightOn,
    ),
    ;
}

val FlashMode.flashLightMode: FlashLightMode
    get() {
        for (entry in FlashLightMode.entries) {
            if (entry.flashMode == this) return entry
        }
        throw RuntimeException("闪光灯模式不合法！")
    }

@Composable
fun CameraActionHeader() {
    val viewModel: CameraViewModel = koinViewModel()
    Box(modifier = Modifier.fillMaxWidth()) {
        val showFlashOption = remember { mutableStateOf(false) }
        val flashMode = viewModel.captureController.flashModeFlow.collectAsState()
        AnimatedVisibility(
            visible = !showFlashOption.value,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Layout.padding.pxs)
            ) {
                Row(
                    modifier = Modifier.align(Alignment.CenterStart),
                    horizontalArrangement = Arrangement.spacedBy(Layout.padding.ps),
                ) {
                    IconButton(
                        onClick = {
                            showFlashOption.value = true
                        },
                    ) {
                        val flashLightMode = flashMode.value.flashLightMode
                        Icon(
                            imageVector = flashLightMode.icon,
                            tint = if (flashLightMode != FlashLightMode.OFF) MaterialTheme.colorScheme.primary
                            else LocalContentColor.current,
                            contentDescription = null
                        )
                    }

                    val currentOutputItem = viewModel.currentOutputItemFlow.collectAsState()
                    IconButton(
                        enabled = currentOutputItem.value?.outputMode == OutputMode.JPEG,
                        onClick = {
                            viewModel.showFilterList.value = !viewModel.showFilterList.value
                        },
                    ) {
                        val currentImageFilter = viewModel.currentImageFilterFlow.collectAsState()
                        Icon(
                            imageVector = Icons.Filled.AutoAwesome,
                            tint = if (currentImageFilter.value.isEmptyImageFilter()) LocalContentColor.current else MaterialTheme.colorScheme.primary,
                            contentDescription = null
                        )
                    }
                }

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

        val stiffness = 100F
        AnimatedVisibility(
            visible = showFlashOption.value,
            enter = expandIn { IntSize(0, it.height) } + fadeIn(),
            exit = shrinkOut(
                animationSpec = spring(stiffness = stiffness)
            ) { IntSize(0, it.height) } + fadeOut(
                animationSpec = spring(stiffness = stiffness)
            ),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        vertical = Layout.padding.pxs,
                        horizontal = Layout.padding.pxl,
                    )
                    .align(Alignment.Center),
                horizontalArrangement = Arrangement.SpaceAround,
            ) {
                FlashLightMode.entries.forEach { flashLightMode ->
                    IconButton(
                        onClick = {
                            viewModel.captureController.flashModeFlow.value =
                                flashLightMode.flashMode
                            showFlashOption.value = false
                        },
                    ) {
                        val color = if (flashMode.value == flashLightMode.flashMode)
                            MaterialTheme.colorScheme.primary else LocalContentColor.current
                        Icon(
                            tint = color,
                            imageVector = flashLightMode.icon,
                            contentDescription = null
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CameraActionFooter(
    modifier: Modifier = Modifier,
    previewerState: PreviewerState,
) {
    val scope = rememberCoroutineScope()
    val viewModel: CameraViewModel = koinViewModel()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Layout.padding.pxl),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceAround,
    ) {
        val sideCircleSize = 48.dp

        @Composable
        fun SideCircleWrap(
            onClick: () -> Unit,
            content: @Composable () -> Unit,
        ) {
            Box(
                modifier = Modifier
                    .size(sideCircleSize)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface)
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
                    imageVector = Icons.Outlined.Refresh, contentDescription = null
                )
            }
        }
        CameraCaptureButton(
            loading = viewModel.captureLoading.value,
            onClick = {
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

        val latestImage by remember {
            derivedStateOf {
                viewModel.imageList.run {
                    if (isNotEmpty()) first() else null
                }
            }
        }
        Box(modifier = Modifier.size(sideCircleSize)) {
            latestImage?.apply {
                val painter = rememberCoilImagePainter(image = path!!)
                FadeAnimatedVisibility(visible = painter.intrinsicSize.isSpecified) {
                    SideCircleWrap(
                        onClick = {
                            scope.launch {
                                previewerState.enterTransform(0)
                            }
                        }
                    ) {
                        val itemState =
                            rememberTransformItemState(intrinsicSize = painter.intrinsicSize)
                        TransformItemView(
                            key = path!!,
                            itemState = itemState,
                            transformState = previewerState,
                        ) {
                            Image(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(
                                        RoundedCornerShape(
                                            (1 - previewerState.decorationAlpha.value)
                                                .times(400).dp
                                        )
                                    ),
                                painter = painter,
                                contentScale = ContentScale.Crop,
                                contentDescription = null,
                            )
                        }
                    }
                }
            }
        }

    }
}

@Composable
fun CameraCaptureButton(
    loading: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(68.dp)
    ) {
        val borderPadding = 6.dp
        AnimatedVisibility(
            visible = loading,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            CircularProgressIndicator(
                modifier = Modifier
                    .fillMaxSize()
                    .align(Alignment.Center),
                strokeWidth = borderPadding,
                color = MaterialTheme.colorScheme.surface,
            )
        }
        AnimatedVisibility(
            visible = !loading,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            CircularProgressIndicator(
                progress = { 100F },
                modifier = Modifier
                    .fillMaxSize()
                    .align(Alignment.Center),
                color = MaterialTheme.colorScheme.surface,
                strokeWidth = borderPadding,
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(borderPadding)
                .clip(CircleShape)
                .background(color = MaterialTheme.colorScheme.onBackground)
                .clickable(
                    enabled = !loading,
                    // TODO 寻找其他解决方案
//                    interactionSource = remember { MutableInteractionSource() },
//                    indication = ripple(color = MaterialTheme.colorScheme.primary)
                ) {
                    onClick()
                }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CameraPictureModeRow() {
    val viewModel: CameraViewModel = koinViewModel()
    val scope = rememberCoroutineScope()
    val vibratorHelper = LocalVibratorHelper.current
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val itemCount = 5
        val itemEmptyCount = itemCount / 2
        val itemWidth = maxWidth.div(itemCount)
        val pictureMode = viewModel.pictureModeFlow
        val lazyListState = rememberLazyListState(PictureMode.entries.indexOf(pictureMode.value))
        val centerIndex by remember {
            derivedStateOf {
                lazyListState.firstVisibleItemIndex
            }
        }
        LaunchedEffect(centerIndex, lazyListState.isScrollInProgress) {
            if (!lazyListState.isScrollInProgress) {
                pictureMode.value = PictureMode.entries[centerIndex]
                vibratorHelper.playTickVibrate()
            }
        }
        LazyRow(
            state = lazyListState,
            flingBehavior = rememberSnapFlingBehavior(lazyListState),
        ) {
            items(itemEmptyCount) {
                Spacer(modifier = Modifier.width(itemWidth))
            }
            PictureMode.entries.forEachIndexed { index, pictureMode ->
                item {
                    CameraModeTextButton(
                        modifier = Modifier.width(itemWidth),
                        label = pictureMode.label,
                        selected = centerIndex == index,
                    ) {
                        scope.launch {
                            lazyListState.animateScrollToItem(index)
                        }
                    }
                }
            }
            items(itemEmptyCount) {
                Spacer(modifier = Modifier.width(itemWidth))
            }
        }
    }
}

@Composable
fun CameraModeTextButton(
    modifier: Modifier = Modifier,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Text(
        modifier = modifier
            .padding(horizontal = Layout.padding.pxs)
            .clip(Layout.roundShape.rs)
            .clickable { onClick() }
            .padding(vertical = Layout.padding.ps),
        text = label,
        textAlign = TextAlign.Center,
        color = if (selected) MaterialTheme.colorScheme.primary else LocalContentColor.current,
    )
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
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(Layout.padding.ps)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
        ) {
            val backgroundColor = MaterialTheme.colorScheme.onBackground.copy(0.4F)
            val lineColor = MaterialTheme.colorScheme.background.copy(0.6F)

            Spacer(modifier = Modifier.weight(1F))

            val exposureHistogramData = viewModel.exposureHistogramDataFlow.collectAsState()
            if (exposureHistogramData.value != null) {
                Canvas(
                    modifier = Modifier
                        .clip(Layout.roundShape.rs)
                        .background(backgroundColor)
                        .padding(Layout.padding.ps)
                        .width(80.dp)
                        .height(40.dp),
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

        val flashLight = viewModel.flashLightFlow.collectAsState(initial = false)
        AnimatedVisibility(
            modifier = Modifier.align(Alignment.TopCenter),
            visible = flashLight.value,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            Box(
                modifier = Modifier
                    .clip(Layout.roundShape.rs)
                    .background(MaterialTheme.colorScheme.primary)
                    .padding(
                        horizontal = Layout.padding.pm,
                        vertical = Layout.padding.pxxs,
                    ),
            ) {
                Icon(
                    modifier = Modifier.size(18.dp),
                    imageVector = Icons.Filled.FlashOn,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    contentDescription = null
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
