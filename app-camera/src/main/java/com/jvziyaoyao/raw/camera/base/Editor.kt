package com.jvziyaoyao.raw.camera.base

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import com.jvziyaoyao.raw.camera.ui.theme.Layout
import java.util.function.Function
import java.util.stream.Collectors

/**
 * @program: ImageGallery
 *
 * @description:
 *
 * @author: JVZIYAOYAO
 *
 * @create: 2023-05-23 11:18
 **/

class SelectedItemsState<T>(
    val getKey: (T) -> Any
) {

    var selectedMode by mutableStateOf(false)

    val selectedMap = mutableStateMapOf<Any, T>()

    val selectedSize: Int
        get() = selectedMap.size

    fun getSelectedList(): List<T> {
        return selectedMap.toList().stream().map { it.second }
            .collect(Collectors.toList())
    }

    fun selectedItems(mediaQueryEntities: List<T>) {
        if (!selectedMode) selectedMode = true
        val additionalMap = mediaQueryEntities.stream()
            .collect(Collectors.toMap({ getKey(it) }, Function.identity()))
        selectedMap.putAll(additionalMap)
    }

    fun unselectItems(mediaQueryEntities: List<T>) {
        mediaQueryEntities.stream()
            .map { getKey(it) }
            .collect(Collectors.toList())
            .forEach {
                selectedMap.remove(it)
            }
    }

    fun cancelSelected() {
        selectedMode = false
        selectedMap.clear()
    }

    fun selectNone() {
        selectedMap.clear()
    }
}

@Composable
fun <T> rememberSelectedItemsState(getKey: (T) -> Any): SelectedItemsState<T> {
    return remember {
        SelectedItemsState(getKey)
    }
}

@Composable
fun EditorNav(
    selectedAll: Boolean,
    titleContent: @Composable () -> Unit = {},
    onCancel: () -> Unit,
    onAll: () -> Unit,
    onNone: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Layout.padding.pl, vertical = Layout.padding.ps),
    ) {
        val density = LocalDensity.current
        var titleHeight by remember { mutableStateOf(0) }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(density.run { titleHeight.toDp() })
                .animateContentSize(),
            contentAlignment = Alignment.Center
        ) {
            titleContent()
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .onGloballyPositioned {
                    titleHeight = it.size.height
                },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MainButton(onClick = {
                onCancel()
            }) {
                Text(
                    text = "Cancel",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Box(
                modifier = Modifier.weight(1F),
            )
            if (selectedAll) {
                MainButton(onClick = {
                    onNone()
                }) {
                    Text(
                        text = "None",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            } else {
                MainButton(onClick = {
                    onAll()
                }) {
                    Text(
                        text = "All",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
fun EditorAlbumBar(
    onOther: () -> Unit,
    onHide: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Layout.padding.pl, vertical = Layout.padding.ps),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceAround,
    ) {
        ForegroundTabMenuItem(
            icon = Icons.Outlined.MoveUp,
            label = "other",
            color = MaterialTheme.colorScheme.onSurface,
        ) {
            onOther()
        }
        ForegroundTabMenuItem(
            icon = Icons.Outlined.HideImage,
            label = "hide",
            color = MaterialTheme.colorScheme.onSurface,
        ) {
            onHide()
        }
        ForegroundTabMenuItem(
            icon = Icons.Outlined.Nature,
            label = "Rename",
            color = MaterialTheme.colorScheme.onSurface,
        ) {
            onRename()
        }
        ForegroundTabMenuItem(
            icon = Icons.Outlined.Delete,
            label = "Delete",
            color = MaterialTheme.colorScheme.error,
        ) {
            onDelete()
        }
    }
}

@Composable
fun EditorBar(
    onShare: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Layout.padding.pl, vertical = Layout.padding.ps),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceAround,
    ) {
        ForegroundTabMenuItem(
            icon = Icons.Outlined.Share,
            label = "Share",
            color = MaterialTheme.colorScheme.onBackground,
        ) {
            onShare()
        }
        ForegroundTabMenuItem(
            icon = Icons.Outlined.Delete,
            label = "Delete",
            color = MaterialTheme.colorScheme.error,
        ) {
            onDelete()
        }
    }
}