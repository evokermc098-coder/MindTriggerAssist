# Third-party notices

This file records direct and transitive libraries/assets used by the v16.2 source.
It is not a substitute for the license text files bundled with the project.

## MiCTS

- Project: MiCTS
- Upstream: parallelcc/MiCTS
- Repository: https://github.com/parallelcc/MiCTS
- License: GNU GPL version 3

The Circle to Search invocation path in MindTrigger Assist was implemented with
reference to MiCTS and is treated as an upstream-derived GPL path. Project-level
release license: GPL-3.0-only.

The full GPL text is in `LICENSE` and is also bundled for in-app viewing.

## Google Material Components for Android

- Artifact: `com.google.android.material:material:1.14.0`
- Upstream: https://github.com/material-components/material-components-android
- License: Apache License 2.0

License text: `THIRD_PARTY_LICENSES/Google_Material_Design_Apache-2.0.txt`

## Google Material Icons

The bottom-navigation icon geometry uses Google Material Icons designs.

- Upstream: https://github.com/google/material-design-icons
- License: Apache License 2.0

The MindTrigger Assist launcher icon is a project asset and is not a Google logo
or Material icon.

## Shizuku API

Direct artifacts:

- `dev.rikka.shizuku:api:13.1.5`
- `dev.rikka.shizuku:provider:13.1.5`

Upstream: https://github.com/RikkaApps/Shizuku-API
License: MIT License
Copyright (c) 2021 RikkaW

License text: `THIRD_PARTY_LICENSES/Shizuku_API_MIT.txt`

Note: the Shizuku manager application is a separate product. The direct library
used by this source is Shizuku-API, whose repository declares the MIT License.

## AndroidHiddenApiBypass

- Artifact: `org.lsposed.hiddenapibypass:hiddenapibypass:6.1`
- Upstream: https://github.com/LSPosed/AndroidHiddenApiBypass
- License: Apache License 2.0
- Copyright: 2021-2025 LSPosed

License text:
`THIRD_PARTY_LICENSES/AndroidHiddenApiBypass_Apache-2.0.txt`

## Release dependency notices

The 61 artifacts in [RELEASE_DEPENDENCY_INVENTORY.md](RELEASE_DEPENDENCY_INVENTORY.md)
were checked against their cached Maven POMs and published AAR/JAR notices.
Shizuku aidl/shared are MIT, like API/provider. AndroidX, Kotlin/coroutines,
JetBrains annotations, Error Prone, Guava ListenableFuture and JSpecify declare
Apache-2.0. Guava inherits its license from guava-parent:26.0-android.

Full extracted notices and the artifact list are preserved in
[Release_Dependency_Notices.txt](THIRD_PARTY_LICENSES/Release_Dependency_Notices.txt)
and in the app's Apache license entry. Existing MIT and GPL entries are retained.

## Bundled audio provenance

`aura_cts.wav` and `aura_gemini.wav` are project audio assets. Their known
provenance and SHA-256 hashes are documented in `AUDIO_PROVENANCE.md`; they are
not third-party library assets.
