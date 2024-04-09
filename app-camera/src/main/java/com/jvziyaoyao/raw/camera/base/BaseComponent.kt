package com.jvziyaoyao.raw.camera.base

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowCompat
import com.jvziyaoyao.raw.camera.util.findWindow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue

@Composable
fun DynamicStatusBarColor(
    dark: Boolean,
) {
    val context = LocalContext.current
    LaunchedEffect(dark) {
        context.findWindow()?.let {
            WindowCompat.getInsetsController(it, it.decorView)
                .isAppearanceLightStatusBars = dark
        }
    }
}

@Composable
fun animateRotationAsState(
    targetValue: Float,
    animationSpec: AnimationSpec<Float> = remember { spring() },
    visibilityThreshold: Float = 0.01f,
    finishedListener: ((Float) -> Unit)? = null
): State<Float> {
    val animatable = remember { Animatable(targetValue, visibilityThreshold) }
    val listener by rememberUpdatedState(finishedListener)
    val animSpec: AnimationSpec<Float> by rememberUpdatedState(
        animationSpec.run {
            if (this is SpringSpec && this.visibilityThreshold != visibilityThreshold
            ) {
                spring(dampingRatio, stiffness, visibilityThreshold)
            } else {
                this
            }
        }
    )
    val channel = remember { Channel<Float>(Channel.CONFLATED) }
    SideEffect {
        channel.trySend(targetValue)
    }
    LaunchedEffect(channel) {
        for (target in channel) {
            val newTarget = channel.tryReceive().getOrNull() ?: target
            launch {
                if (newTarget != animatable.targetValue) {
                    val delta = newTarget - animatable.value
                    if (delta.absoluteValue > 200F) {
                        if (delta > 0) {
                            animatable.snapTo(360 + animatable.value)
                            animatable.animateTo(newTarget)
                        } else {
                            animatable.snapTo(animatable.value - 360)
                            animatable.animateTo(newTarget)
                        }
                    } else {
                        animatable.animateTo(newTarget, animSpec)
                    }
                    listener?.invoke(animatable.value)
                }
            }
        }
    }
    return animatable.asState()
}