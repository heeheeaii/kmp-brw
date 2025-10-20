plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.multiplatorm)
}

kotlin {
    androidTarget()
    sourceSets {
        androidMain.dependencies {
            implementation(project(":shared"))
        }
    }
}

android {
    compileSdk = (findProperty("android.compileSdk") as String).toInt()
    namespace = "com.treevalue.beself"

    sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")

    defaultConfig {
        applicationId = "com.treevalue.beself"
        minSdk = (findProperty("android.minSdk") as String).toInt()
        targetSdk = (findProperty("android.targetSdk") as String).toInt()
        versionCode = 1
        versionName = "1.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlin {
        jvmToolchain(17)
    }

    signingConfigs {
        create("release") {
            storeFile = file(findProperty("RELEASE_STORE_FILE") as String)
            storePassword = findProperty("RELEASE_STORE_PASSWORD") as String
            keyAlias = findProperty("RELEASE_KEY_ALIAS") as String
            keyPassword = findProperty("RELEASE_KEY_PASSWORD") as String
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true // 启用代码混淆
            isShrinkResources = false // 启用资源压缩
            proguardFiles(
//                getDefaultProguardFile("proguard-android-optimize.txt"),
                getDefaultProguardFile("proguard-android.txt"), // 轻量级
                "proguard-rules.pro"
            )
            // 完整模式
            isDebuggable = false
            signingConfig = signingConfigs.getByName("release")
        }

        debug {
            // Debug版本建议不启用混淆，便于调试
            isMinifyEnabled = false
            isShrinkResources = false
            isDebuggable = true

            // debug版本测试混淆效果：
            // isMinifyEnabled = true
            // proguardFiles(
            //     getDefaultProguardFile("proguard-android.txt"),
            //     "proguard-rules.pro",
            //     "proguard-rules-debug.pro"
            // )
        }
    }

// APK命名配置
    applicationVariants.all {
        val variant = this
        val versionName = variant.versionName
        val versionCode = variant.versionCode
        val buildType = variant.buildType.name
        val flavorName = variant.flavorName

        // 配置APK文件名
        variant.outputs.all {
            val output = this as com.android.build.gradle.internal.api.BaseVariantOutputImpl

            val fileName = when (buildType) {
                "release" -> {
                    if (flavorName.isNotEmpty()) {
                        "Beself-${flavorName}-release.apk"
                    } else {
                        "Beself-release.apk"
                    }
                }

                "debug" -> {
                    "Beself-${versionName}-debug.apk"
                }

                else -> {
                    "Beself-${buildType}-v${versionName}.apk"
                }
            }

            output.outputFileName = fileName
        }
    }
}
