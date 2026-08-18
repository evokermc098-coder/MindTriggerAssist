# MindTrigger Assist — v16.0.0 RC1

MindTrigger Assist is an open-source ColorOS compatibility utility that maps a
configured long-press Home/gesture signal to Circle to Search and can route a
Power long press to the active Android Assistant voice session.

Package: `dev.evoker.homeholdcts`
Project repository: https://github.com/evokermc098-coder/MindTriggerAssist

## Runtime architecture

```text
ColorOS long-press signal
    ↓
SpeechAssist service-start failure observed in selected logcat tags
    ↓
dev.evoker.homeholdcts:watcher
    ├─ HomeHoldService foreground service
    ├─ trigger classifier / debounce
    └─ child logcat reader
          ↓
ActivationRunner
    ├─ Home/gesture → CtsProtocol → CTS-specific voice-interaction bundle
    └─ Power        → CtsProtocol → active Assistant voice session
```

`CtsProtocol` obtains Android's `voiceinteraction` binder service and calls the
hidden `IVoiceInteractionManagerService.showSessionFromSession(...)` interface.
The CTS branch adds the CTS-specific invocation arguments; the Assistant branch
does not.

## Device-log session recovery

`READ_LOGS` and the privileged logcat access session are intentionally modeled as
separate states:

- `READ_LOGS` is a package-level permission and remains granted across reboot.
- privileged logcat access is session-scoped;
- Android may require its own device-log access confirmation for a new session;
- MindTrigger Assist does not bypass that confirmation.

The long-lived watcher runs in the private `:watcher` process. If an ACTIVE
logcat session is lost, `LogSessionBridgeActivity` can briefly place the app UID
in a foreground state so Android can display its own confirmation over the app
currently in use. The bridge is transparent, excluded from Recents, and removed
after recovery/timeout.

After reboot, if the Android confirmation does not appear automatically, open
MindTrigger Assist once.

## Setup

### Privileged setup

Shizuku or the generated PC one-shot command is used to:

- grant `android.permission.READ_LOGS`;
- configure the Google voice-interaction service as the Android default assistant;
- apply the listed Doze / standby / AppOps background settings;
- on the Shizuku first-run path only, run `pm uninstall --user 0` for
  `com.heytap.speechassist` and `com.coloros.colordirectservice`.

The latter commands remove those packages from user 0; they do not erase the
system-partition APKs.

Normal Circle to Search activation does not require Shizuku to remain connected
after privileged setup.

### ColorOS manual requirements

MindTrigger Assist cannot reliably query several OEM-only ColorOS states. The UI
therefore requires explicit confirmation for:

- the ColorOS long-press gesture entry point;
- locking MindTrigger Assist in Recent Tasks;
- Google/Gemini Auto launch.

The app does not claim to detect these states when no reliable public interface
is available.

## Release languages

The v16 release selector contains only fully maintained locale packs:

- `vi-VN` — Tiếng Việt
- `en-US` — English
- `id-ID` — Bahasa Indonesia
- `th-TH` — ไทย

All supported non-English packs contain the complete canonical UI string set.
Technical identifiers, package names, Android API names, and license identifiers
remain untranslated where translating them would make troubleshooting less
precise. Unsupported device locales fall back to English.


## Appearance

Material 3 appearance modes:

- White
- System
- Dark
- Night

Theme/color changes recreate the Activity while preserving the currently selected
bottom-navigation tab. Selecting a theme from About therefore remains on About
instead of returning to Setup.

## Audio assets

Bundled activation sounds:

- `app/src/main/res/raw/aura_cts.wav`
- `app/src/main/res/raw/aura_gemini.wav`

Known project provenance records them as generated with Claude Code at the
project author's direction and integrated as local PCM WAV assets. See
`AUDIO_PROVENANCE.md` for format, hashes, and the conservative rights statement.

## Build configuration

- Android Gradle Plugin: 8.13.2
- compileSdk: 36
- targetSdk: 36
- minSdk: 32
- Java: 17
- Material Components: 1.14.0
- Shizuku API/provider: 13.1.5
- AndroidHiddenApiBypass: 6.1
- APK signing policy: v1 + v2 + v3 enabled, v4 disabled

Release builds require a real keystore via `keystore.properties` or the documented
environment variables. See `SIGNING.md`.

## License

MindTrigger Assist is distributed under **GNU GPL-3.0-only**.

The Circle to Search invocation path was implemented with reference to the public
MiCTS project and is treated as an upstream-derived GPL path for release and
attribution purposes. Direct permissive dependencies retain their own notices.

See:

- `LICENSE` — complete GPLv3 text
- `NOTICE.md` — upstream/modification notice
- `SOURCE_PROVENANCE.md` — implementation provenance
- `THIRD_PARTY_NOTICES.md` — direct third-party components
- `LICENSE_AUDIT.md` — release compliance audit
- `AUDIO_PROVENANCE.md` — bundled sound provenance
- `TERMS_AND_PRIVACY.md` — privacy / redistribution notice

If an APK or other object-code build is distributed, satisfy the applicable GPL
corresponding-source obligations for that build and preserve required notices.

## Credits

- Developer: **@EvokerUniverse**
- Maintainer: **@EvokerUniverse**
- AI coding assistance: **Chat GPT / Codex**
- CTS upstream reference: **MiCTS / parallelcc**
- Audio generation provenance: **Claude Code**, at the project author's direction

## Release validation

Run:

```sh
python tools/release_sanity.py
```

The v16 check validates package/version metadata, locale completeness, stale
release wording, required license/notice files, audio hashes, and critical
runtime-file invariants that should not change during release polishing.
