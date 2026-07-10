// Copyright (c) 2026 Edison Lepiten / AIEONYX
// License: Apache-2.0
//
// BLAKE3 hashing — AIEONYX hash standard (replaces SHA-256 everywhere).
// Used by M5 SovereignIdentity export signing pipeline:
//   Rust: blake3_hash(payload) → hash bytes
//   JNI:  hash bytes cross boundary to Kotlin
//   Kotlin: Keystore Ed25519 sign(hash) → signature bytes
//   JNI:  signature returns to Rust
//   Rust: assemble_signature_block { payload, hash, signature }

/// Hash arbitrary bytes with BLAKE3. Returns 32-byte digest as hex string.
pub fn blake3_hash(input: &[u8]) -> String {
    let hash = blake3::hash(input);
    hash.to_hex().to_string()
}

/// Hash a UTF-8 string payload. Returns hex digest.
pub fn blake3_hash_str(input: &str) -> String {
    blake3_hash(input.as_bytes())
}

/// Assemble the final SignatureBlock JSON.
/// Called after Kotlin/Keystore returns the Ed25519 signature bytes.
pub fn assemble_signature_block(
    payload_json: &str,
    hash_hex:     &str,
    signature_hex: &str,
) -> Result<String, String> {
    let block = serde_json::json!({
        "schema":        "aistop.exposure.v1",
        "hash":          hash_hex,
        "signature":     signature_hex,
        "payload":       payload_json,
    });
    serde_json::to_string(&block).map_err(|e| e.to_string())
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn hash_is_deterministic() {
        let a = blake3_hash_str("hello sovereign");
        let b = blake3_hash_str("hello sovereign");
        assert_eq!(a, b);
    }

    #[test]
    fn hash_is_64_hex_chars() {
        let h = blake3_hash_str("test");
        assert_eq!(h.len(), 64);
    }

    #[test]
    fn different_inputs_different_hashes() {
        let a = blake3_hash_str("input A");
        let b = blake3_hash_str("input B");
        assert_ne!(a, b);
    }

    #[test]
    fn assemble_block_contains_schema() {
        let block = assemble_signature_block(
            r#"{"events":[]}"#, "aabbcc", "ddeeff"
        ).unwrap();
        assert!(block.contains("aistop.exposure.v1"));
        assert!(block.contains("aabbcc"));
    }
}
