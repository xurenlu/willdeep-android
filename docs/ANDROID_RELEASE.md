# Android APK release

Stable APKs are published at https://github.com/xurenlu/willdeep-android/releases/latest.
The application ID is `com.willdeep.android`; the minimum OS is Android 13 (API 33).

## Build and verify

Use JDK 21 and the Android SDK referenced by `local.properties` or `ANDROID_HOME`.
The old JBR 17.0.9 can crash in the ARM64 JIT compiler during lint; JDK 21 is the release build runtime.
Run `ruby scripts/release_android.rb`. The script runs Release unit tests, lint and
the Release build, then aligns, signs and verifies the APK. Outputs are in
`build/public-release/<version>/`: APK, SHA256SUMS, report.json and report.md.
Review lint diagnostics and release reports before publishing.
When all dependencies are already cached, add `--offline` to avoid remote repository lookups.

For the first release only, run `ruby scripts/release_android.rb --initialize-key`
to create a dedicated signing key and random password in the ignored `.signing/`
directory. Subsequent releases must reuse these files. Back up the entire directory
to secure storage; never commit it, publish it, or replace it during an update.
Losing the key prevents compatible updates to installed stable APKs.

The signed Release APK cannot update an older debug-signed installation in place.
Users of a debug build must uninstall it before installing the stable APK and pair
again. Future stable APKs can update the stable installation normally.

## Publish

Update `versionName`, increment `versionCode`, and update CHANGELOG and product overview.
Commit the tested release branch, integrate into main while preserving commits,
and tag `v<version>`. Create a non-prerelease GitHub Release with the APK,
SHA256SUMS and reports. Download the public APK again and verify its checksum,
package metadata and signature against the local artifact.

This is direct APK distribution. It does not represent a Google Play or domestic
app-store listing. Push notification availability depends on the configured Umeng
credentials and the connected desktop's support.

## Remaining validation limits

Full lint is required to have zero errors. Existing warnings are counted in the
release report. They include dependency updates, localization plurals and a
third-party Umeng x86_64 native library with 4 KB ELF alignment. APK ZIP alignment
is checked separately at 16 KB; that check alone does not prove every native
library supports a 16 KB device. This release has not been validated end to end on
a 16 KB x86_64 device. The build report does not imply a live phone-to-desktop test.
