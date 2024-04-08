package com.jvziyaoyao.raw.camera.page.sensor

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.jvziyaoyao.raw.camera.ui.base.animateRotationAsState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import org.koin.androidx.compose.koinViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel


class SensorActivity : ComponentActivity(), CoroutineScope by MainScope() {

    private val mViewModel by viewModel<SensorViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mViewModel.setupSensor()

        setContent {
            SensorBody()
        }
    }

    override fun onResume() {
        super.onResume()

        mViewModel.startSensor()
    }

    override fun onPause() {
        super.onPause()

        mViewModel.stopSensor()
    }

}

@Composable
fun SensorBody() {
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        SensorLayer()
        SensorInfoLayer()
    }
}

@Composable
fun SensorLayer() {
    val viewModel: SensorViewModel = koinViewModel()
    val density = LocalDensity.current
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

        val gravity = viewModel.gravityFlow.collectAsState()
        val gravityDegreesAnimation = animateRotationAsState(targetValue = gravity.value)
        Box(
            modifier = Modifier
                .graphicsLayer {
                    rotationZ = gravityDegreesAnimation.value
                }
                .width(100.dp)
                .height(4.dp)
                .clip(CircleShape)
                .background(color = MaterialTheme.colorScheme.primary)
                .align(Alignment.Center)
        )

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1F)
                .align(Alignment.Center)
        ) {
            val pitch = viewModel.pitchFlow.collectAsState()
            val roll = viewModel.rollFlow.collectAsState()
            val pitchAnimation = animateFloatAsState(targetValue = pitch.value)
            val rollAnimation = animateFloatAsState(targetValue = roll.value)
            val offsetY = remember {
                derivedStateOf {
                    var p = pitchAnimation.value.div(90F)
                    if (p < -1) p = -1F
                    if (p > 1) p = 1F
                    density.run { maxHeight.toPx().div(2).times(p) }
                }
            }
            val offsetX = remember {
                derivedStateOf {
                    var p = -rollAnimation.value.div(90F)
                    if (p < -1) p = -1F
                    if (p > 1) p = 1F
                    density.run { maxWidth.toPx().div(2).times(p) }
                }
            }
            Box(
                modifier = Modifier
                    .graphicsLayer {
                        translationX = offsetX.value
                        translationY = offsetY.value
                    }
                    .size(60.dp)
                    .clip(CircleShape)
                    .background(color = MaterialTheme.colorScheme.onBackground.copy(0.2F))
                    .align(Alignment.Center)
            )
        }
    }
}

@Composable
fun SensorInfoLayer() {
    val viewModel: SensorViewModel = koinViewModel()
    val gravity = viewModel.gravityFlow.collectAsState()
    val pitch = viewModel.pitchFlow.collectAsState()
    val roll = viewModel.rollFlow.collectAsState()
    val yaw = viewModel.yawFlow.collectAsState()
    Column(modifier = Modifier.fillMaxSize()) {
        Text(text = "旋转角度：")
        Text(text = "gravity: ${gravity.value}")
        Text(text = "pitch: ${pitch.value}")
        Text(text = "roll：${roll.value}")
        Text(text = "yaw：${yaw.value}")
    }
}