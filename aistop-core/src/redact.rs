// Copyright (c) 2026 Edison Lepiten / AIEONYX
// License: Apache-2.0
//
// Redaction engine — replaces PII matches with stable tokens.
// Token mapping stored locally, encrypted, session-expiring (Kotlin layer).
// restore() reverses tokens in AI reply text.

use crate::pii::detector::{DetectionReport, PiiClass};
use serde::{Deserialize, Serialize};

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct RedactedOutput {
    pub text: String,
    pub mapping: Vec<TokenMapping>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct TokenMapping {
    pub token: String,    // e.g. "[NAME_1]"
    pub original: String, // original value — kept in encrypted local store
    pub class: String,
}

/// Redact all PII matches in text, return redacted string + mapping.
pub fn redact(text: &str, report: &DetectionReport) -> RedactedOutput {
    // Sort matches by start position descending so we can replace without offset shift
    let mut matches = report.matches.clone();
    matches.sort_by_key(|b| std::cmp::Reverse(b.start));

    let mut result = text.to_string();
    let mut mapping: Vec<TokenMapping> = Vec::new();
    let mut counters: std::collections::HashMap<String, usize> = std::collections::HashMap::new();

    for m in &matches {
        let class_key = class_key(&m.class);
        let counter = counters.entry(class_key.clone()).or_insert(0);
        *counter += 1;
        let token = format!("[{}_{}]", class_key, counter);
        let original = result[m.start..m.end].to_string();
        result.replace_range(m.start..m.end, &token);
        mapping.push(TokenMapping {
            token,
            original,
            class: class_key,
        });
    }

    RedactedOutput {
        text: result,
        mapping,
    }
}

/// Restore tokens in AI reply text using the saved mapping.
pub fn restore(text: &str, mapping: &[TokenMapping]) -> String {
    let mut result = text.to_string();
    // Restore in order — longer tokens first to avoid partial replacement
    let mut sorted = mapping.to_vec();
    sorted.sort_by_key(|b| std::cmp::Reverse(b.token.len()));
    for m in &sorted {
        result = result.replace(&m.token, &m.original);
    }
    result
}

fn class_key(class: &PiiClass) -> String {
    match class {
        PiiClass::FullName => "NAME",
        PiiClass::Email => "EMAIL",
        PiiClass::Phone => "PHONE",
        PiiClass::DateOfBirth => "DOB",
        PiiClass::Address => "ADDRESS",
        PiiClass::IdNumber => "ID",
        PiiClass::ApiKey => "API_KEY",
        PiiClass::CreditCard => "CARD",
        PiiClass::IpAddress => "IP",
        PiiClass::CryptoWallet => "CRYPTO",
        PiiClass::Financial => "FINANCIAL",
        PiiClass::GovernmentId => "GOV_ID",
        PiiClass::Location => "LOCATION",
        PiiClass::DeviceId => "DEVICE_ID",
        PiiClass::Custom(s) => return s.to_uppercase(),
    }
    .to_string()
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::pii::detector::PiiMatch;

    fn make_report(matches: Vec<PiiMatch>) -> DetectionReport {
        DetectionReport {
            matches,
            char_count: 100,
        }
    }

    #[test]
    fn redacts_email() {
        let text = "Email john@example.com please";
        let report = make_report(vec![PiiMatch {
            class: PiiClass::Email,
            start: 6,
            end: 22,
            masked: "jo••••@ex•••.com".into(),
        }]);
        let out = redact(text, &report);
        assert!(out.text.contains("[EMAIL_1]"));
        assert!(!out.text.contains("john@example.com"));
        assert_eq!(out.mapping[0].original, "john@example.com");
    }

    #[test]
    fn restore_reverses_redaction() {
        let text = "Email john@example.com here";
        let report = make_report(vec![PiiMatch {
            class: PiiClass::Email,
            start: 6,
            end: 22,
            masked: "".into(),
        }]);
        let redacted = redact(text, &report);
        let restored = restore(&redacted.text, &redacted.mapping);
        assert!(restored.contains("john@example.com"));
    }

    #[test]
    fn multiple_same_class_get_numbered_tokens() {
        let text = "a@a.com and b@b.com";
        let report = make_report(vec![
            PiiMatch {
                class: PiiClass::Email,
                start: 0,
                end: 7,
                masked: "".into(),
            },
            PiiMatch {
                class: PiiClass::Email,
                start: 12,
                end: 19,
                masked: "".into(),
            },
        ]);
        let out = redact(text, &report);
        assert!(out.text.contains("[EMAIL_1]") || out.text.contains("[EMAIL_2]"));
    }
}
