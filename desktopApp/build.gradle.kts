import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.compose.multiplatorm)
}

kotlin {
    jvm {
        jvmToolchain(17)
    }
    sourceSets {
        val jvmMain by getting {
            dependencies {
                implementation(libs.collection) // 或 1.4.0 稳定版
                implementation(compose.desktop.currentOs)
                implementation(project(":shared"))
                implementation(libs.gluegen.rt)
                implementation(libs.jogl.all)
                // java.lang.NoClassDefFoundError: androidx/lifecycle/ViewModelStoreOwner
                implementation(libs.lifecycle.viewmodel.compose.desktop)
                implementation(libs.lifecycle.runtime)
            }
        }
    }
}

compose.desktop {
    application {
        mainClass = "com.treevalue.beself.MainKt"

        nativeDistributions {
            // dmg mac, msi win, deb debian
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "Beself"
            packageVersion = "1.0.0"
            includeAllModules = true
        }

        buildTypes.release.proguard {
            configurationFiles.from("compose-desktop.pro")
        }
    }
}

afterEvaluate {
    tasks.withType<JavaExec> {
        jvmArgs("--add-opens", "java.desktop/sun.awt=ALL-UNNAMED")
        jvmArgs("--add-opens", "java.desktop/java.awt.peer=ALL-UNNAMED")

        if (System.getProperty("os.name").contains("Mac")) {
            jvmArgs("--add-opens", "java.desktop/sun.awt=ALL-UNNAMED")
            jvmArgs("--add-opens", "java.desktop/sun.lwawt=ALL-UNNAMED")
            jvmArgs("--add-opens", "java.desktop/sun.lwawt.macosx=ALL-UNNAMED")
        }
    }
}
