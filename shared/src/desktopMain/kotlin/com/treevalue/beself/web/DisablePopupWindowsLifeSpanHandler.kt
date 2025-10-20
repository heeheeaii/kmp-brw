package com.treevalue.beself.web

import com.treevalue.beself.bus.EventBus
import com.treevalue.beself.bus.TabEvent
import com.treevalue.beself.util.KLogger
import com.treevalue.beself.util.dd
import com.treevalue.beself.util.de
import com.treevalue.beself.util.dw
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.cef.browser.CefBrowser
import org.cef.browser.CefFrame
import org.cef.handler.CefLifeSpanHandlerAdapter
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

class DisablePopupWindowsLifeSpanHandler : CefLifeSpanHandlerAdapter() {

    companion object {
        // 使用更严格的防重复机制
        private val recentPopupRequests = ConcurrentHashMap<String, Long>()
        private val processingUrls = ConcurrentHashMap<String, AtomicBoolean>()
        private const val DEBOUNCE_TIME = 2000L // 增加到2秒
        private const val MAX_CONCURRENT_REQUESTS = 3 // 限制同时处理的请求数量
    }

    override fun onBeforePopup(
        browser: CefBrowser?,
        frame: CefFrame?,
        target_url: String?,
        target_frame_name: String?,
    ): Boolean {
        target_url?.let { url ->
            val currentTime = System.currentTimeMillis()

            // 第一层防护：检查是否正在处理相同URL
            val isProcessing = processingUrls.computeIfAbsent(url) { AtomicBoolean(false) }
            if (!isProcessing.compareAndSet(false, true)) {
                KLogger.dd { "桌面端忽略正在处理的弹窗请求: $url" }
                return true
            }

            try {
                // 第二层防护：时间防重复
                val lastRequestTime = recentPopupRequests[url] ?: 0L
                if (currentTime - lastRequestTime < DEBOUNCE_TIME) {
                    KLogger.dd { "桌面端忽略重复弹窗请求（时间过短）: $url" }
                    return true
                }

                // 第三层防护：限制并发请求数量
                if (processingUrls.size > MAX_CONCURRENT_REQUESTS) {
                    KLogger.dw { "桌面端并发请求过多，忽略弹窗请求: $url" }
                    return true
                }

                // 更新最后请求时间
                recentPopupRequests[url] = currentTime

                KLogger.dd { "桌面端检测到弹窗请求: $url" }

                try {
                    // 使用更安全的事件发布
                    val event = TabEvent.RequestNewTab(url, target_frame_name ?: "")

                    // 异步发布，避免阻塞
                    GlobalScope.launch {
                        try {
                            EventBus.publishSync(event)
                            KLogger.dd { "桌面端发布新标签页事件: $url, 标题: $target_frame_name" }
                        } catch (e: Exception) {
                            KLogger.de { "发布新标签页事件失败: ${e.message}" }
                        }
                    }
                } catch (e: Exception) {
                    KLogger.de { "处理弹窗请求失败: ${e.message}" }
                }

            } finally {
                // 延迟重置处理状态
                GlobalScope.launch {
                    delay(DEBOUNCE_TIME)
                    isProcessing.set(false)

                    // 清理过期的记录
                    val expiredTime = System.currentTimeMillis() - DEBOUNCE_TIME
                    recentPopupRequests.entries.removeAll { it.value < expiredTime }

                    // 清理过期的处理状态
                    processingUrls.entries.removeAll { !it.value.get() }
                }
            }
        }
        return true // 阻止原生弹窗
    }
}
