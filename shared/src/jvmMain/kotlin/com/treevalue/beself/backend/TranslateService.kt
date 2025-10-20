package com.treevalue.beself.backend

import com.treevalue.beself.util.KLogger
import com.treevalue.beself.util.dd
import com.treevalue.beself.util.de
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

object GoogleTranslateService : TranslateService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .build()

    // JSON 解析器
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    /**
     * 翻译文本
     * @param text 要翻译的文本
     * @param sourceLang 源语言（auto为自动检测）
     * @param targetLang 目标语言（zh-CN为简体中文，en为英文）
     * @return 翻译结果
     */
    override fun translate(
        text: String,
        sourceLang: String,
        targetLang: String,
    ): String {
        if (text.isBlank()) return ""

        return try {
            // 方法1: 使用Google Translate API
            translateWithGoogleApi(text, sourceLang, targetLang)
        } catch (e: Exception) {
            KLogger.de { "方法1失败: ${e.message}" }
            try {
                // 方法2: 备用方案 - 使用不同的endpoint
                translateWithAlternativeApi(text, sourceLang, targetLang)
            } catch (e2: Exception) {
                KLogger.de { "方法2失败: ${e2.message}" }
                throw Exception("翻译失败: ${e.message}")
            }
        }
    }

    /**
     * 方法1: 使用Google Translate API（主要方法）
     */
    private fun translateWithGoogleApi(
        text: String,
        sourceLang: String,
        targetLang: String,
    ): String {
        val encodedText = URLEncoder.encode(text, "UTF-8")

        // 构建URL
        val url = "https://translate.googleapis.com/translate_a/single?" +
                "client=gtx&" +
                "sl=$sourceLang&" +
                "tl=$targetLang&" +
                "dt=t&" +
                "q=$encodedText"

        KLogger.dd { "翻译请求URL: $url" }

        // 构建请求
        val request = Request.Builder()
            .url(url)
            .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
            .get()
            .build()

        // 执行请求
        val response = client.newCall(request).execute()

        if (!response.isSuccessful) {
            throw Exception("HTTP ${response.code}: ${response.message}")
        }

        val responseBody = response.body?.string()
            ?: throw Exception("响应体为空")

        KLogger.dd { "翻译响应: ${responseBody.take(200)}..." }

        // 解析JSON响应
        return parseGoogleApiResponse(responseBody)
    }

    /**
     * 方法2: 备用API（使用不同的参数）
     */
    private fun translateWithAlternativeApi(
        text: String,
        sourceLang: String,
        targetLang: String,
    ): String {
        val encodedText = URLEncoder.encode(text, "UTF-8")

        val url = "https://translate.googleapis.com/translate_a/t?" +
                "client=dict-chrome-ex&" +
                "sl=$sourceLang&" +
                "tl=$targetLang&" +
                "q=$encodedText"

        val request = Request.Builder()
            .url(url)
            .addHeader("User-Agent", "Mozilla/5.0")
            .get()
            .build()

        val response = client.newCall(request).execute()

        if (!response.isSuccessful) {
            throw Exception("HTTP ${response.code}")
        }

        val responseBody = response.body?.string()
            ?: throw Exception("响应体为空")

        return parseAlternativeApiResponse(responseBody)
    }

    /**
     * 解析Google API的JSON响应
     * 响应格式: [[["翻译文本","原文",null,null,10]],null,"en",null,null,null,null,[]]
     */
    private fun parseGoogleApiResponse(jsonStr: String): String {
        try {
            // 解析为 JsonElement
            val jsonElement = json.parseToJsonElement(jsonStr)

            // 确保是数组
            if (jsonElement !is JsonArray) {
                throw Exception("响应不是数组格式")
            }

            // 获取第一个元素（翻译结果数组）
            if (jsonElement.isEmpty()) {
                throw Exception("响应数组为空")
            }

            val firstElement = jsonElement[0]
            if (firstElement !is JsonArray) {
                throw Exception("第一个元素不是数组")
            }

            val result = StringBuilder()

            // 遍历所有翻译片段
            for (item in firstElement) {
                if (item is JsonArray && item.isNotEmpty()) {
                    // 获取第一个元素（翻译文本）
                    val translatedText = item[0]
                    if (translatedText is JsonPrimitive && translatedText !is JsonNull) {
                        result.append(translatedText.content)
                    }
                }
            }

            val finalResult = result.toString().trim()
            if (finalResult.isNotEmpty()) {
                return finalResult
            }

            throw Exception("无法从响应中提取翻译结果")

        } catch (e: Exception) {
            KLogger.de { "解析响应失败: ${e.message}, JSON: ${jsonStr.take(200)}" }
            throw Exception("解析失败: ${e.message}")
        }
    }

    /**
     * 解析备用API的响应
     * 备用API返回的是简单的JSON数组: ["翻译结果"]
     */
    private fun parseAlternativeApiResponse(jsonStr: String): String {
        try {
            val jsonElement = json.parseToJsonElement(jsonStr)

            if (jsonElement is JsonArray && jsonElement.isNotEmpty()) {
                val firstElement = jsonElement[0]
                if (firstElement is JsonPrimitive) {
                    return firstElement.content
                }
            }

            throw Exception("响应为空或格式错误")
        } catch (e: Exception) {
            // 如果JSON解析失败，尝试直接返回（可能是纯文本）
            if (jsonStr.isNotEmpty() && !jsonStr.startsWith("[")) {
                return jsonStr
            }
            throw Exception("解析备用API响应失败: ${e.message}")
        }
    }

    /**
     * 智能检测语言并翻译
     * 如果是中文翻译成英文，如果是英文翻译成中文
     */
    override fun smartTranslate(text: String): String {
        val detectedLang = detectLanguage(text)

        return when (detectedLang) {
            "zh" -> translate(text, "zh-CN", "en")  // 中文翻译成英文
            "en" -> translate(text, "en", "zh-CN")  // 英文翻译成中文
            else -> translate(text, "auto", "zh-CN") // 其他语言翻译成中文
        }
    }

    /**
     * 简单的语言检测
     */
    fun detectLanguage(text: String): String {
        val chineseCount = text.count { it in '\u4e00'..'\u9fa5' }
        val englishCount = text.count { it in 'a'..'z' || it in 'A'..'Z' }
        val totalChars = text.length

        return when {
            chineseCount.toDouble() / totalChars > 0.3 -> "zh"
            englishCount.toDouble() / totalChars > 0.5 -> "en"
            else -> "unknown"
        }
    }

    /**
     * 批量翻译（分段处理长文本）
     */
    override fun translateLongText(text: String, maxLength: Int): String {
        if (text.length <= maxLength) {
            return translate(text)
        }

        // 按句子分割
        val sentences = text.split(Regex("(?<=[。！？.!?])\\s*"))
        val result = StringBuilder()
        var currentChunk = StringBuilder()

        for (sentence in sentences) {
            if (currentChunk.length + sentence.length > maxLength) {
                // 翻译当前块
                if (currentChunk.isNotEmpty()) {
                    result.append(translate(currentChunk.toString()))
                    currentChunk.clear()
                }
            }
            currentChunk.append(sentence)
        }

        // 翻译最后一块
        if (currentChunk.isNotEmpty()) {
            result.append(translate(currentChunk.toString()))
        }

        return result.toString()
    }
}
