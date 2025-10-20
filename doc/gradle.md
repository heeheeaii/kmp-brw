安卓混淆崩溃# 获取详细崩溃信息  在cmd里面

adb logcat -v time | grep -E "(FATAL|AndroidRuntime|CrashHandler)"



./gradlew :androidApp:assembleRelease  :desktopApp:packageMsi

./gradlew :androidApp:assembleRelease

./gradlew :desktopApp:packageMsi

./gradlew :shared:linkReleaseSharedAndroidNativeArm64

D:\beself

D:\code\kmp-brw\desktopApp\build\compose\binaries\main\msi\

D:\code\kmp-brw\androidApp\build\outputs\apk\release

D:\code\kmp-brw\sample\desktopApp



```bash
./gradlew assembleRelease --max-workers=3

./gradlew assembleRelease --max-workers=1 --no-parallel
```

./gradlew bundleRelease    安卓aab生产包

./gradlew assembleDebug  安卓debug包

./gradlew bundleDebug

./gradlew :desktopApp:run



 签名

keytool -genkey -v -keystore beself-release-key.jks -keyalg RSA -keysize 2048 -validity 10000 -alias beself-alias





功能:

本地git门禁

./gradlew setUpGitHooks

ktlint 格式化修复

./gradlew ktlintFormat

关闭ktlint

```
tasks.withType<org.jlleitschuh.gradle.ktlint.tasks.KtLintCheckTask> {
    enabled = false
}
```





