package com.jvziyaoyao.raw.camera.page.main

import android.hardware.camera2.CameraMetadata
import android.opengl.GLSurfaceView
import android.os.Build
import android.os.Bundle
import android.view.Surface
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.jvziyaoyao.raw.camera.ui.base.CommonPermissions
import com.jvziyaoyao.raw.camera.ui.base.animateRotationAsState
import com.jvziyaoyao.raw.camera.ui.theme.Layout
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel
import kotlin.math.absoluteValue

class CameraRawActivity : ComponentActivity(), CoroutineScope by MainScope() {

    private val mViewModel by viewModel<CameraRawViewModel>()

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

        setContent {
            CommonPermissions(
                permissions = camera2Permissions,
                onPermissionChange = {
                    mViewModel.allPermissionGrantedFlow.value = it
                }
            ) {
                CameraRawBody(
                    onGLSurfaceView = { glSurfaceView ->
                        mViewModel.setSurfaceView(glSurfaceView)
                    },
                    onCapture = {
                        launch {
                            mViewModel.captureLoading.value = true
                            try {
                                mViewModel.onCapture()
                            } catch (e: Exception) {
                                e.printStackTrace()
                            } finally {
                                mViewModel.captureLoading.value = false
                            }
                        }
                    }
                )
            }
        }

        // 监听传感器角度变化
        launch(Dispatchers.IO) {
            combine(
                mViewModel.pitchFlow,
                mViewModel.rollFlow,
                mViewModel.yawFlow,
            ) { t01, t02, t03 ->
                arrayOf(t01, t02, t03)
            }.collectLatest {
                val currentPitch = mViewModel.pitchFlow.value
                val currentRoll = mViewModel.rollFlow.value
                val currentYaw = mViewModel.yawFlow.value
                val srcOrientation = mViewModel.focusRequestOrientation.value
                srcOrientation?.apply {
                    val delta = 10F
                    val delay = 1000 * 2 // 两秒内不要取消
                    if (
                        (System.currentTimeMillis() - timestamp > delay)
                        && ((currentPitch - pitch).absoluteValue > delta
                                || (currentRoll - roll).absoluteValue > delta
                                || (currentYaw - yaw).absoluteValue > delta)
                    ) {
                        mViewModel.focusCancel()
                    }
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

    override fun onDestroy() {
        super.onDestroy()
        mViewModel.releaseCamera()
        cancel()
    }

}


@Composable
fun CameraRawBody(
    onGLSurfaceView: (GLSurfaceView) -> Unit,
    onCapture: () -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        // 预览图层
        CameraRawPreviewLayer(
            onGLSurfaceView = onGLSurfaceView,
        )
        // 信息图层
        CameraRawInfoLayer()
        // 操作图层
        CameraRawActionLayer(
            onCapture = onCapture,
        )
    }
}

@Composable
fun CameraRawPreviewLayer(
    onGLSurfaceView: (GLSurfaceView) -> Unit,
) {
    val viewModel: CameraRawViewModel = koinViewModel()
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val portrait = maxHeight > maxWidth
        val currentCameraPair = viewModel.currentCameraPairFlow.collectAsState()
        val cameraCharacteristics = currentCameraPair.value?.second
        Box(
            modifier = Modifier
                .run {
                    val imageAspectRatio =
                        cameraCharacteristics?.sensorAspectRatio ?: defaultSensorAspectRatio
                    if (portrait) {
                        fillMaxWidth()
                            .aspectRatio(1.div(imageAspectRatio))
                    } else {
                        fillMaxHeight()
                            .aspectRatio(imageAspectRatio)
                    }
                }
                .background(Color.Cyan.copy(0.2F))
                .align(if (portrait) Alignment.TopCenter else Alignment.CenterStart),
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
                        onGLSurfaceView(this)
                    }
                }
            )

