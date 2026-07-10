// Copyright (c) 2026 Edison Lepiten / AIEONYX
// License: Apache-2.0
//
// Bundled PII regex rules for v1.1 RegexDetector.
// v2.0: these rules become the fallback layer under IAM-lite model.

use super::detector::PiiClass;

pub struct Rule {
    pub class:   PiiClass,
    pub pattern: &'static str,
}

/// Core rules shipped in v1.1 APK.
/// Ordered by specificity — more specific rules first to reduce false positives.
pub const RULES: &[Rule] = &[
    Rule {
        class:   PiiClass::ApiKey,
        // OpenAI sk- prefix, GitHub tokens, generic hex secrets
        pattern: r"(?i)(sk-[a-zA-Z0-9]{32,}|ghp_[a-zA-Z0-9]{36}|[a-fA-F0-9]{40,})",
    },
    Rule {
        class:   PiiClass::Email,
        pattern: r"[a-zA-Z0-9._%+\-]+@[a-zA-Z0-9.\-]+\.[a-zA-Z]{2,}",
    },
    Rule {
        class:   PiiClass::Phone,
        // International formats: +420 555 014 822, (416)-555-0198, etc.
        pattern: r"(\+?\d[\d\s\-().]{7,}\d)",
    },
    Rule {
        class:   PiiClass::CreditCard,
        pattern: r"\b(?:4[0-9]{12}(?:[0-9]{3})?|5[1-5][0-9]{14}|3[47][0-9]{13})\b",
    },
    Rule {
        class:   PiiClass::DateOfBirth,
        // Common date formats
        pattern: r"\b(\d{1,2}[\/\-\.]\d{1,2}[\/\-\.]\d{2,4}|\d{4}[\/\-\.]\d{1,2}[\/\-\.]\d{1,2})\b",
    },
    Rule {
        class:   PiiClass::IpAddress,
        pattern: r"\b(?:\d{1,3}\.){3}\d{1,3}\b",
    },
    // FullName and Address are intentionally heuristic — lower confidence.
    // These fire only when combined with other signals in v1.1.
    // Full named entity recognition deferred to v2.0 IAM-lite model.
];
