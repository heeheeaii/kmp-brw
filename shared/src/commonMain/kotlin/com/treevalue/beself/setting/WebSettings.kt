package com.treevalue.beself.setting

import androidx.compose.ui.graphics.Color


class WebSettings {
    var isJavaScriptEnabled = true

    var customUserAgentString: String? = null

    var zoomLevel: Double = 1.0

    var supportZoom: Boolean = true

    var allowUniversalAccessFromFileURLs: Boolean = false

    var backgroundColor = Color.Transparent

    val androidWebSettings = PlatformWebSettings.AndroidWebSettings()

    val desktopWebSettings = PlatformWebSettings.DesktopWebSettings()

    // 媒体播放设置
    var mediaPlaybackRequiresUserGesture: Boolean = true
    var allowsInlineMediaPlayback: Boolean = false
    var allowsAirPlayForMediaPlayback: Boolean = true
    var allowsPictureInPictureMediaPlayback: Boolean = true

    // 权限设置
    var allowLocationAccess: Boolean = false
    var allowCameraAccess: Boolean = false
    var allowMicrophoneAccess: Boolean = false
    var allowNotifications: Boolean = false

    // Chrome-like配置
    var userAgent: String? = null // 默认使用Chrome UA
    var enableWebRTC: Boolean = true
    var enableWebGL: Boolean = true
    var enableWebAssembly: Boolean = true

    // 安全设置
    var allowMixedContent: Boolean = false
    var allowThirdPartyCookies: Boolean = true
    var allowFileAccessFromFileURLs: Boolean = false

    // Google服务支持
    var enableGooglePlayServices: Boolean = true
    var allowGoogleAccountLogin: Boolean = true
}
