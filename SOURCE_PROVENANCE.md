# Source provenance

## MindTrigger Assist-specific implementation

MindTrigger Assist-specific work includes:

- ColorOS long-press Home/gesture signal detection;
- isolated `:watcher` foreground-service lifecycle;
- privileged logcat access/session state handling;
- transparent `LogSessionBridgeActivity` recovery flow;
- trigger classification/debounce and activation orchestration;
- Google/Gemini readiness handling;
- Shizuku / PC one-shot setup;
- ColorOS Recent Tasks and OEM setup guidance;
- Material 3 UI, theme handling and localization.

MindTrigger Assist modifications are attributed to @EvokerUniverse.
AI coding assistance is credited as Chat GPT.

## CTS invocation bridge

The stable Circle to Search invocation path was implemented with reference to
the public MiCTS project and is therefore treated as an upstream-derived GPL
path for licensing and attribution purposes.

The source intentionally preserves protocol elements required by the tested CTS
behavior, including the `voiceinteraction` binder route and CTS invocation
arguments. Cosmetic renaming is not used as a substitute for license compliance
or provenance disclosure.

Upstream: https://github.com/parallelcc/MiCTS

See `NOTICE.md`, `LICENSE`, and `LICENSE_AUDIT.md`.
