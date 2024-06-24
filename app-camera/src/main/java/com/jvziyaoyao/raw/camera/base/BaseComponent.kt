package com.jvziyaoyao.raw.camera.base

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.jvziyaoyao.raw.camera.ui.theme.Layout
import com.jvziyaoyao.raw.camera.util.findWindow
import com.jvziyaoyao.raw.camera.util.preventPointerInput
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

@Composable
fun ScaleAnimatedVisibility(
    visible: Boolean,
    content: @Composable () -> Unit,
) {
    AnimatedVisibility(
        visible = visible,
        enter = scaleIn(
            animationSpec = tween(200),
            transformOrigin = TransformOrigin(0.5F, 1F)
        ) + fadeIn(),
        exit = scaleOut(
            animationSpec = tween(200),
            transformOrigin = TransformOrigin(0.5F, 1F)
        ) + fadeOut(),
    ) {
        content()
    }
}

@Composable
fun FadeAnimatedVisibility(
    visible: Boolean,
    content: @Composable () -> Unit,
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut(),
    ) {
        content()
    }
}

@Composable
fun ConfirmView(
    content: String = "🎉 节日快乐！",
    title: String? = "提示",
    confirmText: String? = "确认",
    cancelText: String? = "取消",
    confirm: () -> Unit = {},
    cancel: () -> Unit = {},
    confirmTextColor: Color = LocalTextStyle.current.color,
    cancelTextColor: Color = LocalTextStyle.current.color,
    loading: Boolean = false,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .preventPointerInput()
            .background(MaterialTheme.colorScheme.surface),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Layout.padding.pl),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (!title.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(Layout.padding.pxl))
                Text(text = title, color = LocalTextStyle.current.color.copy(0.6F))
            }
            Spacer(modifier = Modifier.height(Layout.padding.pxl))
            Text(text = content)
            Spacer(modifier = Modifier.height(Layout.padding.pxxl))
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .alpha(if (loading) 0.4F else 1F)
        ) {
            if (!cancelText.isNullOrEmpty()) Box(
                modifier = Modifier
                    .weight(1F)
                    .background(MaterialTheme.colorScheme.background)
                    .clickable(!loading) {
                        cancel()
                    }
                    .padding(vertical = Layout.padding.pm),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    color = cancelTextColor,
                    text = cancelText,
                )
            }
            if (!confirmText.isNullOrEmpty()) Row(
                modifier = Modifier
                    .weight(1F)
                    .background(MaterialTheme.colorScheme.background)
                    .clickable(!loading) {
                        confirm()
                    }
                    .padding(vertical = Layout.padding.pm),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Text(
                    color = confirmTextColor,
                    text = confirmText,
                )
                AnimatedVisibility(visible = loading) {
                    Box(modifier = Modifier.padding(start = Layout.padding.ps)) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            strokeWidth = 1.6.dp,
                            color = confirmTextColor
                        )
                    }
                }
            }
        }
    }
}