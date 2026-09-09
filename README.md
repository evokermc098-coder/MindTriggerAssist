# MindTrigger Assist v16.2

[English](README.md) · [Tiếng Việt](README.vi.md)

Use a ColorOS long press to open Circle to Search or Android Assistant.
Primarily designed for OPPO Find X7 / ColorOS 16 China.

## Requirements

- MindTrigger Assist v16.2;
- Shizuku installed and running;
- Google or Gemini installed for Assistant;
- Wireless debugging or a computer to start Shizuku.

Root is not required. Shizuku is needed only during setup; MindTrigger runs independently afterwards.

## Setup

1. Start **Shizuku** through Wireless debugging or a computer.
2. Open **MindTrigger Assist**, accept the terms and select **Run setup with Shizuku** on the first page.
3. Allow the Shizuku request. Wait for the results table to show `SUCCESS`.
4. Complete the ColorOS steps shown in the wizard:
   - enable **Touch and hold gesture guide bar to wake Breeno**;
   - lock MindTrigger Assist in **Recent tasks**;
   - enable **Auto launch** for Google/Gemini.
5. Select **Run MindTrigger Assist** and allow Android's device-log access request.
6. Test Home/gesture long press for Circle to Search and Power long press for Assistant.

MindTrigger's notification should remain visible while the runtime is running.

## Setup without Shizuku

Select **Copy ADB one-shot** on the first setup page. Connect your phone to a computer,
run the command copied by the app, then return to MindTrigger and complete the ColorOS steps.
Neither ADB nor Shizuku needs to stay connected afterwards.

## Required ColorOS settings

The app cannot reliably read these OEM settings. Enable them manually and confirm them in the wizard.

| Setting | Purpose |
| --- | --- |
| Breeno gesture | Supplies the long-press signal used by MindTrigger. |
| Lock in Recent tasks | Reduces the risk of being stopped by **Clear all**. |
| Google/Gemini Auto launch | Helps Assistant start after being stopped in the background. |
| Display over other apps | Supports the watcher's recovery path on ColorOS. |

On ColorOS 16.0.7, read the red warning at the bottom of **Setup**.
If you have already disabled permission monitoring / System Optimization as described there, you can ignore it.

## If Home / Power stops responding

1. Open MindTrigger Assist once.
2. Allow Android's device-log prompt if it appears.
3. Check the *log session* status in **Setup**, not just `READ_LOGS`.
4. If you enabled **Quick Settings log recovery** in Beta, you can also tap that tile to request recovery.

Having `READ_LOGS` permission does not prove that the log session is alive.
A foreground notification alone does not prove that the reader is working either.

## Before running setup

The first Shizuku setup can uninstall `com.heytap.speechassist` and
`com.coloros.colordirectservice` for user 0 to make the triggers available.
This does not delete their original APKs from the system partition, but it does change your device.
Read the command results and proceed only if you agree.

MindTrigger does not use Device Admin, an Accessibility service, child-process logcat,
runtime Shizuku, or a permanent WakeLock.

## Beta

The **Beta** tab offers animations, voice wake, separate CTS/Assistant sounds capped at
3 seconds, CTS ↔ Google Assistant action swapping, and the log recovery tile.
These are optional; basic setup does not require them.

## Build from source

```sh
./gradlew assembleRelease
```

- Package: `dev.evoker.homeholdcts`
- minSdk: 32
- targetSdk / compileSdk: 36
- Java: 17
- Release APK signing: APK Signature Scheme v3

A release build requires a real keystore through `keystore.properties`.
See [SIGNING.md](SIGNING.md).

## License and source

MindTrigger Assist is distributed under **GPL-3.0-only**. See [LICENSE](LICENSE),
[NOTICE.md](NOTICE.md), [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md), and
[SOURCE_PROVENANCE.md](SOURCE_PROVENANCE.md).

Repository: <https://github.com/evokermc098-coder/MindTriggerAssist>

## Credits

- Development and maintenance: **@EvokerUniverse**.
- Coding assistance: **ChatGPT / Codex**.
- CTS reference code: **[MiCTS / parallelcc](https://github.com/parallelcc/MiCTS)**, GPL-3.0; the CTS invocation path is treated as upstream-derived.
- UI and icons: **Google Material Components / Material Icons**, Apache-2.0.
- Setup library: **Shizuku API / RikkaW**, MIT.
- Hidden Android API access: **AndroidHiddenApiBypass / LSPosed**, Apache-2.0.
- Audio: **Claude Code**, at the author's direction; see [AUDIO_PROVENANCE.md](AUDIO_PROVENANCE.md).

Attribution does not imply endorsement. APK distribution requires corresponding source and preservation of copyright notices. See [LICENSE_AUDIT.md](LICENSE_AUDIT.md) and [TERMS_AND_PRIVACY.md](TERMS_AND_PRIVACY.md).
