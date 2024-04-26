package com.jvziyaoyao.raw.sample.page.support

import android.content.Context
import android.content.res.Configuration
import android.hardware.camera2.CaptureRequest
import android.os.Bundle
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.camera.core.ImageCapture
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.common.util.concurrent.ListenableFuture
import com.jvziyaoyao.camera.raw.util.ContextUtil
import java.util.concurrent.Executors
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class SupportActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val systemBarStyle =
            when (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) {
                Configuration.UI_MODE_NIGHT_NO -> SystemBarStyle.light(
                    android.graphics.Color.TRANSPARENT,
                    android.graphics.Color.TRANSPARENT
                )

                Configuration.UI_MODE_NIGHT_YES -> SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                else -> SystemBarStyle.light(
                    android.graphics.Color.TRANSPARENT,
                    android.graphics.Color.TRANSPARENT
                )
            }
        enableEdgeToEdge(
            statusBarStyle = systemBarStyle,
            navigationBarStyle = systemBarStyle,
        )
        setContent {
            SupportCameraBody()
        }
    }

}

suspend fun <T> getFuture(context: Context, future: ListenableFuture<T>) =
    suspendCoroutine<T> { c ->
        try {
            future.addListener({
                c.resume(future.get())
            }, ContextCompat.getMainExecutor(context))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

suspend fun <T> ListenableFuture<T>.getFuture(context: Context = ContextUtil.getApplicationByReflect()): T {
    return getFuture(context, this)
}

@Composable
fun SupportCameraBody() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraController = remember {
        LifecycleCameraController(context).apply {
//            imageCaptureFlashMode = ImageCapture.FLASH_MODE_ON
//            cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA
            imageCaptureFlashMode = ImageCapture.FLASH_MODE_ON
        }
    }
    val executor = remember { Executors.newSingleThreadExecutor() }
    AndroidView(
        modifier = Modifier
            .fillMaxSize(),
        factory = { ctx ->
            PreviewView(ctx).apply {
                setBackgroundColor(Color.Black.toArgb())
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                scaleType = PreviewView.ScaleType.FILL_START
                implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            }.also { previewView ->
                previewView.controller = cameraController
                cameraController.bindToLifecycle(lifecycleOwner)
            }
        },
        onReset = {},
        onRelease = {
            cameraController.unbind()
        },
    )
    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.4F)
                .align(Alignment.BottomCenter),
            contentAlignment = Alignment.Center,
        ) {
            CameraCaptureButton(loading = false) {
                CaptureRequest.CONTROL_AE_LOCK
                cameraController.takePicture(executor,
                    object : ImageCapture.OnImageCapturedCallback() {

                    }
                )
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
                color = MaterialTheme.colorScheme.onBackground.copy(0.4F),
            )
        }
        AnimatedVisibility(
            visible = !loading,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            CircularProgressIndicator(
                progress = 100F,
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
                .background(color = Color.White)
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