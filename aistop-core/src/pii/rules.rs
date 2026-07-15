// Copyright (c) 2026 Edison Lepiten / AIEONYX
// License: Apache-2.0
//
// Bundled PII regex rules for v1.1 RegexDetector.
// v2.0: these rules become the fallback layer under IAM-lite model.

use super::detector::PiiClass;

pub struct Rule {
    pub class: PiiClass,
    pub pattern: &'static str,
}

/// Core rules shipped in v1.1 APK.
/// Ordered by specificity — more specific rules first to reduce false positives.
pub const RULES: &[Rule] = &[
    Rule {
        class: PiiClass::ApiKey,
        // OpenAI sk- prefix, GitHub tokens, generic hex secrets
        pattern: r"(?i)(sk-[a-zA-Z0-9]{32,}|ghp_[a-zA-Z0-9]{36}|[a-fA-F0-9]{40,})",
    },
    Rule {
        class: PiiClass::Email,
        pattern: r"[a-zA-Z0-9._%+\-]+@[a-zA-Z0-9.\-]+\.[a-zA-Z]{2,}",
    },
    Rule {
        class: PiiClass::CreditCard,
        // Check before Phone — card numbers match phone pattern
        pattern: r"\b(?:4[0-9]{3}[\s\-]?[0-9]{4}[\s\-]?[0-9]{4}[\s\-]?[0-9]{4}|5[1-5][0-9]{2}[\s\-]?[0-9]{4}[\s\-]?[0-9]{4}[\s\-]?[0-9]{4}|3[47][0-9]{2}[\s\-]?[0-9]{6}[\s\-]?[0-9]{5})\b",
    },
    Rule {
        class: PiiClass::Phone,
        // International formats: +420 555 014 822, (416)-555-0198, etc.
        pattern: r"(\+\d[\d\s\-().]{7,}\d|\(\d{2,4}\)[\d\s\-().]{5,}\d)",
    },
    Rule {
        class: PiiClass::DateOfBirth,
        // Common date formats
        pattern: r"\b(\d{1,2}[\/\-\.]\d{1,2}[\/\-\.]\d{2,4}|\d{4}[\/\-\.]\d{1,2}[\/\-\.]\d{1,2})\b",
    },
    Rule {
        class: PiiClass::IpAddress,
        pattern: r"\b(?:\d{1,3}\.){3}\d{1,3}\b",
    },
    // FullName and Address are intentionally heuristic — lower confidence.
    // These fire only when combined with other signals in v1.1.
    // Full named entity recognition deferred to v2.0 IAM-lite model.
    // ── API Keys & Developer Secrets ─────────────────────────────────────────
    Rule {
        class: PiiClass::ApiKey,
        // OpenAI API key
        pattern: r"sk-[a-zA-Z0-9]{32,}",
    },
    Rule {
        class: PiiClass::ApiKey,
        // OpenAI project key
        pattern: r"sk-proj-[a-zA-Z0-9_-]{32,}",
    },
    Rule {
        class: PiiClass::ApiKey,
        // Anthropic API key
        pattern: r"sk-ant-[a-zA-Z0-9_-]{32,}",
    },
    Rule {
        class: PiiClass::ApiKey,
        // GitHub personal access token (classic)
        pattern: r"ghp_[a-zA-Z0-9]{20,}",
    },
    Rule {
        class: PiiClass::ApiKey,
        // GitHub fine-grained token
        pattern: r"github_pat_[a-zA-Z0-9_]{82}",
    },
    Rule {
        class: PiiClass::ApiKey,
        // AWS access key
        pattern: r"AKIA[0-9A-Z]{16}",
    },
    Rule {
        class: PiiClass::ApiKey,
        // AWS secret key (context-based)
        pattern: r#"(?i)aws.{0,20}secret.{0,20}[a-zA-Z0-9/+=]{40}"#,
    },
    Rule {
        class: PiiClass::ApiKey,
        // Google API key
        pattern: r"AIza[0-9A-Za-z_-]{35}",
    },
    Rule {
        class: PiiClass::ApiKey,
        // Stripe secret key
        pattern: r"sk_live_[a-zA-Z0-9]{24,}",
    },
    Rule {
        class: PiiClass::ApiKey,
        // Stripe publishable key
        pattern: r"pk_live_[a-zA-Z0-9]{24,}",
    },
    Rule {
        class: PiiClass::ApiKey,
        // Hugging Face token
        pattern: r"hf_[a-zA-Z0-9]{34,}",
    },
    Rule {
        class: PiiClass::ApiKey,
        // Slack bot/user token
        pattern: r"xox[baprs]-[0-9a-zA-Z-]{10,}",
    },
    Rule {
        class: PiiClass::ApiKey,
        // Generic Bearer token in Authorization header
        pattern: r"(?i)bearer\s+[a-zA-Z0-9_\-\.]{20,}",
    },
    Rule {
        class: PiiClass::ApiKey,
        // JWT token (three base64 segments)
        pattern: r"eyJ[a-zA-Z0-9_-]{10,}\.[a-zA-Z0-9_-]{10,}\.[a-zA-Z0-9_-]{10,}",
    },
    Rule {
        class: PiiClass::ApiKey,
        // PEM private key block
        pattern: r"-----BEGIN (?:RSA |EC |OPENSSH |DSA )?PRIVATE KEY-----",
    },
    Rule {
        class: PiiClass::ApiKey,
        // .env style secrets
        pattern: r#"(?i)(?:password|secret|token|api_key|apikey|passwd|pwd)\s*=\s*[a-zA-Z0-9_\-]{8,}"#,
    },
    // ── Crypto ────────────────────────────────────────────────────────────────
    Rule {
        class: PiiClass::CryptoWallet,
        // Bitcoin address (P2PKH, P2SH, Bech32)
        pattern: r"\b(?:1[a-km-zA-HJ-NP-Z1-9]{25,34}|3[a-km-zA-HJ-NP-Z1-9]{25,34}|bc1[a-z0-9]{39,59})\b",
    },
    Rule {
        class: PiiClass::CryptoWallet,
        // Ethereum address
        pattern: r"\b0x[a-fA-F0-9]{40}\b",
    },
    Rule {
        class: PiiClass::CryptoWallet,
        // BIP-39 seed phrase (12 consecutive known words — simplified detection)
        pattern: r"(?i)\b(?:abandon|ability|able|about|above|absent|absorb|abstract|absurd|abuse|access|accident|account|accuse|achieve|acid|acoustic|acquire|across|act|action|actor|actress|actual|adapt|add|addict|address|adjust|admit|adult|advance|advice|aerobic|afford|afraid|again|age|agent|agree|ahead|aim|air|airport|aisle|alarm|album|alcohol|alert|alien|all|alley|allow|almost|alone|alpha|already|also|alter|always|amateur|amazing|among|amount|amused|analyst|anchor|ancient|anger|angle|angry|animal|ankle|announce|annual|answer|antenna|antique|anxiety|any|apart|apology|appear|apple|approve|april|arch|arctic|area|arena|argue|arm|armed|armor|army|around|arrange|arrest|arrive|arrow|art|artefact|artist|artwork|ask|aspect|assault|asset|assist|assume|asthma|athlete|atom|attack|attend|attitude|attract|auction|audit|august|aunt|author|auto|autumn|average|avocado|avoid|awake|aware|away|awesome|awful|awkward|axis)\b(?:\s+\w+){10,23}",
    },
    // ── Financial ─────────────────────────────────────────────────────────────
    Rule {
        class: PiiClass::Financial,
        // IBAN (EU bank account)
        pattern: r"\b[A-Z]{2}[0-9]{2}[A-Z0-9]{4}[0-9]{7}(?:[A-Z0-9]{0,16})?\b",
    },
    Rule {
        class: PiiClass::Financial,
        // SWIFT/BIC code
        pattern: r"\b[A-Z]{6}[A-Z0-9]{2}(?:[A-Z0-9]{3})?\b",
    },
    // ── Government IDs ────────────────────────────────────────────────────────
    Rule {
        class: PiiClass::GovernmentId,
        // US Social Security Number
        pattern: r"\b\d{3}-\d{2}-\d{4}\b",
    },
    Rule {
        class: PiiClass::GovernmentId,
        // Czech rodné číslo (birth number)
        pattern: r"\b\d{2}(?:0[1-9]|1[0-2]|5[1-9]|6[0-2])\d{2}[\/]?\d{3,4}\b",
    },
    Rule {
        class: PiiClass::GovernmentId,
        // Passport number (generic)
        pattern: r"\b[A-Z]{1,2}[0-9]{6,9}\b",
    },
    // ── Location ─────────────────────────────────────────────────────────────
    Rule {
        class: PiiClass::Location,
        // GPS coordinates (decimal)
        pattern: r"[0-9]{1,3}[.][0-9]{2,}[,][ ][0-9]{1,3}[.][0-9]{2,}",
    },
    // ── Device identifiers ────────────────────────────────────────────────────
    Rule {
        class: PiiClass::DeviceId,
        // MAC address
        pattern: r"[0-9a-fA-F]{2}:[0-9a-fA-F]{2}:[0-9a-fA-F]{2}:[0-9a-fA-F]{2}:[0-9a-fA-F]{2}:[0-9a-fA-F]{2}",
    },
    Rule {
        class: PiiClass::DeviceId,
        // IMEI
        pattern: r"[0-9]{15}",
    },
];
