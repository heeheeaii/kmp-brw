package com.treevalue.beself.setting

sealed class PlatformWebSettings {
    data class AndroidWebSettings(
        var hideDefaultVideoPoster: Boolean = false,
    ) : PlatformWebSettings() {
    }


    data class DesktopWebSettings(
        var offScreenRendering: Boolean = false,
        var transparent: Boolean = true,
        var disablePopupWindows: Boolean = false,
    ) : PlatformWebSettings()
}
