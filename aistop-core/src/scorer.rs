// Copyright (c) 2026 Edison Lepiten / AIEONYX
// License: Apache-2.0
//
// M6 TrustScorer — platform-free trust computation.
// Weights: permissions 40%, retention 30%, transparency 20%, opt-out 10%.
// Bands: Red <40 / Amber 40-69 / Green 70+.

use serde::{Deserialize, Serialize};

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct AppRiskProfile {
    pub package: String,
    pub permissions_score: u8, // 0-100 already weighted
    pub retention_score: u8,
    pub transparency_score: u8,
    pub opt_out_score: u8,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct TrustResult {
    pub package: String,
    pub score: u8,
    pub band: TrustBand,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub enum TrustBand {
    Red,   // < 40
    Amber, // 40-69
    Green, // 70+
}

/// Compute trust score from a risk profile.
/// Returns 0-100 where higher = more trustworthy.
pub fn compute_trust(profile: &AppRiskProfile) -> TrustResult {
    let score = (profile.permissions_score as f32 * 0.40
        + profile.retention_score as f32 * 0.30
        + profile.transparency_score as f32 * 0.20
        + profile.opt_out_score as f32 * 0.10)
        .round() as u8;

    let band = match score {
        0..=39 => TrustBand::Red,
        40..=69 => TrustBand::Amber,
        _ => TrustBand::Green,
    };

    TrustResult {
        package: profile.package.clone(),
        score,
        band,
    }
}

/// Parse a JSON array of AppRiskProfile, compute scores, return JSON array of TrustResult.
pub fn compute_trust_batch(profiles_json: &str) -> Result<String, String> {
    let profiles: Vec<AppRiskProfile> =
        serde_json::from_str(profiles_json).map_err(|e| e.to_string())?;
    let results: Vec<TrustResult> = profiles.iter().map(compute_trust).collect();
    serde_json::to_string(&results).map_err(|e| e.to_string())
}

#[cfg(test)]
mod tests {
    use super::*;

    fn profile(p: u8, r: u8, t: u8, o: u8) -> AppRiskProfile {
        AppRiskProfile {
            package: "test.pkg".into(),
            permissions_score: p,
            retention_score: r,
            transparency_score: t,
            opt_out_score: o,
        }
    }

    #[test]
    fn red_band_low_scores() {
        let result = compute_trust(&profile(20, 20, 20, 20));
        assert!(result.score < 40);
        assert!(matches!(result.band, TrustBand::Red));
    }

    #[test]
    fn green_band_high_scores() {
        let result = compute_trust(&profile(90, 90, 90, 90));
        assert!(result.score >= 70);
        assert!(matches!(result.band, TrustBand::Green));
    }

    #[test]
    fn amber_band_mid_scores() {
        let result = compute_trust(&profile(50, 50, 50, 50));
        assert!(result.score >= 40 && result.score < 70);
        assert!(matches!(result.band, TrustBand::Amber));
    }

    #[test]
    fn weights_sum_correctly() {
        // All maxed out should give 100
        let result = compute_trust(&profile(100, 100, 100, 100));
        assert_eq!(result.score, 100);
    }
}
