/*
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package com.treevalue.beself

import androidx.compose.material.Text
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.treevalue.beself.web.cleanupSharedResources
import dev.datlag.kcef.KCEF
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

fun main() =
    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "Beself",
            icon = painterResource("img/ic_launcher.ico")

        ) {
            var restartRequired by remember { mutableStateOf(false) }
            var downloading by remember { mutableStateOf(0F) }
            var initialized by remember { mutableStateOf(false) }

            LaunchedEffect(Unit) {
                withContext(Dispatchers.IO) {
                    KCEF.init(builder = {
                        installDir(File("kcef-bundle"))
                        progress {
                            onDownloading {
                                downloading = it
                            }
                            onInitialized {
                                initialized = true
                            }
                        }
                        download {
                            github {
                                release("jbr-release-17.0.12b1207.37")
                            }
                        }

                        settings {
                            // kcef cache path
                            cachePath = File("cache").absolutePath
                            addArgs(
                                "--disable-features=BlockThirdPartyCookies,RestrictThirdPartyCookiePartitions",
                                "--remote-debugging-port=9999",
                                "--disable-background-timer-throttling",
                                "--enable-features=LegacyCookieAccess",
                            )
                        }
                    }, onError = {
                        it?.printStackTrace()
                    }, onRestartRequired = {
                        restartRequired = true
                    })
                }
            }

            if (restartRequired) {
                Text(text = "Restart required.")
            } else {
                if (initialized) {
                    webViewApp()
                } else {
                    Text(text = "Downloading the browser engine $downloading%")
                }
            }

            DisposableEffect(Unit) {
                onDispose {
                    KCEF.disposeBlocking()
                    cleanupSharedResources()
                }
            }
        }
    }
