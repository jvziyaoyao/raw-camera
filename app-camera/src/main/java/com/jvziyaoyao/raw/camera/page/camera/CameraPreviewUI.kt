package com.jvziyaoyao.raw.camera.page.camera

import android.annotation.SuppressLint
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.jvziyaoyao.raw.camera.base.FlatActionSheet
import com.jvziyaoyao.raw.camera.base.ForegroundTabMenuItem
import com.jvziyaoyao.raw.camera.base.LocalPopupState
import com.jvziyaoyao.raw.camera.base.rememberCoilImagePainter
import com.jvziyaoyao.raw.camera.domain.model.Exif
import com.jvziyaoyao.raw.camera.domain.model.MediaQueryEntity
import com.jvziyaoyao.raw.camera.page.image.ImageActivity
import com.jvziyaoyao.raw.camera.ui.theme.Layout
import com.jvziyaoyao.raw.camera.util.findWindow
import com.jvziyaoyao.raw.camera.util.hideSystemUI
import com.jvziyaoyao.raw.camera.util.preventPointerInput
import com.jvziyaoyao.raw.camera.util.shareItems
import com.jvziyaoyao.raw.camera.util.showSystemUI
import com.jvziyaoyao.scale.zoomable.pager.PagerGestureScope
import com.jvziyaoyao.scale.zoomable.previewer.Previewer
import com.jvziyaoyao.scale.zoomable.previewer.PreviewerState
import com.jvziyaoyao.scale.zoomable.previewer.TransformLayerScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date

@Composable
fun CameraPreviewer(
    images: List<MediaQueryEntity>,
    previewerState: PreviewerState,
    onDelete: (MediaQueryEntity) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val window = context.findWindow()
    var showDetail by rememberSaveable { mutableStateOf(false) }

    var showDelete by rememberSaveable { mutableStateOf(false) }
    val deleteConfirm = remember { MutableStateFlow<Boolean?>(null) }

    val fullScreen = remember { mutableStateOf(false) }
    if (previewerState.canClose || previewerState.animating || showDetail || showDelete) {
        BackHandler {
            scope.launch {
                if (showDetail) {
                    showDetail = false
                } else if (showDelete) {
                    showDelete = false
                } else if (fullScreen.value) {
                    fullScreen.value = false
                } else {
                    previewerState.exitTransform()
                }
            }
        }
    }
    LaunchedEffect(key1 = fullScreen.value) {
        if (window == null) return@LaunchedEffect
        if (fullScreen.value) {
            hideSystemUI(window)
        } else {
            showSystemUI(window)
        }
    }
    Previewer(
        state = previewerState,
        previewerLayer = TransformLayerScope(
            background = {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                )
            },
            foreground = {
                PreviewerForeground(
                    mediaQueryEntity = images[previewerState.currentPage],
                    fullScreen = fullScreen.value,
                    onDetail = {
                        showDetail = true
                    },
                    onShare = {
                        shareItems(context, listOf(images[previewerState.currentPage]))
                    },
                    onDelete = {
                        scope.launch {
                            showDelete = true
                            deleteConfirm.value = null
                            deleteConfirm.takeWhile { it == null }.collectLatest { }
                            val confirm = deleteConfirm.value
                            if (confirm == true) {
                                onDelete(images[previewerState.currentPage])
                            }
                            showDelete = false
                        }
                    },
                    onAll = {
                        context.startActivity(Intent(context, ImageActivity::class.java))
                        scope.launch {
                            delay(2000)
                            previewerState.exitTransform()
                        }
                    },
                    onBack = {
                        scope.launch {
                            previewerState.exitTransform()
                        }
                    }
                )
            }
        ),
        detectGesture = PagerGestureScope(
            onTap = {
                fullScreen.value = !fullScreen.value
            },
            onDoubleTap = {
                fullScreen.value = true
                false
            }
        ),
        zoomablePolicy = { index ->
            val painter = rememberCoilImagePainter(image = images[index].path!!)
            if (painter.intrinsicSize.isSpecified) {
                ZoomablePolicy(intrinsicSize = painter.intrinsicSize) {
                    Image(
                        modifier = Modifier.fillMaxSize(),
                        painter = painter,
                        contentDescription = null,
                    )
                }
            } else {
                Box(modifier = Modifier.fillMaxSize()) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
            }
            painter.intrinsicSize.isSpecified
        }
    )

    AnimatedVisibility(
        visible = showDetail,
        enter = fadeIn() + scaleIn(initialScale = 2F),
        exit = fadeOut() + scaleOut(targetScale = 2F),
    ) {
        ImageDetail(
            photoEntity = images[previewerState.currentPage],
        ) {
            showDetail = false
        }
    }

    val popupState = LocalPopupState.current
    popupState.popupMap["preview_body"] = {
        DeleteView(
            visible = showDelete,
            confirm = {
                deleteConfirm.value = true
            },
            cancel = {
                deleteConfirm.value = false
            },
        )
    }
}

