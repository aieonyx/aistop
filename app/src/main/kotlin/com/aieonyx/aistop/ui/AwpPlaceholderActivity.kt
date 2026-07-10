// Copyright (c) 2026 Edison Lepiten / AIEONYX
// License: Apache-2.0
package com.aieonyx.aistop.ui

import android.os.Bundle
import androidx.activity.ComponentActivity

/**
 * AWP placeholder — no-op scheme handler.
 * awp:// registered in manifest for v2.0 cross-device sync.
 * No functionality in v1.1.
 */
class AwpPlaceholderActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        finish() // no-op, just consumes the intent
    }
}
