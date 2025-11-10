package com.treevalue.beself.bus

import com.treevalue.beself.util.KLogger
import com.treevalue.beself.util.de
import kotlinx.atomicfu.AtomicBoolean
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap

object EventBus {
    val __scope: CoroutineScope = GlobalScope

    private val recentEvents = ConcurrentHashMap<String, Long>()
    private val eventProcessingFlags = ConcurrentHashMap<String, AtomicBoolean>()
    private const val EVENT_DEBOUNCE_TIME = 1000L // 1秒内不允许重复事件

    // 使用SharedFlow支持多个订阅者，replay=0表示不缓存事件
    private val _eventFlow = MutableSharedFlow<Event>(
        replay = 0,
        extraBufferCapacity = 64 // 缓冲区大小
    )
    val eventFlow: SharedFlow<Event> = _eventFlow.asSharedFlow()

    val __handlers = mutableMapOf<EventId, Job>()
    val __handlersMutex = Mutex()

    /**
     * 发布事件到所有监听者
     */
    fun publish(event: Event) {
        __scope.launch {
            val eventKey = "${event::class.simpleName}_${event.hashCode()}"
            val currentTime = System.currentTimeMillis()

            // 检查是否正在处理相同事件
            val isProcessing = eventProcessingFlags.computeIfAbsent(eventKey) { atomic(false) }
            if (!isProcessing.compareAndSet(false, true)) {
                return@launch
            }

            try {
                // 检查时间间隔
                val lastEventTime = recentEvents[eventKey] ?: 0L
                if (currentTime - lastEventTime < EVENT_DEBOUNCE_TIME) {

                    return@launch
                }

                // 更新时间并发布事件
                recentEvents[eventKey] = currentTime
                publishSync(event)

            } finally {
                // 延迟重置处理状态
                GlobalScope.launch {
                    delay(EVENT_DEBOUNCE_TIME)
                    isProcessing.value = false

                    // 清理过期记录
                    val expiredTime = System.currentTimeMillis() - EVENT_DEBOUNCE_TIME
                    recentEvents.entries.removeAll { it.value < expiredTime }
                    eventProcessingFlags.entries.removeAll { !it.value.value }
                }
            }
        }
    }

    /**
     * 非挂起版本的发布方法
     */
    fun publishSync(event: Event) {
        val success = _eventFlow.tryEmit(event)
        if (!success) {
            publish(event)
        }
    }

    /**
     * 注册事件处理器
     * 每个handlerId对应一个独立的监听器
     */
    inline fun <reified T : Event> registerHandler(
        handlerId: EventId,
        crossinline handler: (T) -> Unit,
    ) {
        __scope.launch {
            __handlersMutex.withLock {
                // 如果已经有相同ID的handler，先取消它
                __handlers[handlerId]?.cancel()

                // 创建新的handler job
                val job = __scope.launch {
                    try {
                        eventFlow.collect { event ->
                            if (event is T) {
                                try {
                                    handler(event)

                                } catch (e: Exception) {
                                    KLogger.de { e.toString() }

                                }
                            }
                        }
                    } catch (e: Exception) {
                        KLogger.de { e.toString() }
                    }
                }

                __handlers[handlerId] = job

            }
        }
    }

    /**
     * 注销指定的事件处理器
     */
    suspend fun unregisterHandler(handlerId: EventId) {
        __handlersMutex.withLock {
            __handlers[handlerId]?.cancel()
            __handlers.remove(handlerId)

        }
    }

    /**
     * 注销所有事件处理器
     */
    suspend fun unregisterAllHandlers() {
        __handlersMutex.withLock {
            __handlers.values.forEach { it.cancel() }
            __handlers.clear()

        }
    }

    /**
     * 获取当前活跃的处理器数量（用于调试）
     */
    suspend fun getActiveHandlerCount(): Int {
        return __handlersMutex.withLock {
            __handlers.size
        }
    }

    /**
     * 获取订阅者数量（用于调试）
     */
    fun getSubscriberCount(): Int {
        return _eventFlow.subscriptionCount.value
    }
}

// 事件接口和实现保持不变
interface Event

sealed class PopEvent : Event {
    data object AddSite : PopEvent()
    data object HelpPop : PopEvent()
    data object HidePop : PopEvent()
    data object SearchSite : PopEvent()
    data object FunctionMenu : PopEvent()
    data object SystemSettings : PopEvent()
    data object Calculator : PopEvent()
    data object Schedule : PopEvent()
    data object Compression : PopEvent()
    data object HideSite : PopEvent()
    data object BlockSite : PopEvent()
    data object GrabSite : PopEvent()
    data object StartPageSetting : PopEvent()
    data object OpenUrl : PopEvent()
    data object OtherFunctions : PopEvent()
}

sealed class DownloadEvent : Event {
    data class DownloadAvailable(val url: String, val filename: String) : DownloadEvent()
    data class AutoStartDownload(val url: String, val filename: String) : DownloadEvent()
    data class DownloadStarted(val taskId: String, val url: String, val filename: String) : DownloadEvent()
    data class DownloadProgress(val taskId: String, val progress: Float) : DownloadEvent()
    data class DownloadCompleted(val taskId: String) : DownloadEvent()
    data class DownloadFailed(val taskId: String, val error: String) : DownloadEvent()
}

sealed class TabEvent : Event {
    data class RequestNewTab(val url: String, val title: String?) : TabEvent()
    data class TabClosed(val tabId: String) : TabEvent()
}

enum class EventId {
    Pop,
    NewTab,
    Download,
    TabClosed,
    TabDisposed,
}
