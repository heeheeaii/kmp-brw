package com.treevalue.beself.backend

interface TranslateService {
    fun translate(
        text: String,
        sourceLang: String = "auto",
        targetLang: String = "zh-CN",
    ): String

    fun translateLongText(text: String, maxLength: Int = 500): String
    fun smartTranslate(text: String): String
}
