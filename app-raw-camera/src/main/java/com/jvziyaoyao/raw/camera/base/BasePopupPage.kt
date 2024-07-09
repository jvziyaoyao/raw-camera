package com.jvziyaoyao.raw.camera.base

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideIn
import androidx.compose.animation.slideOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.jvziyaoyao.raw.camera.ui.theme.Layout
import com.jvziyaoyao.raw.camera.ui.theme.isPad
import kotlinx.coroutines.delay

/**
 * @program: WePretend
 *
 * @description:
 *
 * @author: JVZIYAOYAO
 *
 * @create: 2023-06-29 12:19
 **/
val LocalPopupState = compositionLocalOf { PopupMapState() }

class PopupMapState {
    val popupMap = mutableStateMapOf<String, @Composable () -> Unit>()
}

@Composable
fun rememberPopupMapState(): PopupMapState {
    return remember {
        PopupMapState()
    }
}

@Composable
fun BasePopupPage(
    popupMapState: PopupMapState = rememberPopupMapState(),
    content: @Composable () -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        CompositionLocalProvider(LocalPopupState provides popupMapState) {
            content()
        }

        for (entry in popupMapState.popupMap) {
            entry.value.invoke()
        }
    }
}

@Composable
fun CommonDialogBackground(
    shape: Shape = Layout.roundShape.rl,
    content: @Composable () -> Unit,
) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        val fill = if (isPad) 0.48F else 0.84F
        Box(
            modifier = Modifier
                .fillMaxWidth(fill)
                .clip(shape)
                .background(MaterialTheme.colorScheme.surface),
        ) {
            content()
        }
    }
}

@Composable
fun FlatDialog(
    showDialog: Boolean,
    contentAlignment: Alignment = Alignment.Center,
    enter: EnterTransition = fadeIn(tween(300, 100)),
    exit: ExitTransition = fadeOut(tween(300)),
    onDismissRequest: () -> Unit,
    content: @Composable () -> Unit,
) {
    var realShowDialog by remember { mutableStateOf(false) }
    var closeSheet by remember { mutableStateOf(false) }
    BackHandler(showDialog) {
        onDismissRequest()
    }
    LaunchedEffect(key1 = showDialog) {
        if (showDialog) {
            closeSheet = false
        } else {
            closeSheet = true
            delay(100)
        }
        realShowDialog = showDialog
    }
    AnimatedVisibility(visible = realShowDialog, enter = fadeIn(), exit = fadeOut(tween(200))) {
        Box(modifier = Modifier.fillMaxSize()) {
            var showActionSheet by remember { mutableStateOf(false) }
            DisposableEffect(key1 = Unit) {
                showActionSheet = true
                onDispose {
                    showActionSheet = false
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.onBackground.copy(0.72F))
                    .pointerInput(Unit) {
                        detectTapGestures {
                            onDismissRequest()
                        }
                    },
                contentAlignment = contentAlignment,
            ) {
                AnimatedVisibility(
                    visible = showActionSheet && !closeSheet,
                    enter = enter,
                    exit = exit,
                ) {
                    Box(
                        modifier = Modifier
                            .pointerInput(Unit) {
                                detectTapGestures { }
                            }
                    ) {
                        content()
                    }
                }
            }
        }
    }
}

@Composable
fun FlatActionSheet(
    showDialog: Boolean,
    onDismissRequest: () -> Unit,
    content: @Composable () -> Unit,
) {
    var realShowDialog by remember { mutableStateOf(false) }
    var closeSheet by remember { mutableStateOf(false) }
    BackHandler(showDialog) {
        onDismissRequest()
    }
    LaunchedEffect(key1 = showDialog) {
        if (showDialog) {
            closeSheet = false
        } else {
            closeSheet = true
            delay(100)
        }
        realShowDialog = showDialog
    }
    AnimatedVisibility(visible = realShowDialog, enter = fadeIn(), exit = fadeOut(tween(200))) {
        Box(modifier = Modifier.fillMaxSize()) {
            var showActionSheet by remember { mutableStateOf(false) }
            DisposableEffect(key1 = Unit) {
                showActionSheet = true
                onDispose {
                    showActionSheet = false
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background.copy(0.72F))
                    .pointerInput(Unit) {
                        detectTapGestures {
                            onDismissRequest()
                        }
                    },
                contentAlignment = Alignment.BottomCenter
            ) {
                AnimatedVisibility(
                    visible = showActionSheet && !closeSheet,
                    enter = slideIn(tween(300, 100)) { IntOffset(0, it.height) },
                    exit = slideOut(tween(300)) { IntOffset(0, it.height) },
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                    ) {
                        content()
                    }
                }
            }
        }
    }
}

@Composable
fun RowScope.DialogButton(
    title: String,
    color: Color = MaterialTheme.colorScheme.onBackground.copy(0.4F),
    backgroundColor: Color = MaterialTheme.colorScheme.onBackground.copy(0.1F),
    onClick: () -> Unit = {},
) {
    Text(
        modifier = Modifier
            .weight(1F)
            .clip(RoundedCornerShape(10.dp))
            .background(backgroundColor)
            .clickable {
                onClick()
            }
            .padding(vertical = Layout.padding.ps),
        text = title,
        color = color,
        textAlign = TextAlign.Center,
    )
}

@Composable
fun ConfirmDeleteDialog(
    showDialog: Boolean,
    content: String = "💥 确认删除这些内容吗？",
    onDismissRequest: () -> Unit,
    onDelete: () -> Unit,
) {
    FlatDialog(
        showDialog = showDialog,
        onDismissRequest = onDismissRequest,
    ) {
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Column(
                modifier = Modifier
                    .fillMaxWidth(0.84F)
                    .clip(Layout.roundShape.rl)
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(Layout.padding.pl),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(text = "提示", fontSize = Layout.fontSize.fl)
                Spacer(modifier = Modifier.height(Layout.padding.pl))
                Text(
                    text = content,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onBackground.copy(0.6F)
                )
                Spacer(modifier = Modifier.height(Layout.padding.pxl))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    DialogButton(
                        title = "取消",
                        onClick = {
                            onDismissRequest()
                        }
                    )
                    Spacer(modifier = Modifier.width(Layout.padding.pm))
                    DialogButton(
                        title = "删除",
                        color = MaterialTheme.colorScheme.onPrimary,
                        backgroundColor = MaterialTheme.colorScheme.error,
                        onClick = {
                            onDelete()
                            onDismissRequest()
                        }
                    )
                }
            }
        }
    }
}