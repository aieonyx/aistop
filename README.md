# AI Stop — Sovereign AI Guard
**Copyright (c) 2026 Edison Lepiten / AIEONYX · Apache-2.0**

Your words belong to you — not to a model.

AISeal (Android 17) protects the room. AI Stop guards the door.

---

## Status
**Phase 1 scaffold** — implementation begins Aug 2, 2026.
CI must be green before any phase is declared complete.

## Architecture
See `~/nlnet-evidence/aistop/ARCHITECTURE_v1.1_FINAL.md`

## Build Requirements
- Rust stable (pinned via `aistop-core/rust-toolchain.toml`)
- Android NDK 26.3.11579264 (pinned in CI)
- cargo-ndk 3.5.4 (pinned in CI)
- JDK 17
- Android Studio Hedgehog or later

## Build

```bash
# 1. Build Rust core for Android targets
cd aistop-core
cargo ndk -t arm64-v8a -t x86_64 -o ../app/src/main/jniLibs build --release

# 2. Run Rust tests (must pass before any phase close)
cargo test

# 3. Build Android APK
cd ..
./gradlew assembleDebug
```

## Doctrine
- Apache-2.0 license on all public code
- No AI model names in source
- Copyright: "Copyright (c) 2026 Edison Lepiten / AIEONYX"
- Post Doctrine 5-check gate before every phase close
- CI must be green before any phase is declared complete
- NLNet evidence artifacts: local only at `~/nlnet-evidence/`

## Developer
AIEONYX · aieonyx.com · github.com/aieonyx
