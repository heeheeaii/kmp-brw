package com.treevalue.beself.backend

import android.speech.tts.TextToSpeech
import com.treevalue.beself.platform.AndroidContextProvider
import com.treevalue.beself.util.KLogger
import com.treevalue.beself.util.de
import java.util.Locale

actual class TextToSpeechEngine {
    private var tts: TextToSpeech? = null
    private var isInitialized = false

    actual fun initialize(onInitialized: (Boolean) -> Unit) {
        try {
            val context = AndroidContextProvider.getContext()

            tts = TextToSpeech(context) { status ->
                isInitialized = status == TextToSpeech.SUCCESS

                if (isInitialized) {
                    // 设置中文语言
                    val result = tts?.setLanguage(Locale.CHINESE)
                    if (result == TextToSpeech.LANG_MISSING_DATA ||
                        result == TextToSpeech.LANG_NOT_SUPPORTED
                    ) {
                        // 如果中文不支持，使用默认语言
                        tts?.setLanguage(Locale.getDefault())
                    }
                }

                onInitialized(isInitialized)
            }
        } catch (e: Exception) {
            KLogger.de { "TTS初始化失败: ${e.message}" }
            onInitialized(false)
        }
    }

    actual fun speak(text: String) {
        if (isInitialized && tts != null) {
            try {
                tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
            } catch (e: Exception) {
                KLogger.de { "TTS播报失败: ${e.message}" }
            }
        }
    }

    actual fun shutdown() {
        try {
            tts?.stop()
            tts?.shutdown()
            tts = null
            isInitialized = false
        } catch (e: Exception) {
            KLogger.de { "TTS关闭失败: ${e.message}" }
        }
    }
}
