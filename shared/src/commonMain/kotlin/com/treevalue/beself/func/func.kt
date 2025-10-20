import kotlinx.coroutines.*
import kotlinx.datetime.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * 函数调用限制器
 * 限制在指定时间窗口内只能调用一次函数
 */
class FuncLimiter {

    // 存储每个key的最后调用时间
    private val lastCallTimes = mutableMapOf<String, Long>()

    /**
     * 限制调用频率
     * @param intervalMs 限制间隔（毫秒）
     * @param key 调用标识，默认使用"default"
     * @param block 要执行的代码块
     * @return 如果在限制时间内重复调用则返回null，否则返回执行结果
     */
    fun <T> call(intervalMs: Long, key: String = "default", block: () -> T): T? {
        val currentTime = Clock.System.now().toEpochMilliseconds()
        val lastTime = lastCallTimes[key] ?: 0L

        return if (currentTime - lastTime >= intervalMs) {
            lastCallTimes[key] = currentTime
            block()
        } else {
            null
        }
    }

    /**
     * 使用 Duration 的版本
     * @param interval 限制间隔
     * @param key 调用标识
     * @param block 要执行的代码块
     * @return 如果在限制时间内重复调用则返回null，否则返回执行结果
     */
    fun <T> call(interval: Duration, key: String = "default", block: () -> T): T? {
        return call(interval.inWholeMilliseconds, key, block)
    }

    /**
     * 协程版本：限制调用频率（等待到可以调用为止）
     * @param intervalMs 限制间隔（毫秒）
     * @param key 调用标识
     * @param block 要执行的代码块
     * @return 执行结果
     */
    suspend fun <T> callSuspend(intervalMs: Long, key: String = "default", block: suspend () -> T): T {
        val currentTime = Clock.System.now().toEpochMilliseconds()
        val lastTime = lastCallTimes[key] ?: 0L
        val waitTime = intervalMs - (currentTime - lastTime)

        if (waitTime > 0) {
            delay(waitTime)
        }

        lastCallTimes[key] = Clock.System.now().toEpochMilliseconds()
        return block()
    }

    /**
     * 使用 Duration 的协程版本
     */
    suspend fun <T> callSuspend(interval: Duration, key: String = "default", block: suspend () -> T): T {
        return callSuspend(interval.inWholeMilliseconds, key, block)
    }

    /**
     * 协程版本：尝试调用，如果在限制时间内则返回null
     * @param intervalMs 限制间隔（毫秒）
     * @param key 调用标识
     * @param block 要执行的代码块
     * @return 如果在限制时间内重复调用则返回null，否则返回执行结果
     */
    suspend fun <T> tryCallSuspend(intervalMs: Long, key: String = "default", block: suspend () -> T): T? {
        val currentTime = Clock.System.now().toEpochMilliseconds()
        val lastTime = lastCallTimes[key] ?: 0L

        return if (currentTime - lastTime >= intervalMs) {
            lastCallTimes[key] = currentTime
            block()
        } else {
            null
        }
    }

    /**
     * 使用 Duration 的协程尝试调用版本
     */
    suspend fun <T> tryCallSuspend(interval: Duration, key: String = "default", block: suspend () -> T): T? {
        return tryCallSuspend(interval.inWholeMilliseconds, key, block)
    }

    /**
     * 清除指定key的调用记录
     */
    fun clearKey(key: String) {
        lastCallTimes.remove(key)
    }

    /**
     * 清除所有调用记录
     */
    fun clearAll() {
        lastCallTimes.clear()
    }

    /**
     * 检查是否可以调用
     */
    fun canCall(intervalMs: Long, key: String = "default"): Boolean {
        val currentTime = Clock.System.now().toEpochMilliseconds()
        val lastTime = lastCallTimes[key] ?: 0L
        return currentTime - lastTime >= intervalMs
    }

    /**
     * Duration 版本的检查是否可以调用
     */
    fun canCall(interval: Duration, key: String = "default"): Boolean {
        return canCall(interval.inWholeMilliseconds, key)
    }

    /**
     * 获取距离下次可调用的剩余时间（毫秒）
     */
    fun getRemainingTime(intervalMs: Long, key: String = "default"): Long {
        val currentTime = Clock.System.now().toEpochMilliseconds()
        val lastTime = lastCallTimes[key] ?: 0L
        val remaining = intervalMs - (currentTime - lastTime)
        return if (remaining > 0) remaining else 0
    }

    /**
     * Duration 版本的获取剩余时间
     */
    fun getRemainingTime(interval: Duration, key: String = "default"): Duration {
        return getRemainingTime(interval.inWholeMilliseconds, key).milliseconds
    }
}


// 辅助扩展函数，提供更简洁的API
/**
 * 创建一个专用的函数限制器
 */
fun createLimiter(intervalMs: Long, key: String = "default") = object {
    private val limiter = FuncLimiter()

    fun <T> invoke(block: () -> T): T? = limiter.call(intervalMs, key, block)
    suspend fun <T> invokeSuspend(block: suspend () -> T): T = limiter.callSuspend(intervalMs, key, block)
    fun canCall(): Boolean = limiter.canCall(intervalMs, key)
}

/**
 * Duration 版本的专用限制器
 */
fun createLimiter(interval: Duration, key: String = "default") = object {
    private val limiter = FuncLimiter()

    fun <T> invoke(block: () -> T): T? = limiter.call(interval, key, block)
    suspend fun <T> invokeSuspend(block: suspend () -> T): T = limiter.callSuspend(interval, key, block)
    fun canCall(): Boolean = limiter.canCall(interval, key)
}
