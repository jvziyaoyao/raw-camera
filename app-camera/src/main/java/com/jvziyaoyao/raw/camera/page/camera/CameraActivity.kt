package com.jvziyaoyao.raw.camera.page.camera

import android.opengl.GLSurfaceView
import android.os.Build
import android.os.Bundle
import android.view.Surface
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.SnapLayoutInfoProvider
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.Button
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.jvziyaoyao.camera.raw.holder.camera.cameraRequirePermissions
import com.jvziyaoyao.camera.raw.holder.camera.defaultSensorAspectRatio
import com.jvziyaoyao.camera.raw.holder.camera.sensorAspectRatio
import com.jvziyaoyao.raw.camera.base.BaseActivity
import com.jvziyaoyao.raw.camera.base.CommonPermissions
import com.jvziyaoyao.raw.camera.base.DynamicStatusBarColor
import com.jvziyaoyao.raw.camera.base.animateRotationAsState
import com.jvziyaoyao.raw.camera.ui.theme.Layout
import com.jvziyaoyao.raw.camera.ui.theme.PrimaryDarkFull
import dev.chrisbanes.snapper.ExperimentalSnapperApi
import dev.chrisbanes.snapper.SnapOffsets
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

    @OptIn(ExperimentalSnapperApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mViewModel.setupSensor()
        mViewModel.setupCamera(displayRotation)

        setBasicContent {
            CompositionLocalProvider(
                LocalTextStyle provides LocalTextStyle.current.copy(color = MaterialTheme.colorScheme.surface),
                LocalContentColor provides MaterialTheme.colorScheme.surface,
            ) {
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
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.onBackground)
    ) {
        Spacer(modifier = Modifier.statusBarsPadding())
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Layout.padding.pxs)
        ) {
            IconButton(
                modifier = Modifier.align(Alignment.CenterEnd),
                onClick = {

                },
            ) {
                Icon(
                    imageVector = Icons.Filled.Menu,
                    contentDescription = null
                )
            }
        }
        CameraPreviewLayer(
            foreground = {
                CameraGridIndicator()
                CameraSeaLevelIndicator()
            }
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1F)
        ) {
            Spacer(modifier = Modifier.height(Layout.padding.ps))
            CameraPictureModeRow()

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1F)
            ) {
                val borderPadding = 6.dp
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .border(
                            width = borderPadding,
                            color = MaterialTheme.colorScheme.surface.copy(0.4F),
                            shape = CircleShape,
                        )
                        .padding(borderPadding)
                        .clip(CircleShape)
                        .background(color = MaterialTheme.colorScheme.surface)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = rememberRipple(color = MaterialTheme.colorScheme.primary)
                        ) {

                        }
                        .align(Alignment.Center)
                )
            }
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
                        color = if (centerIndex == index) PrimaryDarkFull else LocalContentColor.current,
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
fun CameraGridIndicator() {
    val density = LocalDensity.current
    val borderDarkWidth = density.run { 1F.toDp() }
    val borderDarkColor = MaterialTheme.colorScheme.onBackground.copy(0.28F)
    val borderLightWidth = density.run { 2F.toDp() }
    val borderLightColor = MaterialTheme.colorScheme.surface.copy(0.46F)
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
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .border(
                    width = 2.dp,
                    color = MaterialTheme.colorScheme.onBackground.copy(0.2F),
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
                .width(100.dp)
                .height(4.dp)
                .clip(CircleShape)
                .background(color = MaterialTheme.colorScheme.primary)
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
        Box(
            modifier = Modifier
                .height(80.dp)
                .rotate(saveImageOrientation.value.toFloat())
                .align(Alignment.Center)
        ) {
            Box(
                modifier = Modifier
                    .graphicsLayer {}
                    .width(4.dp)
                    .height(24.dp)
                    .clip(CircleShape)
                    .background(color = MaterialTheme.colorScheme.error)
                    .align(Alignment.TopCenter)
            )
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
                    .size(60.dp)
                    .clip(CircleShape)
                    .background(color = MaterialTheme.colorScheme.onBackground.copy(0.2F))
                    .align(Alignment.Center)
            )
        }
    }
}
