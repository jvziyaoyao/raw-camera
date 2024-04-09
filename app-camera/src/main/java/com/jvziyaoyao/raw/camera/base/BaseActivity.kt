package com.jvziyaoyao.raw.camera.base

import android.content.res.Configuration
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.core.view.WindowCompat
import com.jvziyaoyao.raw.camera.ui.theme.RawCameraTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel

/**
 * @program: WePrompter
 *
 * @description:
 *
 * @author: JVZIYAOYAO
 *
 * @create: 2023-07-08 23:15
 **/

open class BaseActivity : ComponentActivity(), CoroutineScope by MainScope() {

    fun setBasicContent(
        darkIcons: Boolean = true,
        content: @Composable () -> Unit,
    ) {
        val systemBarStyle = when (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) {
            Configuration.UI_MODE_NIGHT_NO -> SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT)
            Configuration.UI_MODE_NIGHT_YES -> SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
            else -> SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT)
        }
        enableEdgeToEdge(
            statusBarStyle = systemBarStyle,
            navigationBarStyle = systemBarStyle,
        )
        setContent {
            SideEffect {
                WindowCompat.getInsetsController(window,window.decorView).isAppearanceLightStatusBars = darkIcons
            }
            RawCameraTheme {
                val contentColor = MaterialTheme.colorScheme.onBackground.copy(0.8F)
                CompositionLocalProvider(
                    LocalTextStyle provides LocalTextStyle.current.copy(color = contentColor),
                    LocalContentColor provides contentColor,
                ) {
                    BasePopupPage {
                        content()
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        cancel()
        super.onDestroy()
    }

}