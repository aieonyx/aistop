// Copyright (c) 2026 Edison Lepiten / AIEONYX
// License: Apache-2.0
package com.aieonyx.aistop.ui.theme

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember

// ── Theme entry point ─────────────────────────────────────────────────────────

@Composable
fun AiStopTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colors = remember(darkTheme) {
        if (darkTheme) AiStopDarkColors else AiStopLightColors
    }
    val typography = remember { AiStopTypography() }

    CompositionLocalProvider(
        LocalAiStopColors     provides colors,
        LocalAiStopTypography provides typography,
        content = content
    )
}

// ── Convenience accessors ─────────────────────────────────────────────────────

object AiStopTheme {
    val colors: AiStopColors
        @Composable get() = LocalAiStopColors.current
    val typography: AiStopTypography
        @Composable get() = LocalAiStopTypography.current
}

// ── Theme preference helpers ──────────────────────────────────────────────────

private const val PREFS_THEME = "aistop_prefs"
private const val KEY_DARK    = "dark_mode"

fun saveDarkMode(context: Context, dark: Boolean) {
    context.getSharedPreferences(PREFS_THEME, Context.MODE_PRIVATE)
        .edit().putBoolean(KEY_DARK, dark).apply()
}

fun loadDarkMode(context: Context): Boolean =
    context.getSharedPreferences(PREFS_THEME, Context.MODE_PRIVATE)
        .getBoolean(KEY_DARK, true) // default dark
