package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.example.model.KidTheme

private val ParentLightColorScheme = lightColorScheme(
    primary = IndigoPrimary,
    onPrimary = IndigoOnPrimary,
    primaryContainer = IndigoPrimaryContainer,
    onPrimaryContainer = IndigoOnPrimaryContainer,
    secondary = SlateSecondary,
    background = SlateBackground,
    surface = SlateSurface,
    surfaceVariant = SlateSurfaceVariant,
    error = RoseDanger
)

private val ParentDarkColorScheme = darkColorScheme(
    primary = IndigoPrimaryDark,
    onPrimary = IndigoOnPrimaryDark,
    primaryContainer = IndigoPrimaryContainerDark,
    onPrimaryContainer = IndigoOnPrimaryContainerDark,
    secondary = SlateSecondary,
    background = SlateBackgroundDark,
    surface = SlateSurfaceDark,
    surfaceVariant = SlateSurfaceVariantDark,
    error = RoseDanger
)

@Composable
fun KidLockTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    kidTheme: KidTheme? = null,
    isChildMode: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        isChildMode && kidTheme != null -> {
            darkColorScheme(
                primary = kidTheme.primaryColor,
                secondary = kidTheme.secondaryColor,
                tertiary = kidTheme.accentColor,
                background = kidTheme.backgroundColor,
                surface = kidTheme.surfaceColor,
                onPrimary = Color.White,
                onBackground = Color.White,
                onSurface = Color.White
            )
        }
        darkTheme -> ParentDarkColorScheme
        else -> ParentLightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
