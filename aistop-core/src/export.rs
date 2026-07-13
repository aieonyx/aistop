// Copyright (c) 2026 Edison Lepiten / AIEONYX
// License: Apache-2.0
//
// Export pipeline — serialises ExposureEvents into signed report JSON.
// Signing split: Rust hashes, Kotlin/Keystore signs, Rust assembles block.
// Schema version is non-negotiable — it is the v2.0 AWP/EdisonDB hinge.

use crate::hasher::{assemble_signature_block, blake3_hash_str};
use crate::store::ExposureEvent;
use serde::{Deserialize, Serialize};

#[derive(Debug, Serialize, Deserialize)]
pub struct ExportPayload {
    pub schema: &'static str,
    pub device_pubkey: String,
    pub exported_at: String,
    pub events: Vec<ExposureEvent>,
}

/// Step 1: build the payload JSON and compute BLAKE3 hash.
/// Returns (payload_json, hash_hex) — hash is passed to Kotlin for signing.
pub fn prepare_export(
    events: &[ExposureEvent],
    device_pubkey: &str,
    exported_at: &str,
) -> Result<(String, String), String> {
    let payload = ExportPayload {
        schema: "aistop.exposure.v1",
        device_pubkey: device_pubkey.to_string(),
        exported_at: exported_at.to_string(),
        events: events.to_vec(),
    };
    let payload_json = serde_json::to_string(&payload).map_err(|e| e.to_string())?;
    let hash_hex = blake3_hash_str(&payload_json);
    Ok((payload_json, hash_hex))
}

/// Step 2: called after Kotlin returns Ed25519 signature.
/// Assembles and returns the final signed report JSON.
pub fn finalise_export(
    payload_json: &str,
    hash_hex: &str,
    signature_hex: &str,
) -> Result<String, String> {
    assemble_signature_block(payload_json, hash_hex, signature_hex)
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::store::EventType;

    fn dummy_event() -> ExposureEvent {
        ExposureEvent {
            id: Some(1),
            ts: 1_700_000_000_000,
            package: "com.openai.chatgpt".into(),
            app_label: "ChatGPT".into(),
            event_type: EventType::PasteBlocked,
            preview_20: "Hi, my name is John".into(),
            trust_score: 28,
            pii_classes: vec!["NAME".into(), "EMAIL".into()],
        }
    }

    #[test]
    fn prepare_returns_payload_and_hash() {
        let (payload, hash) = prepare_export(
            &[dummy_event()],
            "pubkey_fingerprint",
            "2026-08-02T00:00:00Z",
        )
        .unwrap();
        assert!(payload.contains("aistop.exposure.v1"));
        assert_eq!(hash.len(), 64); // BLAKE3 hex
    }

    #[test]
    fn finalise_produces_valid_json() {
        let (payload, hash) =
            prepare_export(&[dummy_event()], "pubkey", "2026-08-02T00:00:00Z").unwrap();
        let final_block = finalise_export(&payload, &hash, "fake_sig_hex").unwrap();
        let parsed: serde_json::Value = serde_json::from_str(&final_block).unwrap();
        assert_eq!(parsed["schema"], "aistop.exposure.v1");
        assert_eq!(parsed["signature"], "fake_sig_hex");
    }
}
