// Copyright (c) 2026 Edison Lepiten / AIEONYX
// License: Apache-2.0
//
// aistop-core: platform-free Rust core.
// No Android/JNI types below the jni_bridge module.
// Same crate serves Android (JNI), iOS (C FFI), desktop.

pub mod auditor;
pub mod scorer;
pub mod pii;
pub mod redact;
pub mod hasher;
pub mod store;
pub mod export;
pub mod jni_bridge;
