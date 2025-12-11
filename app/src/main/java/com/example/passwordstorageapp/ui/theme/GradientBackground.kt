package com.example.passwordstorageapp.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.luminance

@Composable
fun GradientBackground(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    // Infer dark / light based on current theme background
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    val brush = if (isDark) {
        Brush.verticalGradient(
            colors = listOf(
                DarkBackground,   // near-black top
                GradientStart,
                GradientEnd       // deep blue bottom
            )
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(
                LightGradientStart,
                LightGradientMid,
                LightGradientEnd
            )
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(brush)
    ) {
        content()
    }
}
