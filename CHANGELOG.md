# Changelog

## v16.0.0 RC1 — repository-link sync

- Updated the in-app GitHub action to open the official MindTrigger Assist repository:
  `https://github.com/evokermc098-coder/MindTriggerAssist`.
- Added the same repository URL to `README.md`.
- Increased `versionCode` to `16000002` for the rebuild; `versionName` remains `v16.0.0-rc1`.


## v16.0.0 RC1 — 2026-08-18

### Release polish

- Rebuilt localization around four fully maintained release locales: vi-VN,
  en-US, id-ID and th-TH. Removed partial preview locale packs.
- Theme and accent changes now preserve the current bottom-navigation tab across
  Activity recreation instead of returning to Setup.
- Reworked user-facing terminology around READ_LOGS, privileged logcat sessions,
  VoiceInteractionManager, the Assistant route, foreground services and the Log
  Session Bridge.
- Changed AI credit to `Chat GPT` without exposing a model variant.
- Replaced topic-specific redistribution wording with a neutral added-content
  responsibility/non-association notice that does not restrict GPL rights.
- Added an in-app open-source license viewer with bundled GPL-3.0, Apache-2.0,
  Shizuku API MIT, MiCTS and AndroidHiddenApiBypass notices.
- Added direct dependency license audit and complete third-party notices.
- Documented Claude Code audio provenance, WAV properties and SHA-256 hashes.
- Preserved v2.1 watcher / transparent log-session bridge runtime behavior.

### Source hardening

- Made the internal `RestartReceiver` non-exported; system and same-app restart broadcasts remain supported.
- Removed the unrelated `USE_FULL_SCREEN_INTENT` AppOp from privileged setup.
- Added `SECURITY_SOURCE_AUDIT.md` covering component exposure, local privileged operations, network/telemetry absence, and audit limits.

### Build metadata

- package: `dev.evoker.homeholdcts`
- versionCode: `16000002`
- versionName: `v16.0.0-rc1`
- compileSdk / targetSdk: 36
- minSdk: 32
- APK signature schemes: v1 + v2 + v3
