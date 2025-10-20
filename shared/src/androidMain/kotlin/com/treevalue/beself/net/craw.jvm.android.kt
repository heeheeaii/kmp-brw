package com.treevalue.beself.net

import android.os.Environment
import java.io.File

actual fun getDownloadDirectory(): File {
    return Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
}
