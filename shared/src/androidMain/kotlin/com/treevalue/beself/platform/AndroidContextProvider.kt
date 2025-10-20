package com.treevalue.beself.platform

import android.app.Application

object AndroidContextProvider {
    private var application: Application? = null

    fun init(app: Application) {
        application = app
    }

    fun getContext(): Application {
        return application ?: throw IllegalStateException("AndroidContextProvider not initialized")
    }
}
