package com.treevalue.beself.backend

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import com.russhwolf.settings.Settings
import com.treevalue.beself.config.LangType
import com.treevalue.beself.config.OuterConfig
import com.treevalue.beself.util.KLogger
import com.treevalue.beself.util.de

object FunctionBackend {

    private val KEY_LANGUAGE = "system_language"

    private val settings: Settings = Settings()

    private val defaultLangType = LangType.EN
    private val _language = mutableStateOf(defaultLangType)
    val language: State<LangType> = _language

    init {
        loadSettings()
    }

    fun setLanguage(langType: LangType) {
        if (_language.value != langType) {
            _language.value = langType
            OuterConfig.langType = langType
            saveSettings()
        }
    }

    fun getLanguage(): LangType {
        return _language.value
    }

    private fun saveSettings() {
        try {
            settings.putString(KEY_LANGUAGE, _language.value.name)
        } catch (e: Exception) {
            KLogger.de { "保存配置失败: ${e.message}" }
            e.printStackTrace()
        }
    }

    private fun loadSettings() {
        try {
            val savedLanguage = settings.getStringOrNull(KEY_LANGUAGE)
            if (savedLanguage != null) {
                val langType = try {
                    LangType.valueOf(savedLanguage)
                } catch (e: Exception) {
                    KLogger.de { "语言配置解析失败，使用默认配置: ${e.message}" }
                    LangType.CN
                }
                _language.value = langType
                OuterConfig.langType = langType
            } else {
                _language.value = defaultLangType
                OuterConfig.langType = defaultLangType
                saveSettings()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            _language.value = defaultLangType
            OuterConfig.langType = defaultLangType
        }
    }

    fun resetToDefault() {
        setLanguage(defaultLangType)
    }
}
