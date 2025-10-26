package com.treevalue.beself.config

import com.treevalue.beself.setting.WebSettings
import com.treevalue.beself.values.agentStr

fun WebSettings.applyDefault() {
    customUserAgentString = agentStr
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
