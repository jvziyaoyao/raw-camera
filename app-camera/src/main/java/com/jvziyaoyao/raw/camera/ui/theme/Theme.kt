package com.jvziyaoyao.raw.camera.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

val DarkColorScheme = darkColorScheme(
    primary = PrimaryDarkFull,
    secondary = SecondaryDarkFull,

    secondaryContainer = SecondaryDarkContainerFull,
    tertiaryContainer = TertiaryDarkContainerFull,
    background = BackgroundDarkFull,
    onBackground = FontDarkFull,
    surface = LightDarkFull,
    error = ErrorDarkFull,
)

val LightColorScheme = lightColorScheme(
    primary = PrimaryFull,
    secondary = SecondaryFull,

    secondaryContainer = SecondaryContainerFull,
    tertiaryContainer = TertiaryContainerFull,
    background = BackgroundFull,
    onBackground = FontFull,
    surface = LightFull,
    error = ErrorFull,
)
@Composable
fun RawCameraTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}