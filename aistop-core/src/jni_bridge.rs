// Copyright (c) 2026 Edison Lepiten / AIEONYX
// License: Apache-2.0
//
// JNI bridge — the ONLY place Android/JNI types appear in this crate.
// All business logic lives in the other modules (platform-free).
// Keep this shim thin. Every function here is a thin wrapper only.

#![allow(non_snake_case)]

use jni::JNIEnv;
use jni::objects::{JClass, JString};
use jni::sys::jstring;

use crate::pii::regex_detector::RegexDetector;
use crate::pii::detector::PiiDetector;
use crate::scorer::compute_trust_batch;
use crate::hasher::{blake3_hash_str, assemble_signature_block};
use crate::export::{prepare_export, finalise_export};

// ── helpers ──────────────────────────────────────────────────────────────────

fn jstring_to_str(env: &mut JNIEnv, s: JString) -> String {
    env.get_string(&s).map(|j| j.into()).unwrap_or_default()
}

fn str_to_jstring(env: &mut JNIEnv, s: &str) -> jstring {
    env.new_string(s).map(|j| j.into_raw()).unwrap_or_else(|_| std::ptr::null_mut())
}

// ── M6 TrustScorer ───────────────────────────────────────────────────────────

#[no_mangle]
pub extern "system" fn Java_com_aieonyx_aistop_jni_AiStopCore_trustComputeBatch(
    mut env: JNIEnv, _class: JClass, profiles_json: JString,
) -> jstring {
    let input = jstring_to_str(&mut env, profiles_json);
    let result = compute_trust_batch(&input).unwrap_or_else(|e| {
        format!(r#"{{"error":"{}"}}"#, e)
    });
    str_to_jstring(&mut env, &result)
}

// ── M2/M3 PII Detection ──────────────────────────────────────────────────────

#[no_mangle]
pub extern "system" fn Java_com_aieonyx_aistop_jni_AiStopCore_piiDetect(
    mut env: JNIEnv, _class: JClass, text: JString,
) -> jstring {
    let input = jstring_to_str(&mut env, text);
    let detector = RegexDetector;
    let result = detector.detect(&input);
    let json = serde_json::to_string(&result).unwrap_or_else(|e| {
        format!(r#"{{"error":"{}"}}"#, e)
    });
    str_to_jstring(&mut env, &json)
}

// ── M5 SovereignIdentity — split signing ─────────────────────────────────────
// Step 1: Rust hashes. Hash bytes cross JNI to Kotlin.
// Step 2: Kotlin Keystore signs the hash.
// Step 3: Signature bytes return to Rust via this bridge.
// Step 4: Rust assembles final SignatureBlock.

#[no_mangle]
pub extern "system" fn Java_com_aieonyx_aistop_jni_AiStopCore_blake3Hash(
    mut env: JNIEnv, _class: JClass, payload: JString,
) -> jstring {
    let input = jstring_to_str(&mut env, payload);
    let hash = blake3_hash_str(&input);
    str_to_jstring(&mut env, &hash)
}

#[no_mangle]
pub extern "system" fn Java_com_aieonyx_aistop_jni_AiStopCore_assembleSignatureBlock(
    mut env: JNIEnv, _class: JClass,
    payload_json: JString, hash_hex: JString, signature_hex: JString,
) -> jstring {
    let payload   = jstring_to_str(&mut env, payload_json);
    let hash      = jstring_to_str(&mut env, hash_hex);
    let signature = jstring_to_str(&mut env, signature_hex);
    let result = assemble_signature_block(&payload, &hash, &signature)
        .unwrap_or_else(|e| format!(r#"{{"error":"{}"}}"#, e));
    str_to_jstring(&mut env, &result)
}

// ── Export ───────────────────────────────────────────────────────────────────

#[no_mangle]
pub extern "system" fn Java_com_aieonyx_aistop_jni_AiStopCore_prepareExport(
    mut env: JNIEnv, _class: JClass,
    events_json: JString, device_pubkey: JString, exported_at: JString,
) -> jstring {
    let events_str = jstring_to_str(&mut env, events_json);
    let pubkey     = jstring_to_str(&mut env, device_pubkey);
    let ts         = jstring_to_str(&mut env, exported_at);

    let events: Vec<crate::store::ExposureEvent> =
        serde_json::from_str(&events_str).unwrap_or_default();

    let result = match prepare_export(&events, &pubkey, &ts) {
        Ok((payload, hash)) => format!(
            r#"{{"payload":{},"hash":"{}"}}"#,
            serde_json::to_string(&payload).unwrap_or_default(),
            hash
        ),
        Err(e) => format!(r#"{{"error":"{}"}}"#, e),
    };
    str_to_jstring(&mut env, &result)
}

#[no_mangle]
pub extern "system" fn Java_com_aieonyx_aistop_jni_AiStopCore_finaliseExport(
    mut env: JNIEnv, _class: JClass,
    payload_json: JString, hash_hex: JString, signature_hex: JString,
) -> jstring {
    let payload   = jstring_to_str(&mut env, payload_json);
    let hash      = jstring_to_str(&mut env, hash_hex);
    let signature = jstring_to_str(&mut env, signature_hex);
    let result = finalise_export(&payload, &hash, &signature)
        .unwrap_or_else(|e| format!(r#"{{"error":"{}"}}"#, e));
    str_to_jstring(&mut env, &result)
}
