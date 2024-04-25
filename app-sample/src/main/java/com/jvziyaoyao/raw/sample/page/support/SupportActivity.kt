package com.jvziyaoyao.raw.sample.page.support

import android.content.Context
import android.os.Bundle
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.MeteringPoint
import androidx.camera.core.SurfaceOrientedMeteringPointFactory
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.common.util.concurrent.ListenableFuture
import com.jvziyaoyao.camera.raw.util.ContextUtil
import com.jvziyaoyao.raw.sample.ui.theme.Layout
import kotlinx.coroutines.launch
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class SupportActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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
    val cameraController = remember { LifecycleCameraController(context) }
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
    Box(
        modifier = Modifier
            .padding(Layout.padding.pl)
            .clip(Layout.roundShape.rs)
            .background(Color.White.copy(0.2F))
            .padding(Layout.padding.pm)
    ) {
        val tapToFocusState = cameraController.tapToFocusState.observeAsState()
        Text(text = "state: ${tapToFocusState.value}")
    }
}