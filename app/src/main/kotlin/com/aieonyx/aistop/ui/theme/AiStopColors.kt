// Copyright (c) 2026 Edison Lepiten / AIEONYX
// License: Apache-2.0
package com.aieonyx.aistop.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

@Immutable
data class AiStopColors(
    val background: Color,
    val surface: Color,
    val surface2: Color,
    val accentPrimary: Color,
    val accentSecondary: Color,
    val danger: Color,
    val warning: Color,
    val success: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val outline: Color,
    val divider: Color,
    val disabled: Color,
    val onPrimary: Color,
    val onSignal: Color,
    val primaryContainer: Color,
    val secondaryContainer: Color,
    val successContainer: Color,
    val warningContainer: Color,
    val dangerContainer: Color,
    val isDark: Boolean
)

val AiStopDarkColors = AiStopColors(
    background       = Color(0xFF09111F),
    surface          = Color(0xFF111C2E),
    surface2         = Color(0xFF18263C),
    accentPrimary    = Color(0xFF3D63FF),
    accentSecondary  = Color(0xFF00C2FF),
    danger           = Color(0xFFFF3B4E),
    warning          = Color(0xFFFFAC12),
    success          = Color(0xFF00D084),
    textPrimary      = Color(0xFFF7FAFF),
    textSecondary    = Color(0xFFA9B8CF),
    outline          = Color(0xFF344761),
    divider          = Color(0xFF26374F),
    disabled         = Color(0xFF66758C),
    onPrimary        = Color(0xFFFFFFFF),
    onSignal         = Color(0xFF06111A),
    primaryContainer  = Color(0xFF172B68),
    secondaryContainer= Color(0xFF073A4D),
    successContainer  = Color(0xFF0B382D),
    warningContainer  = Color(0xFF422F08),
    dangerContainer   = Color(0xFF431821),
    isDark           = true
)

val AiStopLightColors = AiStopColors(
    background       = Color(0xFFE9EFF7),
    surface          = Color(0xFFFFFFFF),
    surface2         = Color(0xFFDCE6F2),
    accentPrimary    = Color(0xFF244DDB),
    accentSecondary  = Color(0xFF007FA8),
    danger           = Color(0xFFD5002D),
    warning          = Color(0xFF9A5A00),
    success          = Color(0xFF007C50),
    textPrimary      = Color(0xFF09111F),
    textSecondary    = Color(0xFF40526C),
    outline          = Color(0xFFAAB8C9),
    divider          = Color(0xFFCAD4E1),
    disabled         = Color(0xFF7B8796),
    onPrimary        = Color(0xFFFFFFFF),
    onSignal         = Color(0xFF06111A),
    primaryContainer  = Color(0xFFDCE5FF),
    secondaryContainer= Color(0xFFD4F2FC),
    successContainer  = Color(0xFFD6F5E8),
    warningContainer  = Color(0xFFFFF0C7),
    dangerContainer   = Color(0xFFFFE0E5),
    isDark           = false
)

val LocalAiStopColors = staticCompositionLocalOf { AiStopDarkColors }
