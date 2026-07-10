// Copyright (c) 2026 Edison Lepiten / AIEONYX
// License: Apache-2.0
//
// RegexDetector — v1.1 implementation of PiiDetector trait.
// Compiled once at startup via once_cell::sync::Lazy.

use once_cell::sync::Lazy;
use regex::Regex;
use super::detector::{DetectionReport, DetectionResult, PiiDetector, PiiMatch};
use super::rules::RULES;

static COMPILED: Lazy<Vec<(super::detector::PiiClass, Regex)>> = Lazy::new(|| {
    RULES.iter().filter_map(|rule| {
        match Regex::new(rule.pattern) {
            Ok(re) => Some((rule.class.clone(), re)),
            Err(_) => None,
        }
    }).collect()
});

pub struct RegexDetector;

impl PiiDetector for RegexDetector {
    fn detect(&self, text: &str) -> DetectionResult {
        if text.is_empty() {
            return DetectionResult::NoPiiFound;
        }
        let mut matches: Vec<PiiMatch> = Vec::new();
        for (class, re) in COMPILED.iter() {
            for m in re.find_iter(text) {
                let raw = &text[m.start()..m.end()];
                matches.push(PiiMatch {
                    class:  class.clone(),
                    start:  m.start(),
                    end:    m.end(),
                    masked: mask(raw, class),
                });
            }
        }
        if matches.is_empty() {
            DetectionResult::NoPiiFound
        } else {
            DetectionResult::PiiFound(DetectionReport {
                matches,
                char_count: text.chars().count(),
            })
        }
    }
}

fn mask(raw: &str, class: &super::detector::PiiClass) -> String {
    use super::detector::PiiClass::*;
    match class {
        Email => {
            if let Some(at) = raw.find('@') {
                let (local, domain) = raw.split_at(at);
                let masked_local = if local.len() > 2 {
                    format!("{}••••", &local[..2])
                } else {
                    "••••".into()
                };
                let masked_domain = domain.chars().enumerate().map(|(i, c)| {
                    if i < 3 || c == '.' { c } else { '•' }
                }).collect::<String>();
                format!("{}{}", masked_local, masked_domain)
            } else {
                "••••@••••".into()
            }
        }
        Phone => {
            let digits: String = raw.chars().filter(|c| c.is_ascii_digit()).collect();
            if digits.len() > 4 {
                format!("{}••• ••• {}", &raw[..2], &digits[digits.len()-3..])
            } else {
                "•••••".into()
            }
        }
        ApiKey => {
            if raw.len() > 8 {
                format!("{}••••••••{}", &raw[..4], &raw[raw.len()-4..])
            } else {
                "••••••••".into()
            }
        }
        _ => {
            let visible = (raw.len() / 3).max(2).min(4);
            format!("{}••••", &raw[..visible.min(raw.len())])
        }
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    fn detect(text: &str) -> DetectionResult {
        RegexDetector.detect(text)
    }

    #[test]
    fn detects_email() {
        let r = detect("Contact me at john@example.com please");
        assert!(matches!(r, DetectionResult::PiiFound(_)));
    }

    #[test]
    fn detects_api_key() {
        let r = detect("My key is sk-abcdefghijklmnopqrstuvwxyz123456");
        assert!(matches!(r, DetectionResult::PiiFound(_)));
    }

    #[test]
    fn no_pii_clean_text() {
        let r = detect("The weather in Prague is cloudy today.");
        assert!(matches!(r, DetectionResult::NoPiiFound));
    }

    #[test]
    fn empty_text_no_pii() {
        let r = detect("");
        assert!(matches!(r, DetectionResult::NoPiiFound));
    }

    #[test]
    fn detects_phone() {
        let r = detect("Call me at +420 555 014 822 anytime");
        assert!(matches!(r, DetectionResult::PiiFound(_)));
    }

    #[test]
    fn masked_email_format() {
        let masked = mask("john@example.com",
            &super::super::detector::PiiClass::Email);
        assert!(masked.contains("••••"));
        assert!(!masked.contains("john"));
    }
}
