// Copyright (c) 2026 Edison Lepiten / AIEONYX
// License: Apache-2.0
//
// M1 PermissionAuditor — parse permission lists, produce risk profiles.
// Package detection happens in Kotlin (PackageManager / <queries>).
// This module receives the results and classifies them.

use crate::scorer::{compute_trust, AppRiskProfile, TrustResult};
use serde::{Deserialize, Serialize};

#[derive(Debug, Clone, Serialize, Deserialize)]
pub enum RiskLevel {
    Low,
    Medium,
    High,
    Critical,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct PermissionEntry {
    pub name: String,
    pub level: RiskLevel,
}

/// Classify a single Android permission string into a risk level.
pub fn classify_permission(perm: &str) -> RiskLevel {
    match perm {
        p if p.contains("RECORD_AUDIO")
            || p.contains("READ_CONTACTS")
            || p.contains("ACCESS_FINE_LOCATION") =>
        {
            RiskLevel::Critical
        }

        p if p.contains("READ_CLIPBOARD")
            || p.contains("ACCESS_COARSE_LOCATION")
            || p.contains("READ_CALL_LOG")
            || p.contains("CAMERA") =>
        {
            RiskLevel::High
        }

        p if p.contains("READ_EXTERNAL_STORAGE")
            || p.contains("RECEIVE_SMS")
            || p.contains("INTERNET") =>
        {
            RiskLevel::Medium
        }

        _ => RiskLevel::Low,
    }
}

/// Parse a JSON array of permission strings, return classified list.
pub fn classify_permissions(perms_json: &str) -> Result<Vec<PermissionEntry>, String> {
    let perms: Vec<String> = serde_json::from_str(perms_json).map_err(|e| e.to_string())?;
    let entries = perms
        .into_iter()
        .map(|p| {
            let level = classify_permission(&p);
            PermissionEntry { name: p, level }
        })
        .collect();
    Ok(entries)
}

/// Full audit pipeline: classify permissions, score trust, return JSON.
pub fn audit_app(profile: &AppRiskProfile) -> TrustResult {
    compute_trust(profile)
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn microphone_is_critical() {
        assert!(matches!(
            classify_permission("android.permission.RECORD_AUDIO"),
            RiskLevel::Critical
        ));
    }

    #[test]
    fn internet_is_medium() {
        assert!(matches!(
            classify_permission("android.permission.INTERNET"),
            RiskLevel::Medium
        ));
    }

    #[test]
    fn unknown_permission_is_low() {
        assert!(matches!(
            classify_permission("com.some.custom.PERMISSION"),
            RiskLevel::Low
        ));
    }
}
