#!/usr/bin/env bash
# Copyright (c) 2026 Edison Lepiten / AIEONYX
# License: Apache-2.0
#
# integrate-aistop.sh — Phase M3 integration commit for AI Stop repo.
# Run from ~/projects/aistop after swapping Room → EdisonDB.

set -euo pipefail

# 1. Copy Android SDK files into aistop
SDK_SRC="${SDK_SRC:-../edisondb/mobile/android-sdk}"
SDK_DST="app/src/main/java/com/aieonyx/edisondb"

mkdir -p "${SDK_DST}"
cp "${SDK_SRC}/EdisonDbAndroid.kt"      "${SDK_DST}/"
cp "${SDK_SRC}/ArpiHeader.kt"           "${SDK_DST}/"

mkdir -p "app/src/main/java/com/aieonyx/aistop/db"
cp "${SDK_SRC}/ExposureStore.kt"            "app/src/main/java/com/aieonyx/aistop/db/"
cp "${SDK_SRC}/EdisonDbExposureStore.kt"    "app/src/main/java/com/aieonyx/aistop/db/"

echo "=== SDK files copied ==="

# 2. Build AI Stop with EdisonDB
./gradlew assembleDebug

echo "=== AI Stop build successful ==="

# 3. Commit
git add -A
git commit -m "feat: swap Room ExposureDatabase for EdisonDB mobile SDK

- Replace ExposureDatabase (Room) with EdisonDbAndroid singleton
- EdisonDbExposureStore implements ExposureStore (drop-in swap)
- Every ExposureEvent tagged with ARPi provenance header
- BLAKE3-signed records, AES-256-GCM at rest (Android Keystore)
- Signed export bundle now includes EdisonDB provenance chain
- libeditsondb.so: arm64-v8a + x86_64 from edisondb mobile SDK

Closes: AIEONYX-MOBILE-M3

Copyright (c) 2026 Edison Lepiten / AIEONYX"

git push origin main

echo ""
echo "=== AI Stop integration commit pushed ==="
echo "NLNet evidence: both edisondb and aistop repos updated this week."
