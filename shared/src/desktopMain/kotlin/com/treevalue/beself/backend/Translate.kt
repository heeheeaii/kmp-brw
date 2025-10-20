package com.treevalue.beself.backend

import com.treevalue.beself.util.KLogger
import com.treevalue.beself.util.dd
import com.treevalue.beself.util.de
import dev.datlag.kcef.KCEFBrowser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import org.cef.browser.CefBrowser
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.concurrent.atomic.AtomicBoolean

private object TranslatorBackend {
    suspend fun translate(text: String, targetLang: String): String = withContext(Dispatchers.IO) {
        if (text.isBlank()) return@withContext text

        try {
            val encoded = URLEncoder.encode(text, StandardCharsets.UTF_8.toString())
            val urlStr =
                "https://translate.googleapis.com/translate_a/single?client=gtx&sl=auto&tl=$targetLang&dt=t&q=$encoded"

            val connection = URL(urlStr).openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("User-Agent", "Mozilla/5.0")
            connection.connectTimeout = 5000
            connection.readTimeout = 5000

            val response = connection.inputStream.bufferedReader().use { it.readText() }
            connection.disconnect()

            // 解析: [[["翻译","原文",...]]
            Regex("""\[\[\["(.*?)",".*?"""").find(response)
                ?.groupValues?.get(1)
                ?.replace("\\n", "\n")
                ?.replace("\\\"", "\"")
                ?.replace("\\\\", "\\")
                ?: text
        } catch (e: Exception) {
            KLogger.de { "翻译失败: ${text.take(30)}..." }
            text
        }
    }
}

private class TranslationState {
    val isTranslating = AtomicBoolean(false)
    var pendingTexts = mutableListOf<String>()
    var translations = mutableMapOf<String, String>()
}

private val translationStates = mutableMapOf<String, TranslationState>()
private val json = Json { ignoreUnknownKeys = true }

private fun generateTranslationScript(): String {
    return """
(function() {
    if (window.__translatorReady) {
        console.log('[翻译] 已注入');
        return;
    }
    window.__translatorReady = true;
    
    // 创建按钮
    var btn = document.createElement('button');
    btn.id = '__translate_btn';
    btn.innerHTML = '🌐 翻译';
    btn.style.cssText = 
        'position:fixed!important;top:10px!important;right:10px!important;' +
        'z-index:2147483647!important;padding:8px 16px!important;' +
        'background:#4285f4!important;color:white!important;border:none!important;' +
        'border-radius:6px!important;cursor:pointer!important;font-size:14px!important;' +
        'box-shadow:0 2px 8px rgba(0,0,0,0.3)!important;font-weight:500!important;' +
        'transition:all 0.3s!important;';
    
    // 悬停效果
    btn.onmouseenter = function() {
        if (!this.disabled) {
            this.style.background = '#3367d6';
        }
    };
    btn.onmouseleave = function() {
        if (!this.disabled) {
            this.style.background = '#4285f4';
        }
    };
    
    // 收集文本节点
    var textNodes = [];
    var textIndex = {};
    
    function collectTexts() {
        textNodes = [];
        textIndex = {};
        var uniqueTexts = [];
        
        function collect(node) {
            if (node.nodeType === 3) {
                var text = node.textContent.trim();
                if (text && text.length > 1 && !/^[\d\s\p{P}]+$/u.test(text)) {
                    textNodes.push({ node: node, text: text });
                    if (!textIndex[text]) {
                        textIndex[text] = [];
                        uniqueTexts.push(text);
                    }
                    textIndex[text].push(textNodes.length - 1);
                }
            } else if (node.nodeType === 1) {
                var skip = ['SCRIPT','STYLE','NOSCRIPT','CODE','PRE','SVG','IFRAME'];
                if (!skip.includes(node.tagName)) {
                    Array.from(node.childNodes).forEach(collect);
                }
            }
        }
        
        collect(document.body || document.documentElement);
        return uniqueTexts;
    }
    
    // 应用翻译
    window.__applyTranslations = function(translations) {
        console.log('[翻译] 应用翻译结果:', Object.keys(translations).length);
        
        try {
            Object.keys(translations).forEach(function(original) {
                var translated = translations[original];
                var indices = textIndex[original] || [];
                
                indices.forEach(function(idx) {
                    if (textNodes[idx]) {
                        textNodes[idx].node.textContent = translated;
                    }
                });
            });
            
            btn.innerHTML = '✓ 已翻译';
            btn.style.background = '#34a853';
            btn.disabled = false;
            
            setTimeout(function() {
                btn.innerHTML = '🌐 翻译';
                btn.style.background = '#4285f4';
            }, 3000);
        } catch(e) {
            console.error('[翻译] 应用失败:', e);
            btn.innerHTML = '✗ 失败';
            btn.style.background = '#ea4335';
            btn.disabled = false;
        }
    };
    
    // 点击事件
    btn.onclick = function() {
        if (this.disabled) return;
        
        this.disabled = true;
        this.innerHTML = '⏳ 收集中...';
        this.style.background = '#fbbc04';
        
        var uniqueTexts = collectTexts();
        
        if (uniqueTexts.length === 0) {
            this.innerHTML = '⚠️ 无文本';
            this.style.background = '#ea4335';
            this.disabled = false;
            return;
        }
        
        console.log('[翻译] 找到', uniqueTexts.length, '个不同文本，共', textNodes.length, '个节点');
        
        // 存储到window，让Kotlin读取
        window.__textsToTranslate = uniqueTexts;
        window.__translationReady = true;
        
        this.innerHTML = '⏳ 翻译中...';
    };
    
    // 添加到页面
    (function addBtn() {
        if (document.body) {
            document.body.appendChild(btn);
            console.log('[翻译] 按钮已添加');
        } else {
            setTimeout(addBtn, 100);
        }
    })();
})();
    """.trimIndent()
}

private fun generateApplyTranslationScript(translations: Map<String, String>): String {
    val translationsJson = json.encodeToString(translations)
        .replace("\\", "\\\\")
        .replace("'", "\\'")

    return """
(function() {
    try {
        var translations = JSON.parse('$translationsJson');
        window.__applyTranslations(translations);
        window.__translationReady = false;
        window.__textsToTranslate = null;
    } catch(e) {
        console.error('[翻译] 注入结果失败:', e);
        document.getElementById('__translate_btn').innerHTML = '✗ 失败';
        document.getElementById('__translate_btn').style.background = '#ea4335';
        document.getElementById('__translate_btn').disabled = false;
    }
})();
    """.trimIndent()
}

fun injectTranslationScript(browser: CefBrowser?, url: String) {
    if (browser == null) return

    // todo use js -> kotlin native solve
//    try {
//        browser.executeJavaScript(generateTranslationScript(), url, 0)
//        KLogger.dd { "翻译脚本已注入" }
//    } catch (e: Exception) {
//        KLogger.de { "注入翻译脚本失败: ${e.message}" }
//    }
}

fun startTranslationLoop(browser: KCEFBrowser, scope: CoroutineScope, targetLang: String = "zh-CN") {
    val browserId = browser.identifier.toString()
    val state = translationStates.getOrPut(browserId) { TranslationState() }

    scope.launch {
        while (true) {
            kotlinx.coroutines.delay(500)

            if (!state.isTranslating.get()) {
                val hasTexts = withContext(Dispatchers.Main) {
                    var result = false
                    browser.evaluateJavaScript("window.__translationReady && window.__textsToTranslate ? true : false") { ready ->
                        result = ready == "true"
                    }
                    kotlinx.coroutines.delay(100)
                    result
                }

                if (hasTexts && state.isTranslating.compareAndSet(false, true)) {
                    val texts = mutableListOf<String>()
                    withContext(Dispatchers.Main) {
                        browser.evaluateJavaScript("JSON.stringify(window.__textsToTranslate || [])") { jsonString ->
                            try {
                                texts.addAll(json.decodeFromString<List<String>>(jsonString ?: "[]"))
                            } catch (e: Exception) {
                                KLogger.de { "解析待翻译文本失败: ${e.message}" }
                            }
                        }
                        kotlinx.coroutines.delay(200)
                    }

                    if (texts.isNotEmpty()) {
                        KLogger.dd { "开始翻译 ${texts.size} 个文本" }

                        val translations = mutableMapOf<String, String>()
                        texts.chunked(15).forEachIndexed { batchIdx, batch ->
                            KLogger.dd { "翻译批次 ${batchIdx + 1}/${(texts.size + 14) / 15}" }
                            batch.forEach { text ->
                                translations[text] = TranslatorBackend.translate(text, targetLang)
                            }
                            kotlinx.coroutines.delay(300)
                        }

                        KLogger.dd { "翻译完成，注入结果" }

                        withContext(Dispatchers.Main) {
                            browser.executeJavaScript(
                                generateApplyTranslationScript(translations),
                                browser.url ?: "",
                                0
                            )
                        }
                    }

                    state.isTranslating.set(false)
                }
            }
        }
    }
}
