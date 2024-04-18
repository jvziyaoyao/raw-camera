package com.jvziyaoyao.raw.camera.page.camera

import android.opengl.GLSurfaceView
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Surface
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.jvziyaoyao.camera.raw.holder.camera.cameraRequirePermissions
import com.jvziyaoyao.camera.raw.holder.camera.defaultSensorAspectRatio
import com.jvziyaoyao.camera.raw.holder.camera.sensorAspectRatio
import com.jvziyaoyao.raw.camera.base.BaseActivity
import com.jvziyaoyao.raw.camera.base.CommonPermissions
import com.jvziyaoyao.raw.camera.base.DynamicStatusBarColor
import com.jvziyaoyao.raw.camera.base.animateRotationAsState
import com.jvziyaoyao.raw.camera.ui.theme.Layout
import kotlinx.coroutines.delay
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
    CameraPopup()

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
                CameraFocusLayer()
                CameraFaceDetectLayer()

                val pictureMode = viewModel.pictureMode
                AnimatedVisibility(
                    visible = pictureMode.value == PictureMode.Manual,
                    enter = scaleIn(
                        animationSpec = tween(200),
                        transformOrigin = TransformOrigin(0.5F, 1F)
                    ) + fadeIn(),
                    exit = scaleOut(
                        animationSpec = tween(200),
                        transformOrigin = TransformOrigin(0.5F, 1F)
                    ) + fadeOut(),
                ) {
                    CameraManualLayer()
                }

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
            SideCircleWrap(
                onClick = {

                }
            ) {}
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
                color = MaterialTheme.colorScheme.onBackground.copy(0.4F),
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
                color = MaterialTheme.colorScheme.onBackground.copy(0.4F),
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
                    interactionSource = remember { MutableInteractionSource() },
                    indication = rememberRipple(color = MaterialTheme.colorScheme.primary)
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
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val itemCount = 5
        val itemEmptyCount = itemCount / 2
        val itemWidth = maxWidth.div(itemCount)
        val pictureMode = viewModel.pictureMode
        val lazyListState = rememberLazyListState(PictureMode.entries.indexOf(pictureMode.value))
        val centerIndex by remember {
            derivedStateOf {
                lazyListState.firstVisibleItemIndex
            }
        }
        LaunchedEffect(centerIndex, lazyListState.isScrollInProgress) {
            if (!lazyListState.isScrollInProgress) {
                pictureMode.value = PictureMode.entries[centerIndex]
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
                            Text(
                                text = outputMode.label,
                                color = LocalContentColor.current,
                                fontSize = Layout.fontSize.fs,
                            )
                            Spacer(modifier = Modifier.width(Layout.padding.pxxs))
                            Icon(
                                modifier = Modifier.size(12.dp),
                                imageVector = Icons.Filled.ArrowDropDown,
                                contentDescription = null
                            )
                        }
                        Text(
                            text = "${bestSize.width}x${bestSize.height}",
                            fontSize = Layout.fontSize.fxxs,
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