            val previewSize = remember { mutableStateOf(IntSize.Zero) }
            val fingerClick = remember { mutableStateOf(Offset.Zero) }
            val fingerRect = remember { mutableStateOf(Rect.Zero) }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures {
                            fingerClick.value = it
                            val previewWidth = previewSize.value.width.toFloat()
                            val previewHeight = previewSize.value.height.toFloat()
                            val fingerSize = 100F
                            val rect = getFingerPointRect(
                                it.x,
                                it.y,
                                previewWidth,
                                previewHeight,
                                fingerSize
                            )
                            fingerRect.value = rect
                            val normalizedRect = rectNormalized(rect, previewWidth, previewHeight)
                            viewModel.focusRequest(normalizedRect)
                        }
                    }
                    .onSizeChanged {
                        previewSize.value = it
                    }
            ) {
                if (cameraCharacteristics != null) {
                    val focusPointRect = viewModel.focusPointRectFlow.collectAsState()
                    val captureResult = viewModel.captureResultFlow.collectAsState()
                    val sensorSize = cameraCharacteristics.sensorSize
                    if (sensorSize != null) {
                        val sensorWidth = sensorSize.width()
                        val sensorHeight = sensorSize.height()
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            focusPointRect.value?.let { fPointRect ->
                                val pointRect =
                                    rectFromNormalized(fPointRect, size.width, size.height)
                                drawRect(
                                    color = Color.White,
                                    topLeft = pointRect.topLeft,
                                    size = pointRect.size,
                                    style = Stroke(width = 10F)
                                )
                            }

                            captureResult.value?.apply {
                                afRegions?.forEach { meteringRectangle ->
                                    val rect = sensorDetectRect2ComposeRect(
                                        rect = meteringRectangle.rect,
                                        rotationOrientation = viewModel.rotationOrientation.value,
                                        flipHorizontal = cameraCharacteristics.isFrontCamera,
                                        size = size,
                                        sensorWidth = sensorWidth,
                                        sensorHeight = sensorHeight,
                                    )
                                    drawRect(
                                        color = Color.Red,
                                        topLeft = rect.topLeft,
                                        size = rect.size,
                                        style = Stroke(width = 4F)
                                    )
                                    val circleSize = 16F
                                    drawCircle(
                                        color = when (afState) {
                                            CameraMetadata.CONTROL_AF_STATE_ACTIVE_SCAN -> Color.White
                                            CameraMetadata.CONTROL_AF_STATE_FOCUSED_LOCKED -> Color.Green
                                            CameraMetadata.CONTROL_AF_STATE_NOT_FOCUSED_LOCKED -> Color.Red
                                            else -> Color.Gray
                                        },
                                        radius = circleSize,
                                        center = Offset(
                                            x = rect.right + circleSize,
                                            y = rect.top,
                                        ),
                                    )
                                }
                                aeRegions?.forEach { meteringRectangle ->
                                    val rect = sensorDetectRect2ComposeRect(
                                        rect = meteringRectangle.rect,
                                        rotationOrientation = viewModel.rotationOrientation.value,
                                        flipHorizontal = cameraCharacteristics.isFrontCamera,
                                        size = size,
                                        sensorWidth = sensorWidth,
                                        sensorHeight = sensorHeight,
                                    )
                                    drawRect(
                                        color = Color.Red,
                                        topLeft = rect.topLeft,
                                        size = rect.size,
                                        style = Stroke(width = 4F)
                                    )
                                    val circleSize = 16F
                                    drawCircle(
                                        color = when (aeState) {
                                            CameraMetadata.CONTROL_AE_STATE_PRECAPTURE -> Color.White
                                            CameraMetadata.CONTROL_AE_STATE_CONVERGED, CameraMetadata.CONTROL_AE_STATE_FLASH_REQUIRED -> Color.Green
                                            else -> Color.Gray
                                        },
                                        radius = circleSize,
                                        center = Offset(
                                            x = rect.right + circleSize,
                                            y = rect.bottom,
                                        ),
                                    )
                                }
                                faceDetectResult?.forEach {
                                    val faceRect = sensorDetectRect2ComposeRect(
                                        rect = it.bounds,
                                        rotationOrientation = viewModel.rotationOrientation.value,
                                        flipHorizontal = cameraCharacteristics.isFrontCamera,
                                        size = size,
                                        sensorWidth = sensorWidth,
                                        sensorHeight = sensorHeight,
                                    )
                                    drawRect(
                                        color = Color.Cyan,
                                        topLeft = faceRect.topLeft,
                                        size = faceRect.size,
                                        style = Stroke(width = 10F)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            CameraRawSeaLevel()
        }
    }
}

@Composable
fun CameraRawInfoLayer() {
    val viewModel: CameraRawViewModel = koinViewModel()
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .clip(Layout.roundShape.rm)
                .background(
                    MaterialTheme.colorScheme.surface.copy(0.6F)
                )
                .padding(Layout.padding.pm)
        ) {
            Text(
                text = "${viewModel.captureFrameRate.value}-${viewModel.rendererFrameRate.value}",
                color = Color.Green,
                fontSize = Layout.fontSize.fl,
                fontWeight = FontWeight.Bold
            )
            val captureResult = viewModel.captureResultFlow.collectAsState()
            Text(text = "曝光时间：${captureResult.value?.sensorExposureTime}")
            Text(text = "感光度：${captureResult.value?.sensorSensitivity}")
            Text(text = "电子变焦：${captureResult.value?.zoomRatio}")
            Text(text = "场景模式：${SceneMode.getByCode(captureResult.value?.sceneMode ?: -1)}")
            Text(text = "OIS：${captureResult.value?.oisEnable}")
        }

        Column(
            modifier = Modifier
                .align(Alignment.TopEnd)
        ) {
            val exposureHistogramData = viewModel.exposureHistogramDataFlow.collectAsState()
            if (exposureHistogramData.value != null) {
                Canvas(
                    modifier = Modifier
                        .width(160.dp)
                        .height(80.dp),
                    onDraw = {
                        drawRect(color = Color.White)
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
                                color = Color.Black,
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
fun CameraRawActionLayer(
    onCapture: () -> Unit,
) {
    val viewModel: CameraRawViewModel = koinViewModel()
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .run {
                    if (maxHeight > maxWidth) {
                        fillMaxWidth().align(Alignment.BottomCenter)
                    } else {
                        fillMaxHeight().align(Alignment.BottomEnd)
                    }
                }
                .aspectRatio(1F)
                .padding(Layout.padding.pm)
                .clip(Layout.roundShape.rm)
                .background(
                    MaterialTheme.colorScheme.surface.copy(0.6F)
                )
                .verticalScroll(state = rememberScrollState())
                .padding(Layout.padding.pm)
                .align(Alignment.BottomCenter)
        ) {
            val currentCamera = viewModel.currentCameraPairFlow.collectAsState()
            val cameraList = viewModel.cameraPairListFlow.collectAsState()
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "摄像头：")
                cameraList.value.forEachIndexed { index, pair ->
                    Button(enabled = pair != currentCamera.value, onClick = {
                        viewModel.currentCameraPairFlow.value = pair
                    }) {
                        Text(text = "$index")
                    }
                }
            }

            val currentCameraPair = viewModel.currentCameraPairFlow.collectAsState()
            val cameraCharacteristics = currentCameraPair.value?.second
            if (cameraCharacteristics != null) {
                val outputItemList = cameraCharacteristics.outputSupportedMode
                val currentOutputItem = viewModel.currentOutputItemFlow.collectAsState()
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "输出格式：")
                    Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                        outputItemList.forEach { outputItem ->
                            val enable = currentOutputItem.value == outputItem
                            Button(
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (enable) MaterialTheme.colorScheme.error
                                    else MaterialTheme.colorScheme.primary
                                ),
                                onClick = {
                                    viewModel.currentOutputItemFlow.value = outputItem
                                }
                            ) {
                                Column {
                                    Text(text = outputItem.outputMode.label)
                                    Text(
                                        text = outputItem.bestSize.run { "${width}x${height}" },
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        }
                    }
                }

                val captureMode = viewModel.captureModeFlow.collectAsState()
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "拍照模式：")
                    for (mode in CaptureMode.entries) {
                        Button(enabled = captureMode.value != mode, onClick = {
                            viewModel.captureModeFlow.value = mode
                        }) {
                            Text(text = mode.name)
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "辅助：")
                    Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                        val focusPeakingEnable = viewModel.focusPeakingEnableFlow.collectAsState()
                        EnableButton(
                            enable = focusPeakingEnable.value,
                            label = "峰值对焦",
                            onClick = { viewModel.focusPeakingEnableFlow.apply { value = !value } }
                        )
                        val brightnessPeakingEnable =
                            viewModel.brightnessPeakingEnableFlow.collectAsState()
                        EnableButton(
                            enable = brightnessPeakingEnable.value,
                            label = "峰值亮度",
                            onClick = {
                                viewModel.brightnessPeakingEnableFlow.apply {
                                    value = !value
                                }
                            }
                        )
                        val exposureHistogramEnable =
                            viewModel.exposureHistogramEnableFlow.collectAsState()
                        EnableButton(
                            enable = exposureHistogramEnable.value,
                            label = "直方图",
                            onClick = {
                                viewModel.exposureHistogramEnableFlow.apply {
                                    value = !value
                                }
                            }
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "场景模式：")
                    Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                        val currentSceneMode =
                            viewModel.captureController.currentSceneModeFlow.collectAsState()
                        EnableButton(
                            enable = currentSceneMode.value == null,
                            label = "无",
                            onClick = {
                                viewModel.captureController.currentSceneModeFlow.value = null
                            }
                        )
                        cameraCharacteristics.sceneModes.forEach { sceneMode ->
                            EnableButton(
                                enable = currentSceneMode.value == sceneMode,
                                label = sceneMode.name,
                                onClick = {
                                    viewModel.captureController.currentSceneModeFlow.value =
                                        sceneMode
                                }
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "光学防抖：")
                    Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                        val oisEnable = viewModel.captureController.oisEnableFlow.collectAsState()
                        val oisAvailable = cameraCharacteristics.oisAvailable
                        if (oisAvailable) {
                            EnableButton(
                                enable = oisEnable.value,
                                label = "OIS",
                                onClick = {
                                    viewModel.captureController.oisEnableFlow.apply {
                                        value = !value
                                    }
                                }
                            )
                        } else {
                            Button(onClick = { }, enabled = false) {
                                Text(text = "无")
                            }
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "人脸识别：")
                    Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                        val currentFaceDetectMode =
                            viewModel.captureController.currentFaceDetectModeFlow.collectAsState()
                        cameraCharacteristics.faceDetectModes.forEach { faceDetectMode ->
                            EnableButton(
                                enable = currentFaceDetectMode.value == faceDetectMode,
                                label = faceDetectMode.label,
                                onClick = {
                                    viewModel.captureController.currentFaceDetectModeFlow.value =
                                        faceDetectMode
                                }
                            )
                        }
                    }
                }

                val zoomRatioRange = cameraCharacteristics.zoomRatioRange
                if (zoomRatioRange != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "电子变焦：")
                        val zoomRatio = viewModel.captureController.zoomRatioFlow.collectAsState()
                        Box(modifier = Modifier.width(80.dp)) {
                            Text(text = "${zoomRatio.value}/${zoomRatioRange.upper}")
                        }
                        Slider(
                            valueRange = zoomRatioRange.run { lower.toFloat()..upper.toFloat() },
                            value = zoomRatio.value,
                            onValueChange = {
                                viewModel.captureController.zoomRatioFlow.value = it
                            },
                        )
                    }
                }

                Column(modifier = Modifier.animateContentSize()) {
                    if (captureMode.value == CaptureMode.MANUAL) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "手动控制：")

                            Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                                val afEnable =
                                    viewModel.captureController.afEnableFlow.collectAsState(initial = true)
                                EnableButton(
                                    enable = afEnable.value,
                                    label = "AF",
                                    onClick = { viewModel.captureController.setAfEnable() })

                                val aeEnable =
                                    viewModel.captureController.aeEnableFlow.collectAsState(initial = true)
                                EnableButton(
                                    enable = aeEnable.value,
                                    label = "AE",
                                    onClick = { viewModel.captureController.setAeEnable() })

                                val awbEnable =
                                    viewModel.captureController.awbEnableFlow.collectAsState(initial = true)
                                EnableButton(
                                    enable = awbEnable.value,
                                    label = "AWB",
                                    onClick = { viewModel.captureController.setAwbEnable() }
                                )
                            }
                        }

                        val focalDistanceRange = cameraCharacteristics.focalDistanceRange
                        val focalDistance =
                            viewModel.captureController.focalDistanceFlow.collectAsState()
                        if (focalDistanceRange != null) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(text = "对焦距离：")
                                Box(modifier = Modifier.width(80.dp)) {
                                    Text(text = "${focalDistance.value}/${focalDistanceRange.upper}")
                                }
                                Slider(
                                    valueRange = focalDistanceRange.run { lower.toFloat()..upper.toFloat() },
                                    value = focalDistance.value
                                        ?: focalDistanceRange.lower.toFloat(),
                                    onValueChange = {
                                        viewModel.captureController.focalDistanceFlow.value = it
                                    },
                                )
                            }
                        }

                        val sensorSensitivityRange = cameraCharacteristics.sensorSensitivityRange
                        val sensorSensitivity =
                            viewModel.captureController.sensorSensitivityFlow.collectAsState()
                        if (sensorSensitivityRange != null) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(text = "感光度：")
                                Box(modifier = Modifier.width(80.dp)) {
                                    Text(text = "${sensorSensitivity.value}/${sensorSensitivityRange.upper}")
                                }
                                Slider(
                                    valueRange = sensorSensitivityRange.run { lower.toFloat()..upper.toFloat() },
                                    value = sensorSensitivity.value?.toFloat()
                                        ?: sensorSensitivityRange.lower.toFloat(),
                                    onValueChange = {
                                        viewModel.captureController.sensorSensitivityFlow.value =
                                            it.toInt()
                                    },
                                )
                            }
                        }

                        val sensorExposureTime =
                            viewModel.captureController.sensorExposureTimeFlow.collectAsState()
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "曝光时间：")
                            Row(
                                modifier = Modifier
                                    .weight(1F)
                                    .horizontalScroll(rememberScrollState())
                            ) {
                                for (exposureTime in ExposureTime.entries) {
                                    Button(
                                        enabled = exposureTime.time != sensorExposureTime.value,
                                        onClick = {
                                            viewModel.captureController.sensorExposureTimeFlow.value =
                                                exposureTime.time
                                        }
                                    ) {
                                        Text(text = exposureTime.label)
                                    }
                                }
                            }
                        }

                        val customTemperature =
                            viewModel.captureController.customTemperatureFlow.collectAsState()
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "白平衡：")
                            Box(modifier = Modifier.width(80.dp)) {
                                Text(text = "${customTemperature.value}")
                            }
                            Slider(
                                valueRange = 0F..100F,
                                value = customTemperature.value?.toFloat() ?: 0F,
                                onValueChange = {
                                    viewModel.captureController.customTemperatureFlow.value =
                                        it.toInt()
                                },
                            )
                        }
                    } else {
                        val aeCompensationRange = cameraCharacteristics.aeCompensationRange
                        val aeCompensation =
                            viewModel.captureController.aeCompensationFlow.collectAsState()
                        if (aeCompensationRange != null) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(text = "曝光补偿：")
                                Slider(
                                    valueRange = aeCompensationRange.run { lower.toFloat()..upper.toFloat() },
                                    value = aeCompensation.value.toFloat(),
                                    onValueChange = {
                                        viewModel.captureController.aeCompensationFlow.value =
                                            it.toInt()
                                    },
                                )
                            }
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.Center) {
                    val captureLoading = viewModel.captureLoading
                    Button(onClick = onCapture, enabled = !captureLoading.value) {
                        if (captureLoading.value) {
                            CircularProgressIndicator(
                                modifier = Modifier
                                    .padding(end = 12.dp)
                                    .size(16.dp),
                                strokeWidth = 2.dp
                            )
                        }
                        Text(text = "拍照")
                    }
                }
            }
        }
    }
}

@Composable
fun CameraRawSeaLevel() {
    val density = LocalDensity.current
    val viewModel: CameraRawViewModel = koinViewModel()
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