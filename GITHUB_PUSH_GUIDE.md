# GitHub push guide — MindTrigger Assist v16.0.0 RC1

Gói source này tương ứng với mã nguồn đã build thành công APK debug `v16.0.0-rc1`.

| Trường | Giá trị |
|---|---|
| Application ID | `dev.evoker.homeholdcts` |
| Version name | `v16.0.0-rc1` |
| Version code | `16000001` |
| Compile SDK | 36 |
| Min SDK | 32 |
| Target SDK | 36 |
| Android Gradle Plugin | 8.9.1 |
| Gradle khuyến nghị | 8.11.1 |

## 1. Khởi tạo repository

Tạo một repository GitHub mới, sau đó đặt thư mục source này làm thư mục gốc của repository. Không đổi `applicationId` nếu muốn APK cập nhật đè lên các bản MindTrigger Assist trước đây.

```bash
git init
git branch -M main
git add .
git commit -m "Release v16.0.0 RC1"
git remote add origin https://github.com/evokermc098-coder/MindTriggerAssist.git
git push -u origin main
```

## 2. Build debug từ source

Cài Android SDK với platform 36 và build-tools phù hợp. Dùng JDK 21, Gradle 8.11.1 và Android Gradle Plugin 8.9.1.

```bash
printf 'sdk.dir=/absolute/path/to/android-sdk\n' > local.properties
./gradlew --no-daemon clean assembleDebug
```

Nếu repository chưa có Gradle Wrapper, có thể mở project bằng Android Studio hoặc cài Gradle 8.11.1 trên máy build. Tệp `local.properties` chỉ dùng cục bộ và không được commit.

## 3. Build release có ký tên

Không đặt private keystore, mật khẩu hoặc certificate vào repository. Sao chép `keystore.properties.example` thành `keystore.properties` trên máy build riêng, rồi điền đường dẫn keystore và mật khẩu thật. Tệp `keystore.properties` đã được loại khỏi gói source và phải nằm trong `.gitignore`.

```properties
storeFile=/secure/path/release-key.jks
storePassword=YOUR_STORE_PASSWORD
keyAlias=YOUR_KEY_ALIAS
keyPassword=YOUR_KEY_PASSWORD
```

Có thể dùng các biến môi trường `MTA_STORE_FILE`, `MTA_STORE_PASSWORD`, `MTA_KEY_ALIAS` và `MTA_KEY_PASSWORD` thay cho file properties. Không ghi các giá trị thật vào issue, pull request, log CI hoặc GitHub Actions output.

## 4. Kiểm tra trước khi phát hành

```bash
python3 tools/release_sanity.py
$ANDROID_HOME/build-tools/36.0.0/apksigner verify --verbose app/build/outputs/apk/release/app-release.apk
```

Nếu build-tools 36 chưa có trên máy, dùng phiên bản build-tools đã được cài đặt và tương thích với SDK 36. APK debug của bản đã build trước đó có SHA-256:

```text
32905f2de43b2804e9cfff0c977d689d224b0463de2ec11a95dfb3e82bd3a77d
```

## 5. Tệp cố ý không có trong archive

Thư mục `build/`, `.gradle/` và `local.properties` đã được loại khỏi archive vì đây là dữ liệu generated hoặc phụ thuộc máy cục bộ. Private keystore và `keystore.properties` cũng không được phân phối. Tất cả mã nguồn Java, manifest, resource, WAV, license, notices, security audit và tài liệu provenance cần thiết cho việc review GPL vẫn được giữ lại.

MindTrigger Assist được phân phối theo GPL-3.0-only. Khi phân phối APK hoặc object code, cần cung cấp corresponding source và giữ lại license, attribution, notice và provenance tương ứng.
