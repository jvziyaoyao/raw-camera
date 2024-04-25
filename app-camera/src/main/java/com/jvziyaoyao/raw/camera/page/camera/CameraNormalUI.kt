package com.jvziyaoyao.raw.camera.page.camera

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.jvziyaoyao.raw.camera.ui.theme.Layout
import com.jvziyaoyao.raw.camera.util.formatToDecimalPlaces
import org.koin.androidx.compose.koinViewModel

@Composable
fun CameraNormalLayer() {
    val viewModel: CameraViewModel = koinViewModel()
    val zoomRatio = viewModel.captureController.zoomRatioFlow.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = Layout.padding.pl)
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.onBackground.copy(0.4F))
        ) {
            Text(
                modifier = Modifier.align(Alignment.Center),
                text = "${zoomRatio.value.formatToDecimalPlaces(1)}X",
                fontSize = Layout.fontSize.fxs,
                color = MaterialTheme.colorScheme.background.copy(0.6F),
            )
        }
    }
}