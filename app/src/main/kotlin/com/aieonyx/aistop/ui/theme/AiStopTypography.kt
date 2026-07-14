// Copyright (c) 2026 Edison Lepiten / AIEONYX
// License: Apache-2.0
package com.aieonyx.aistop.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.aieonyx.aistop.R

// ── Font families ─────────────────────────────────────────────────────────────

val LeagueSpartan = FontFamily(
    Font(R.font.league_spartan_bold,      FontWeight.Bold),
    Font(R.font.league_spartan_extrabold, FontWeight.ExtraBold),
    Font(R.font.league_spartan_black,     FontWeight.Black)
)

val Inter = FontFamily(
    Font(R.font.inter_regular,  FontWeight.Normal),
    Font(R.font.inter_medium,   FontWeight.Medium),
    Font(R.font.inter_semibold, FontWeight.SemiBold),
    Font(R.font.inter_bold,     FontWeight.Bold)
)

// ── Type scale ────────────────────────────────────────────────────────────────

data class AiStopTypography(
    // Display — protection status, hero numbers
    val display: TextStyle = TextStyle(
        fontFamily   = LeagueSpartan,
        fontWeight   = FontWeight.Black,
        fontSize     = 44.sp,
        lineHeight   = 46.sp,
        letterSpacing = (-0.5).sp
    ),
    // H1 — screen titles, major headings
    val h1: TextStyle = TextStyle(
        fontFamily   = LeagueSpartan,
        fontWeight   = FontWeight.ExtraBold,
        fontSize     = 32.sp,
        lineHeight   = 36.sp,
        letterSpacing = (-0.2).sp
    ),
    // H2 — section titles, mode names
    val h2: TextStyle = TextStyle(
        fontFamily   = LeagueSpartan,
        fontWeight   = FontWeight.ExtraBold,
        fontSize     = 24.sp,
        lineHeight   = 28.sp,
        letterSpacing = 0.sp
    ),
    // H3 — tool names, card titles
    val h3: TextStyle = TextStyle(
        fontFamily   = LeagueSpartan,
        fontWeight   = FontWeight.Bold,
        fontSize     = 20.sp,
        lineHeight   = 24.sp,
        letterSpacing = 0.sp
    ),
    // Body — descriptions, detail text
    val body: TextStyle = TextStyle(
        fontFamily   = Inter,
        fontWeight   = FontWeight.Normal,
        fontSize     = 16.sp,
        lineHeight   = 24.sp,
        letterSpacing = 0.1.sp
    ),
    // Body small — supporting info, timestamps
    val bodySmall: TextStyle = TextStyle(
        fontFamily   = Inter,
        fontWeight   = FontWeight.Medium,
        fontSize     = 14.sp,
        lineHeight   = 20.sp,
        letterSpacing = 0.1.sp
    ),
    // Caption — secondary info, metadata
    val caption: TextStyle = TextStyle(
        fontFamily   = Inter,
        fontWeight   = FontWeight.Medium,
        fontSize     = 12.sp,
        lineHeight   = 16.sp,
        letterSpacing = 0.2.sp
    ),
    // Label — badges, buttons, nav tabs (UPPERCASE)
    val label: TextStyle = TextStyle(
        fontFamily   = Inter,
        fontWeight   = FontWeight.Bold,
        fontSize     = 13.sp,
        lineHeight   = 16.sp,
        letterSpacing = 1.sp
    ),
    // Label small — chips, micro labels (UPPERCASE)
    val labelSmall: TextStyle = TextStyle(
        fontFamily   = Inter,
        fontWeight   = FontWeight.Bold,
        fontSize     = 11.sp,
        lineHeight   = 14.sp,
        letterSpacing = 0.8.sp
    )
)

val LocalAiStopTypography = staticCompositionLocalOf { AiStopTypography() }
