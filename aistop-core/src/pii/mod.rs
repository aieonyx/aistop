// Copyright (c) 2026 Edison Lepiten / AIEONYX
// License: Apache-2.0
//
// PII detection pipeline.
// Trait-pluggable: RegexDetector (v1.1) → ModelDetector (v2.0 IAM-lite).

pub mod detector;
pub mod regex_detector;
pub mod rules;

pub use detector::{DetectionResult, DetectionReport, PiiMatch, PiiClass};
pub use regex_detector::RegexDetector;