@SuppressLint("SimpleDateFormat")
private val timeDataFormat = SimpleDateFormat("HH:mm:ss")

@SuppressLint("SimpleDateFormat")
private val dayDataFormatWithYear = SimpleDateFormat("yyyy年MM月dd日")

@SuppressLint("SimpleDateFormat")
private val dayDataFormat = SimpleDateFormat("MM月dd日")

// TODO 不应该放这里
const val DAY_LENGTH = 1000 * 60 * 60 * 24
fun getDayLabel(timeStamp: Long): String {
    val calendar = Calendar.getInstance()
    calendar.set(Calendar.HOUR_OF_DAY, 0)
    calendar.set(Calendar.MINUTE, 0)
    calendar.set(Calendar.SECOND, 0)
    calendar.set(Calendar.MILLISECOND, 0)
    val thisYear = calendar.get(Calendar.YEAR)
    val todayZero = calendar.time.time
    if (timeStamp - todayZero >= 0) return "今天"
    if (timeStamp - todayZero + DAY_LENGTH > 0) return "昨天"
    val year = calendar.get(Calendar.YEAR)
    if (thisYear != year) return dayDataFormatWithYear.format(Date(timeStamp))
    return dayDataFormat.format(Date(timeStamp))
}

@Composable
fun PreviewerForeground(
    fullScreen: Boolean,
    mediaQueryEntity: MediaQueryEntity,
    onDetail: () -> Unit = {},
    onShare: () -> Unit = {},
    onDelete: () -> Unit = {},
    onAll: () -> Unit = {},
    onBack: () -> Unit = {},
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        AnimatedVisibility(
            visible = !fullScreen,
            modifier = Modifier.fillMaxWidth(),
            enter = fadeIn() + slideInVertically { h -> -h },
            exit = fadeOut() + slideOutVertically { h -> -h },
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                PreviewerForegroundNav(
                    mediaQueryEntity = mediaQueryEntity,
                    onBack = onBack
                ) {
                    Text(
                        modifier = Modifier
                            .clip(Layout.roundShape.rs)
                            .clickable { onAll() }
                            .padding(
                                horizontal = Layout.padding.pm,
                                vertical = Layout.padding.ps,
                            ),
                        text = "全部图片",
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = !fullScreen,
            modifier = Modifier.fillMaxWidth(),
            enter = fadeIn() + slideInVertically { h -> h },
            exit = fadeOut() + slideOutVertically { h -> h },
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .navigationBarsPadding()
            ) {
                ForegroundTab(
                    onDetail = onDetail,
                    onDelete = onDelete,
                    onShare = onShare,
                )
            }
        }
    }
}

@Composable
fun PreviewerForegroundNav(
    mediaQueryEntity: MediaQueryEntity,
    onBack: () -> Unit = {},
    end: @Composable () -> Unit = {},
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = Layout.padding.ps,
                vertical = Layout.padding.pxxs,
            )
            .statusBarsPadding(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(imageVector = Icons.Filled.ArrowBack, contentDescription = null)
        }
        val dateAdd = mediaQueryEntity.dateAdded
        Text(
            text = if (dateAdd != null) {
                "${getDayLabel(dateAdd)} ${timeDataFormat.format(Date(dateAdd))}"
            } else ""
        )
        Spacer(modifier = Modifier.weight(1F))
        end()
    }
}

@Composable
fun ForegroundTab(
    onDetail: () -> Unit = {},
    onShare: () -> Unit = {},
    onDelete: () -> Unit = {},
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .preventPointerInput()
            .padding(horizontal = Layout.padding.pl),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = Layout.padding.ps),
            horizontalArrangement = Arrangement.SpaceAround,
        ) {
            ForegroundTabMenuItem(
                icon = Icons.Outlined.Info,
                label = "详情",
                color = MaterialTheme.colorScheme.onBackground,
            ) {
                onDetail()
            }
            ForegroundTabMenuItem(
                icon = Icons.Outlined.Share,
                label = "分享",
                color = MaterialTheme.colorScheme.onBackground,
            ) {
                onShare()
            }
            ForegroundTabMenuItem(
                icon = Icons.Outlined.Delete,
                label = "删除",
                color = MaterialTheme.colorScheme.error,
            ) {
                onDelete()
            }
        }
    }
}

