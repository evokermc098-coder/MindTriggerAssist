# MindTrigger Assist APK signing

Release configuration is intentionally separate from source control.

## Signature schemes

APK builds are configured as:

- v1 (JAR signing): enabled
- v2 (APK Signature Scheme v2): enabled
- v3 (APK Signature Scheme v3): enabled
- v4: disabled

Both `debug` and `release` variants use the same scheme policy.

## Release signing

Choose one of the following.

### Option A — keystore.properties

Copy:

```text
keystore.properties.example
```

to:

```text
keystore.properties
```

and set:

```properties
storeFile=signing/mindtrigger-release.jks
storePassword=YOUR_STORE_PASSWORD
keyAlias=mindtrigger
keyPassword=YOUR_KEY_PASSWORD
```

`storeFile` may also be an absolute path.

### Option B — environment variables

```text
MTA_KEYSTORE
MTA_STORE_PASSWORD
MTA_KEY_ALIAS
MTA_KEY_PASSWORD
```

Example on Windows PowerShell:

```powershell
$env:MTA_KEYSTORE="C:\Keys\mindtrigger-release.jks"
$env:MTA_STORE_PASSWORD="..."
$env:MTA_KEY_ALIAS="mindtrigger"
$env:MTA_KEY_PASSWORD="..."
.\gradlew assembleRelease
```

If release credentials are absent, a release task fails instead of silently
creating an unsigned release APK.

## Verify the produced APK

Use Android SDK `apksigner`:

```text
apksigner verify --verbose --print-certs app-release.apk
```

The verification output should report v1, v2 and v3 as verified.
