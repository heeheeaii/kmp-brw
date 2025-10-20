package com.treevalue.beself.config

import com.treevalue.beself.setting.WebSettings

fun WebSettings.applyDefault() {
    customUserAgentString =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"
    isJavaScriptEnabled = true
    supportZoom = true
    allowFileAccessFromFileURLs = true
    allowUniversalAccessFromFileURLs = true

    mediaPlaybackRequiresUserGesture = false
    allowsInlineMediaPlayback = true
    allowsAirPlayForMediaPlayback = true
    allowsPictureInPictureMediaPlayback = true

    // 权限设置
    allowLocationAccess = true
    allowCameraAccess = true
    allowMicrophoneAccess = true
    allowNotifications = false

    // 存储和缓存
    allowThirdPartyCookies = true
    allowMixedContent = true
    allowFileAccessFromFileURLs = true

    // 现代Web技术
    enableWebRTC = true
    enableWebGL = true
    enableWebAssembly = true
}
