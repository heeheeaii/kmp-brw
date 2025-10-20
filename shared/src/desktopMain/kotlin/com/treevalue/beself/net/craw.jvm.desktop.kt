package com.treevalue.beself.net

import java.io.File

actual fun getDownloadDirectory(): File {
    val userHome = System.getProperty("user.home")
    return File(userHome, "Downloads")
}
