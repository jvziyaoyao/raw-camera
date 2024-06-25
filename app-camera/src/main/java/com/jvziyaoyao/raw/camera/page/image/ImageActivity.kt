package com.jvziyaoyao.raw.camera.page.image

import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.jvziyaoyao.raw.camera.base.BaseActivity
import com.jvziyaoyao.raw.camera.base.CheckButton
import com.jvziyaoyao.raw.camera.base.DynamicStatusBarColor
import com.jvziyaoyao.raw.camera.base.EditorBar
import com.jvziyaoyao.raw.camera.base.EditorNav
import com.jvziyaoyao.raw.camera.base.ImageGrid
import com.jvziyaoyao.raw.camera.base.ScrollBarContainer
import com.jvziyaoyao.raw.camera.base.getTimeLabel
import com.jvziyaoyao.raw.camera.base.rememberSelectedItemsState
import com.jvziyaoyao.raw.camera.domain.model.CameraPhotoEntity
import com.jvziyaoyao.raw.camera.domain.model.MediaQueryEntity
import com.jvziyaoyao.raw.camera.ui.theme.Layout
import com.jvziyaoyao.raw.camera.util.shareItems
import com.jvziyaoyao.scale.zoomable.previewer.PreviewerState
import com.jvziyaoyao.scale.zoomable.previewer.VerticalDragType
import com.jvziyaoyao.scale.zoomable.previewer.rememberPreviewerState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel

class ImageActivity : BaseActivity() {

    private val mViewModel by viewModel<ImageViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setBasicContent {
            DynamicStatusBarColor(dark = false)
            ImageBody(
                onBack = {
                    finish()
                }
            )
        }

        launch(Dispatchers.IO) {
            mViewModel.fetchImages()
        }
    }

}

