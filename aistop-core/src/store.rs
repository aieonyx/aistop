// Copyright (c) 2026 Edison Lepiten / AIEONYX
// License: Apache-2.0
//
// ExposureStore trait — swappable storage backend.
// v1.1: RoomStore (Android Room via JNI)
// v2.0: EdisonDbStore (mobile EdisonDB backend, no core rewrite needed)

use serde::{Deserialize, Serialize};

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct ExposureEvent {
    pub id:          Option<i64>,
    pub ts:          i64,       // epoch ms
    pub package:     String,
    pub app_label:   String,
    pub event_type:  EventType,
    pub preview_20:  String,    // first 20 chars only — NEVER full content
    pub trust_score: u8,
    pub pii_classes: Vec<String>, // JSON-serialised PiiClass names
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub enum EventType {
    PasteAllowed,
    PasteBlocked,
    PasteRedacted,
    ScrubShare,
    ClipAutoClear,
}

#[derive(Debug, Clone, Default)]
pub struct LogFilter {
    pub event_type: Option<EventType>,
    pub since_ts:   Option<i64>,
    pub limit:      Option<usize>,
}

/// Pluggable storage backend trait.
pub trait ExposureStore: Send + Sync {
    fn insert(&self, event: &ExposureEvent) -> Result<i64, String>;
    fn query(&self, filter: &LogFilter) -> Result<Vec<ExposureEvent>, String>;
    fn purge_before(&self, ts: i64) -> Result<usize, String>;
    fn count(&self) -> Result<usize, String>;
}
