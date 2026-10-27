# QR Scanner Pro

ক্যামেরা দিয়ে QR কোড ও বারকোড স্ক্যান করার একটি আধুনিক অ্যান্ড্রয়েড অ্যাপ।
Kotlin + Jetpack Compose + CameraX + Google ML Kit (সম্পূর্ণ অফলাইন)।

## ফিচার

- লাইভ স্ক্যান: QR, Data Matrix, Aztec, PDF417, EAN-8/13, UPC-A/E, Code 39/93/128, ITF, Codabar
- অ্যানিমেটেড স্ক্যান ফ্রেম, পিঞ্চ-টু-জুম, ট্যাপ-টু-ফোকাস, ফ্ল্যাশলাইট
- গ্যালারির ছবি থেকে স্ক্যান
- স্মার্ট রেজাল্ট (লিংক, ওয়াই-ফাই, কন্টাক্ট, ইমেইল, ফোন, SMS, লোকেশন, বারকোড, টেক্সট) এবং প্রতিটির জন্য আলাদা অ্যাকশন
- `http` লিংকে নিরাপত্তা সতর্কতা
- স্ক্যান ইতিহাস, ভাইব্রেশন/বিপ/ইতিহাস সেটিংস
- বাংলা ও ইংরেজি (ফোনের ভাষা অনুযায়ী)
- শুধু ক্যামেরা ও ভাইব্রেশন পারমিশন, ইন্টারনেট পারমিশন নেই
- minSdk 26 (Android 8.0+)

## APK বানানোর উপায়

### পদ্ধতি ১: GitHub দিয়ে (কম্পিউটারে কিছু ইনস্টল লাগবে না)

1. [github.com](https://github.com)-এ বিনামূল্যে অ্যাকাউন্ট খুলুন এবং একটি নতুন repository বানান (যেমন `qr-scanner-pro`)।
2. এই ফোল্ডারের **সব ফাইল ও ফোল্ডার** (লুকানো `.github` ফোল্ডারসহ) repository-তে আপলোড করুন।
   (কম্পিউটারের ব্রাউজারে "uploading an existing file" থেকে ফোল্ডারটি ড্র্যাগ করে দিন।)
3. **Actions** ট্যাবে যান। "Build APK" নিজে থেকে চলবে। না চললে বাম পাশে "Build APK" বেছে **Run workflow** চাপুন।
4. ৫–৮ মিনিট পর কাজ সবুজ হলে ওই রানে ক্লিক করুন এবং নিচে **Artifacts → QRScannerPro-APK** ডাউনলোড করুন।
5. জিপ খুললে `debug/app-debug.apk` ও `release/app-release.apk` পাবেন। ফোনে যেকোনো একটি ইনস্টল করুন।
   (প্রথমবার "Unknown sources" থেকে ইনস্টলের অনুমতি দিতে হতে পারে।)

### পদ্ধতি ২: Android Studio দিয়ে

1. Android Studio (Ladybug বা নতুন) ইনস্টল করুন।
2. **File → Open** থেকে এই ফোল্ডারটি খুলুন এবং Gradle sync শেষ হওয়া পর্যন্ত অপেক্ষা করুন।
3. **Build → Build Bundle(s) / APK(s) → Build APK(s)**।
4. APK পাবেন `app/build/outputs/apk/debug/app-debug.apk`-এ।

কমান্ড লাইনে: `./gradlew assembleRelease`

## Play Store-এ দিতে চাইলে

`app/build.gradle.kts`-এর release বিল্ডে এখন debug কী দিয়ে সাইন করা আছে, যাতে APK সরাসরি ইনস্টল করা যায়।
Play Store-এর জন্য নিজের keystore বানিয়ে `signingConfigs`-এ যোগ করুন এবং `applicationId` বদলে নিন।

## কাস্টমাইজ

| কী বদলাবেন | কোথায় |
|---|---|
| অ্যাপের নাম | `app/src/main/res/values/strings.xml` ও `values-bn/strings.xml` |
| রঙ | `app/src/main/java/com/qrpro/scanner/ui/Theme.kt` |
| আইকন | `app/src/main/res/drawable/ic_launcher_foreground.xml` |
| প্যাকেজ নাম | `app/build.gradle.kts` (`applicationId`, `namespace`) |