@Composable
fun ImageDetail(
    photoEntity: MediaQueryEntity,
    onBack: () -> Unit = {},
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .preventPointerInput()
            .verticalScroll(state = rememberScrollState())
    ) {
        PreviewerForegroundNav(
            mediaQueryEntity = photoEntity,
            onBack = onBack,
        )
        photoEntity.apply {
            Spacer(modifier = Modifier.height(Layout.padding.pxxl))
            if (!name.isNullOrEmpty()) ImageDetailItem(
                label = "File",
                content = photoEntity.name!!
            )
            if (width != null && height != null) ImageDetailItem(
                label = "Resolution",
                content = "${width}×${height}"
            )
            if (!calcSize.isNullOrEmpty()) ImageDetailItem(
                label = "File size",
                content = "$calcSize"
            )
            if (!purePath.isNullOrEmpty()) ImageDetailItem(
                label = "Path",
                content = "$purePath"
            )
            Spacer(modifier = Modifier.height(Layout.padding.pxxl))
            if (!timeAddDate.isNullOrEmpty()) ImageDetailItem(
                label = "Time",
                content = "$timeAddDate"
            )
            if (!timeModifiedDate.isNullOrEmpty()) ImageDetailItem(
                label = "Modified",
                content = "$timeModifiedDate"
            )
        }
        var exif by remember { mutableStateOf<Exif?>(null) }
        LaunchedEffect(photoEntity.path) {
            exif = photoEntity.getExif()
        }
        exif?.apply {
            Spacer(modifier = Modifier.height(Layout.padding.pxxl))
            if (!make.isNullOrEmpty()) ImageDetailItem(label = "Manufacturer", content = make)
            if (!model.isNullOrEmpty()) ImageDetailItem(label = "Model", content = model)
            if (!flash.isNullOrEmpty()) ImageDetailItem(label = "Flash", content = flash)
            if (!focal_length.isNullOrEmpty()) ImageDetailItem(
                label = "Focal length",
                content = focal_length
            )
            if (!aperture.isNullOrEmpty()) ImageDetailItem(
                label = "Aperture",
                content = aperture
            )
            if (!exposure_time.isNullOrEmpty()) ImageDetailItem(
                label = "Exposure time",
                content = exposure_time
            )
            if (!iso.isNullOrEmpty()) ImageDetailItem(label = "ISO", content = iso)
        }
    }
}

@Composable
fun ImageDetailItem(
    label: String,
    content: String,
) {
    Spacer(modifier = Modifier.height(Layout.padding.ps))
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Layout.padding.pm)
    ) {
        Text(
            modifier = Modifier.weight(3F),
            color = LocalTextStyle.current.color.copy(0.6F),
            text = label,
            textAlign = TextAlign.End,
        )
        Spacer(modifier = Modifier.width(Layout.padding.pl))
        Text(
            modifier = Modifier.weight(7F),
            text = content,
        )
    }
}

@Composable
fun DeleteView(
    visible: Boolean,
    confirm: () -> Unit = {},
    cancel: () -> Unit = {},
) {
    FlatActionSheet(
        showDialog = visible, onDismissRequest = { cancel() }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(Layout.roundShape.rl)
                    .background(MaterialTheme.colorScheme.surface)
                    .preventPointerInput()
                    .padding(
                        start = Layout.padding.pl,
                        end = Layout.padding.pl,
                        top = Layout.padding.pl,
                    )
                    .align(Alignment.BottomCenter),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(text = "提示", fontSize = Layout.fontSize.fl, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(Layout.padding.pl))
                Text(text = "⛔️ 确认删除图片吗？", fontSize = Layout.fontSize.fl)
                Spacer(modifier = Modifier.height(Layout.padding.pxxl))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Layout.padding.pm)
                ) {
                    ConfirmButton(
                        modifier = Modifier.weight(1F),
                        color = MaterialTheme.colorScheme.onBackground.copy(0.2F),
                        label = "取消"
                    ) {
                        cancel()
                    }
                    ConfirmButton(
                        modifier = Modifier.weight(1F),
                        color = MaterialTheme.colorScheme.error,
                        label = "确认"
                    ) {
                        confirm()
                    }
                }

                Spacer(modifier = Modifier.height(Layout.padding.pl))
                Spacer(modifier = Modifier.navigationBarsPadding())
            }
        }
    }
}

@Composable
fun ConfirmButton(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.surface,
    label: String,
    onClick: () -> Unit,
) {
    Text(
        modifier = modifier
            .clip(Layout.roundShape.rm)
            .background(color)
            .clickable { onClick() }
            .padding(vertical = Layout.padding.pm),
        textAlign = TextAlign.Center,
        fontSize = Layout.fontSize.fl,
        text = label,
    )
}