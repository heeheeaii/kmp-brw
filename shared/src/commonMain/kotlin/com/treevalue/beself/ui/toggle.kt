package com.treevalue.beself.ui

import com.treevalue.beself.js.getForceDarkModeScript
import com.treevalue.beself.web.WebViewController
import kotlinx.coroutines.delay


fun toggleForceDarkMode(enable: Boolean, navigator: WebViewController) {
    navigator.evaluateJavaScript(getForceDarkModeScript(enable))
}

suspend fun enhanceToggleForceDarkMode(navigator: WebViewController) {
    delay(200)
    toggleForceDarkMode(true, navigator)
    delay(1000)
    toggleForceDarkMode(true, navigator)
    delay(2000)
    toggleForceDarkMode(true, navigator)
    delay(3000)
    toggleForceDarkMode(true, navigator)
}
