package com.treevalue.beself.util

import co.touchlab.kermit.DefaultFormatter
import co.touchlab.kermit.Logger
import co.touchlab.kermit.Severity
import co.touchlab.kermit.mutableLoggerConfigInit
import co.touchlab.kermit.platformLogWriter
import com.treevalue.beself.config.BrowserConfig

internal object KLogger : Logger(
    config = mutableLoggerConfigInit(listOf(platformLogWriter(DefaultFormatter))),
    tag = "brw",
) {
    init {
        mutableConfig.minSeverity = Severity.Debug
    }
}

fun Logger.dv(msg: String, throwable: Throwable) {
    if (BrowserConfig.useDebugModel) {
        v(throwable = throwable) { msg }
    }
}

fun Logger.dd(msg: String, throwable: Throwable) {
    if (BrowserConfig.useDebugModel) {
        d(throwable = throwable) { msg }
    }
}

fun Logger.di(msg: String, throwable: Throwable) {
    if (BrowserConfig.useDebugModel) {
        i(throwable = throwable) { msg }
    }
}

fun Logger.dw(msg: String, throwable: Throwable) {
    if (BrowserConfig.useDebugModel) {
        w(throwable = throwable) { msg }
    }
}

fun Logger.de(msg: String, throwable: Throwable) {
    if (BrowserConfig.useDebugModel) {
        e(throwable = throwable) { msg }
    }
}

fun Logger.da(msg: String, throwable: Throwable) {
    if (BrowserConfig.useDebugModel) {
        a(throwable = throwable) { msg }
    }
}

inline fun Logger.dv(crossinline message: () -> String) {
    if (BrowserConfig.useDebugModel) {
        v(message = message)
    }
}

inline fun Logger.dd(crossinline message: () -> String) {
    if (BrowserConfig.useDebugModel) {
        d(message = message)
    }
}

inline fun Logger.di(crossinline message: () -> String) {
    if (BrowserConfig.useDebugModel) {
        i(message = message)
    }
}

inline fun Logger.dw(crossinline message: () -> String) {
    if (BrowserConfig.useDebugModel) {
        w(message = message)
    }
}

inline fun Logger.de(crossinline message: () -> String) {
    if (BrowserConfig.useDebugModel) {
        e(message = message)
    }
}

inline fun Logger.da(crossinline message: () -> String) {
    if (BrowserConfig.useDebugModel) {
        a(message = message)
    }
}
