package com.treevalue.beself.backend

import com.treevalue.beself.util.KLogger
import com.treevalue.beself.util.de
import com.treevalue.beself.util.dd
import java.io.IOException

actual class TextToSpeechEngine {
    private var isInitialized = false
    private var isWindows = false
    private var hasChineseVoice = false

    actual fun initialize(onInitialized: (Boolean) -> Unit) {
        try {
            val osName = System.getProperty("os.name").lowercase()
            isWindows = osName.contains("windows")

            when {
                isWindows -> {
                    // 检查 Windows 是否有中文语音
                    val checkCommand = arrayOf(
                        "powershell",
                        "-Command",
                        """
                        Add-Type -AssemblyName System.Speech
                        ${'$'}synth = New-Object System.Speech.Synthesis.SpeechSynthesizer
                        ${'$'}voices = ${'$'}synth.GetInstalledVoices()
                        ${'$'}chineseVoice = ${'$'}voices | Where-Object { ${'$'}_.VoiceInfo.Culture.Name -like 'zh-*' } | Select-Object -First 1
                        if (${'$'}chineseVoice) { exit 0 } else { exit 1 }
                        """.trimIndent()
                    )

                    val process = Runtime.getRuntime().exec(checkCommand)
                    val exitCode = process.waitFor()

                    hasChineseVoice = exitCode == 0
                    isInitialized = true

                    if (!hasChineseVoice) {
                        KLogger.de { "警告: Windows 未安装中文语音包，将使用默认语音(可能无法正确播报中文),请在 设置 → 时间和语言 → 语音 中添加中文语音" }
                    } else {
                        KLogger.dd { "检测到中文语音，TTS初始化成功" }
                    }
                }

                else -> {
                    // Linux 检查 espeak
                    val process = Runtime.getRuntime().exec(arrayOf("which", "espeak"))
                    val exitCode = process.waitFor()
                    isInitialized = exitCode == 0
                    hasChineseVoice = isInitialized

                    if (!isInitialized) {
                        KLogger.de { "Linux 未安装 espeak，请运行: sudo apt-get install espeak" }
                    }
                }
            }

            onInitialized(isInitialized)

        } catch (e: Exception) {
            KLogger.de { "TTS初始化失败: ${e.message}" }
            e.printStackTrace()
            isInitialized = false
            onInitialized(false)
        }
    }

    actual fun speak(text: String) {
        if (!isInitialized) {
            KLogger.de { "TTS未初始化，无法播报: $text" }
            return
        }

        if (text.isEmpty()) {
            KLogger.de { "TTS警告: 文本为空" }
            return
        }

        // 在后台线程执行语音播报
        Thread {
            try {
                val command = when {
                    isWindows -> {
                        // Windows PowerShell - 优先使用中文语音
                        val escapedText = text.replace("'", "''")

                        val script = if (hasChineseVoice) {
                            // 有中文语音，指定使用
                            """
                            Add-Type -AssemblyName System.Speech
                            ${'$'}speak = New-Object System.Speech.Synthesis.SpeechSynthesizer
                            ${'$'}speak.Rate = 0
                            
                            # 选择中文语音
                            ${'$'}chineseVoice = ${'$'}speak.GetInstalledVoices() | 
                                Where-Object { ${'$'}_.VoiceInfo.Culture.Name -like 'zh-*' } | 
                                Select-Object -First 1
                            
                            if (${'$'}chineseVoice) {
                                ${'$'}speak.SelectVoice(${'$'}chineseVoice.VoiceInfo.Name)
                            }
                            
                            ${'$'}speak.Speak('$escapedText')
                            """.trimIndent()
                        } else {
                            // 没有中文语音，使用默认
                            """
                            Add-Type -AssemblyName System.Speech
                            ${'$'}speak = New-Object System.Speech.Synthesis.SpeechSynthesizer
                            ${'$'}speak.Rate = 0
                            ${'$'}speak.Speak('$escapedText')
                            """.trimIndent()
                        }

                        arrayOf("powershell", "-Command", script)
                    }

                    else -> {
                        // Linux - espeak 使用中文语音
                        arrayOf("espeak", "-v", "zh", text)
                    }
                }

                val process = Runtime.getRuntime().exec(command)
                val exitCode = process.waitFor()

                if (exitCode != 0) {
                    val errorStream = process.errorStream.bufferedReader().use { it.readText() }
                    KLogger.de { "TTS播报失败 (退出码: $exitCode): $errorStream" }
                } else {
                    KLogger.dd { "TTS播报成功: $text" }
                }

            } catch (e: IOException) {
                KLogger.de { "TTS播报IO异常: ${e.message}" }
                e.printStackTrace()
            } catch (e: InterruptedException) {
                KLogger.de { "TTS播报被中断: ${e.message}" }
                Thread.currentThread().interrupt()
            } catch (e: Exception) {
                KLogger.de { "TTS播报未知异常: ${e.message}" }
                e.printStackTrace()
            }
        }.start()
    }

    actual fun shutdown() {
        isInitialized = false
        KLogger.dd { "TTS引擎已关闭" }
    }
}
