# ================ 高安全性混淆配置 ================

# 基础必须保留（最小化）
-keep public class * extends android.app.Activity {
    public void onCreate(android.os.Bundle);
    public void onDestroy();
}
-keep public class * extends android.app.Application {
    public void onCreate();
}

# 反射和序列化保护
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes EnclosingMethod
-keepattributes InnerClasses

# === 优化设置 ===
-repackageclasses ''
-flattenpackagehierarchy ''
-allowaccessmodification
-overloadaggressively
-mergeinterfacesaggressively

# 字符串加密（R8不直接支持，但会重命名）
-adaptclassstrings
-adaptresourcefilenames
-adaptresourcefilecontents

# === Kotlin最小保留 ===
# 只保留必要的Kotlin运行时
-keep class kotlin.Metadata { *; }
-keep class kotlin.jvm.internal.** { *; }
-keepclassmembers class **$WhenMappings {
    <fields>;
}

# 协程最小保留
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler

# === Compose最小保留 ===
# 只保留编译器必需的部分
-keep class androidx.compose.runtime.Composer { *; }
-keep class androidx.compose.runtime.ComposerKt { *; }
-keep class androidx.compose.runtime.CompositionLocal { *; }
-keep class * extends androidx.compose.runtime.RememberObserver { *; }

# 移除Compose调试信息
-assumenosideeffects class androidx.compose.runtime.ComposerKt {
    boolean isTraceInProgress();
    void traceEventStart(int, java.lang.String);
    void traceEventEnd();
}

# === WebView安全保留 ===
# 只保留实际使用的WebView功能
-keep class android.webkit.WebView {
    public void loadUrl(java.lang.String);
    public void evaluateJavascript(java.lang.String, android.webkit.ValueCallback);
}
-keep class android.webkit.WebViewClient {
    public boolean shouldOverrideUrlLoading(android.webkit.WebView, java.lang.String);
}

-keep,allowobfuscation,allowshrinking,allowoptimization class com.treevalue.beself.** {
    <fields>;
    <methods>;
}

-keep class com.treevalue.beself.MainActivity {
    void onCreate(android.os.Bundle);
    void onDestroy();
    void onResume();
    void onpause();
}
-keep public class com.treevalue.beself.** extends android.app.Application {
    public void onCreate();
    public void onTerminate();
}
-keep public class com.treevalue.beself.** extends android.app.Service {
    public android.os.IBinder onBind(android.content.Intent);
}

-keep public class com.treevalue.beself.** extends android.content.BroadcastReceiver {
    public void onReceive(android.content.Context, android.content.Intent);
}

-adaptclassstrings com.treevalue.beself.**

# 其他业务类全部混淆
#-keep class com.treevalue.beself.** {
#    # 只保留public方法，private全部混淆
#    public *;
#}

# === 激进日志移除 ===
-assumenosideeffects class com.treevalue.beself.** {
    *** println(...);
    *** print(...);
    *** log(...);
    *** debug(...);
    *** info(...);
    *** warn(...);
    *** error(...);
}

# === 反调试保护 ===
# 移除调试相关信息
-assumenosideeffects class java.lang.Throwable {
    public void printStackTrace();
}

-keepattributes !Metadata
-keepattributes !RuntimeVisibleAnnotations
-keepattributes !RuntimeInvisibleAnnotations
-keepattributes !RuntimeVisibleParameterAnnotations
-keepattributes !RuntimeInvisibleParameterAnnotations

-keep,allowobfuscation class com.treevalue.beself.**$Companion {
    <fields>;
    <methods>;
}
-keep,allowobfuscation class com.treevalue.beself.**$WhenMappings {
    <fields>;
}

-renamesourcefileattribute ""
-keepattributes !SourceFile
-keepattributes !LineNumberTable
-renamesourcefileattribute ""

-assumenosideeffects class com.treevalue.beself.** {
    public java.lang.String toString();
}
