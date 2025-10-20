package com.treevalue.beself.platform

enum class PlatformType {
    Android,
    Desktop
}

val g_android = PlatformType.Android
val g_desktop = PlatformType.Desktop

expect fun getPlatformName(): PlatformType
