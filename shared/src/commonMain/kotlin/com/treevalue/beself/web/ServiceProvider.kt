package com.treevalue.beself.web

interface ServiceProvider {
    fun isUrlAllowed(url: String): Boolean
    fun isUrlBlocked(url: String): Boolean
    fun isDarkMode(): Boolean
    fun setVideoLimiterSpeed(speed: Boolean)
    fun isStillVideoEnable(): Boolean
}