@Composable
fun ImageBody(
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val viewModel: ImageViewModel = koinViewModel()
    val selectedItemsState = rememberSelectedItemsState<MediaQueryEntity> {
        it.path ?: ""
    }
    val previewList = viewModel.imageList
    val cameraPhotoList = remember {
        derivedStateOf {
            filterCameraPhotoList(previewList)
        }
    }
    val previewerState = rememberPreviewerState(
        verticalDragType = VerticalDragType.Down,
        pageCount = { previewList.size },
        getKey = { previewList[it].path!! },
    )

    selectedItemsState.apply {
        fun selectedAll() {
            selectedItems(previewList)
        }
        BackHandler(enabled = selectedMode) {
            cancelSelected()
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .animateContentSize()
                    .statusBarsPadding()
            ) {
                if (selectedMode) {
                    EditorNav(
                        titleContent = {
                            Text(
                                text = if (selectedSize == 0) "Select item" else "$selectedSize photo(s) selected",
                                textAlign = TextAlign.Center,
                            )
                        },
                        selectedAll = selectedSize == previewList.size,
                        onCancel = {
                            cancelSelected()
                        },
                        onAll = {
                            selectedAll()
                        },
                        onNone = {
                            selectNone()
                        }
                    )
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                horizontal = Layout.padding.ps,
                                vertical = Layout.padding.pxxs,
                            ),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        IconButton(onClick = onBack) {
                            Icon(imageVector = Icons.Filled.ArrowBack, contentDescription = null)
                        }
                        Text(
                            text = "图片",
                            fontSize = Layout.fontSize.fl,
                        )
                    }
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1F)
            ) {
                ImagePhotosPage(
                    list = cameraPhotoList.value,
                    selectedMap = selectedMap as Map<String, MediaQueryEntity>,
                    selectedMode = selectedMode,
                    previewerState = previewerState,
                    onPreview = { },
                    onSelected = { list ->
                        selectedItems(list)
                    },
                    onUnselected = { list ->
                        unselectItems(list)
                    }
                )
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .animateContentSize()
            ) {
                if (selectedMode) {
                    EditorBar(
                        onShare = {
                            scope.launch {
                                try {
                                    val selectedList = getSelectedList()
                                    shareItems(context, selectedList)
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                        },
                        onDelete = {
                            scope.launch {
                                try {
                                    val selectedList = getSelectedList()
                                    // TODO 实际删除操作
                                    cancelSelected()
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                        }
                    )
                    Spacer(modifier = Modifier.navigationBarsPadding())
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ImagePhotosPage(
    list: List<CameraPhotoEntity>,
    selectedMap: Map<String, MediaQueryEntity>,
    selectedMode: Boolean,
    previewerState: PreviewerState,
    onPreview: (MediaQueryEntity) -> Unit,
    onSelected: (List<MediaQueryEntity>) -> Unit,
    onUnselected: (List<MediaQueryEntity>) -> Unit,
) {
    val columns = 4
    val titleHeight = 54.dp
    val contentTypeTitle = 0
    val contentTypeBlock = 1
    val lazyGridState = rememberLazyGridState()

    ImageScrollBarContainer(
        columns = columns,
        titleHeight = titleHeight,
        lazyGridState = lazyGridState,
        list = list
    ) {
        LazyVerticalGrid(
            modifier = Modifier
                .fillMaxSize(),
            columns = GridCells.Fixed(columns),
            state = lazyGridState,
        ) {
            list.forEach { cameraPhotoEntity ->
                item(contentType = contentTypeTitle, key = cameraPhotoEntity.timeStamp, span = {
                    GridItemSpan(this.maxLineSpan)
                }) {
                    val timeLabel = remember { getTimeLabel(cameraPhotoEntity.timeStamp) }
                    val selectedAll = remember(selectedMap, selectedMap.size, selectedMode) {
                        for (mediaQueryEntity in cameraPhotoEntity.list) {
                            val path = mediaQueryEntity.path ?: ""
                            if (selectedMap[path] == null) {
                                return@remember false
                            }
                        }
                        return@remember true
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(titleHeight),
                        contentAlignment = Alignment.BottomStart,
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(bottom = Layout.padding.pxs)
                                .pointerInput(selectedMode, selectedAll, selectedMap) {
                                    detectTapGestures {
                                        if (selectedMode) {
                                            if (selectedAll) {
                                                onUnselected(cameraPhotoEntity.list)
                                            } else {
                                                onSelected(cameraPhotoEntity.list)
                                            }
                                        }
                                    }
                                },
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Row(modifier = Modifier.animateContentSize()) {
                                if (selectedMode) {
                                    Spacer(modifier = Modifier.width(Layout.padding.ps))
                                    CheckButton(
                                        check = selectedAll,
                                        checkColor = MaterialTheme.colorScheme.primary,
                                        uncheckColor = MaterialTheme.colorScheme.onBackground.copy(
                                            0.4F
                                        ),
                                    )
                                }
                            }
                            Text(
                                modifier = Modifier
                                    .padding(start = Layout.padding.ps),
                                text = timeLabel
                            )
                        }
                    }
                }
                cameraPhotoEntity.list.forEachIndexed { index, mediaQueryEntity ->
                    val needStart = index % columns != 0
                    val path = mediaQueryEntity.path ?: ""
                    val selected = selectedMap[path] != null
                    item(key = path, contentType = contentTypeBlock) {
                        Box(
                            modifier = Modifier
                                .animateItemPlacement()
                                .fillMaxWidth()
                                .aspectRatio(1F)
                                .padding(start = if (needStart) 2.dp else 0.dp, bottom = 2.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            ImageGrid(
                                mediaQueryEntity = mediaQueryEntity,
                                selectedMode = selectedMode,
                                selected = selected,
                                previewerState = previewerState,
                                onPress = onPress@{
                                    onPreview(mediaQueryEntity)
                                },
                                onLongPress = {
                                    onSelected(listOf(mediaQueryEntity))
                                },
                                onSelected = {
                                    onSelected(listOf(mediaQueryEntity))
                                },
                                onUnselected = {
                                    onUnselected(listOf(mediaQueryEntity))
                                }
                            )
                        }
                    }
                }
            }
            // 一个额外的padding
            commonPaddingBottom()
        }
    }
}

fun LazyGridScope.commonPaddingBottom(height: Dp = 100.dp) {
    item(span = {
        GridItemSpan(this.maxLineSpan)
    }) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(height)
        )
    }
}

@Composable
fun ImageScrollBarContainer(
    columns: Int,
    titleHeight: Dp,
    lazyGridState: LazyGridState,
    list: List<CameraPhotoEntity>,
    content: @Composable () -> Unit,
) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        // 下面是滚动的逻辑
        val density = LocalDensity.current
        val blockHeight = remember { density.run { maxWidth.toPx().div(columns) } }
        val positionMap = remember { mutableMapOf<Int, Float>() }
        val indexTimeMap = remember { mutableMapOf<Int, Long?>() }
        var totalHeight by remember { mutableStateOf(0F) }

        LaunchedEffect(key1 = list, key2 = list.size) {
            val titleHeightPx = density.run { titleHeight.toPx() }
            var itemIndex = -1
            positionMap.clear()
            indexTimeMap.clear()
            totalHeight = 0F
            list.forEach { cameraPhotoEntity ->
                itemIndex++
                positionMap[itemIndex] = totalHeight
                indexTimeMap[itemIndex] = cameraPhotoEntity.timeStamp
                totalHeight += titleHeightPx
                cameraPhotoEntity.list.forEachIndexed { index, mediaQueryEntity ->
                    itemIndex++
                    positionMap[itemIndex] = totalHeight
                    indexTimeMap[itemIndex] = mediaQueryEntity.timeStamp
                    if ((index + 1) % columns == 0 || index + 1 == cameraPhotoEntity.list.size) {
                        totalHeight += blockHeight
                    }
                }
            }
        }

        ScrollBarContainer(
            totalHeight = totalHeight,
            index2PositionMap = positionMap,
            index2TimeMap = indexTimeMap,
            lazyGridState = lazyGridState,
        ) {
            content()
        }
    }
}