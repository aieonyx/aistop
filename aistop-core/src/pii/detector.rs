// Copyright (c) 2026 Edison Lepiten / AIEONYX
// License: Apache-2.0

use serde::{Deserialize, Serialize};

/// PII entity classes detected by AI Stop.
#[derive(Debug, Clone, Serialize, Deserialize, PartialEq)]
pub enum PiiClass {
    FullName,
    Email,
    Phone,
    DateOfBirth,
    Address,
    IdNumber,
    ApiKey,
    CreditCard,
    IpAddress,
    CryptoWallet,
    Financial,
    GovernmentId,
    Location,
    DeviceId,
    Custom(String),
}

/// A single matched PII span in the input text.
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct PiiMatch {
    pub class:      PiiClass,
    pub start:      usize,
    pub end:        usize,
    pub masked:     String,  // e.g. "ed••••@gm•••.com" — shown in IME sheet
}

/// Full detection report returned to Kotlin via JNI.
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct DetectionReport {
    pub matches:    Vec<PiiMatch>,
    pub char_count: usize,
}

/// Top-level result — includes Unavailable for null InputConnection.
/// v1.1: Rust must never panic on null/restricted InputConnection.
#[derive(Debug, Clone, Serialize, Deserialize)]
pub enum DetectionResult {
    PiiFound(DetectionReport),
    NoPiiFound,
    Unavailable,  // null or restricted InputConnection — pass through safely
}

/// Pluggable detector trait.
/// v1.1: RegexDetector
/// v2.0: ModelDetector (quantized IAM-lite on-device)
pub trait PiiDetector: Send + Sync {
    fn detect(&self, text: &str) -> DetectionResult;
}
