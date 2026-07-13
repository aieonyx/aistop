// Copyright (c) 2026 Edison Lepiten / AIEONYX
// License: Apache-2.0
package com.aieonyx.aistop.ui

import android.content.Context

enum class SovereignMode(
    val id:          String,
    val displayName: String,
    val badge:       String,
    val tagline:     String,
    val detail:      String
) {
    AUTOPILOT(
        id          = "autopilot",
        displayName = "AutoPilot",
        badge       = "✈ AUTOPILOT",
        tagline     = "Silent. Total. Automatic.",
        detail      = "AI Stop intercepts, scrubs, and logs everything silently. No prompts. Maximum protection, zero friction."
    ),
    DEFAULT(
        id          = "default",
        displayName = "Default",
        badge       = "◉ DEFAULT",
        tagline     = "Smart protection, balanced.",
        detail      = "High-confidence threats are blocked automatically. Lower-confidence items are logged only. No interruptions unless it's clearly sensitive."
    ),
    MANUAL(
        id          = "manual",
        displayName = "Manual",
        badge       = "⚙ MANUAL",
        tagline     = "You decide. Every time.",
        detail      = "When sensitive data is detected, AI Stop asks you — block, redact, or allow. Full visibility. Full control."
    );

    companion object {
        fun fromId(id: String): SovereignMode =
            entries.firstOrNull { it.id == id } ?: DEFAULT
    }
}

private const val PREFS_NAME = "aistop_prefs"
private const val KEY_MODE   = "sovereign_mode"

fun saveSovereignMode(context: Context, mode: SovereignMode) {
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .edit().putString(KEY_MODE, mode.id).apply()
}

fun loadSovereignMode(context: Context): SovereignMode =
    SovereignMode.fromId(
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_MODE, SovereignMode.DEFAULT.id) ?: SovereignMode.DEFAULT.id
    )
