package com.treevalue.beself

// commonTest
import FuncLimiter
import com.treevalue.beself.encrypt.EncryptionService
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock
import org.junit.Test
import kotlin.time.Duration.Companion.milliseconds

class CommonTests {
    @Test
    fun testExample() {
        val deviceId = "12345"
        val decryptionPassword = "mySecretPassword"
        val obfuscationChars = "randomChars"

        val encryptedString = EncryptionService.generateEncryptedString(
            deviceId = deviceId,
            decryptionPassword = decryptionPassword,
            obfuscationChars = obfuscationChars
        )
        

        val decryptedData = EncryptionService.decryptEncryptedString(encryptedString)
        
    }

    @Test
    fun testFuncLimiter() {
        runBlocking {
            val funcLimiter = FuncLimiter()
            fun testFunction(a: Int, b: Int, c: Int): String {
                return "结果: ${a + b + c}"
            }

            val result1 = funcLimiter.call(100) { testFunction(1, 2, 3) }
            

            val result2 = funcLimiter.call(100) { testFunction(4, 5, 6) }
            

            delay(110) // 等待超过限制时间
            val result3 = funcLimiter.call(100) { testFunction(7, 8, 9) }
            

            
            val durationResult1 = funcLimiter.call(200.milliseconds) { "使用Duration的调用" }
            

            val durationResult2 = funcLimiter.call(200.milliseconds) { "第二次Duration调用" }
            

            
            val apiResult1 = funcLimiter.call(200, "api1") { "API1调用成功" }
            val apiResult2 = funcLimiter.call(200, "api2") { "API2调用成功" }
            
            

            
            val startTime = Clock.System.now().toEpochMilliseconds()
            val suspendResult = funcLimiter.callSuspend(500) { "等待后执行" }
            val endTime = Clock.System.now().toEpochMilliseconds()
            
            

            // 示例5：检查调用状态
            
            
            

            // 示例6：Duration版本的状态检查
            
            

            // 示例7：协程尝试调用
            
            val tryResult1 = funcLimiter.tryCallSuspend(300) { "协程尝试调用1" }
            

            val tryResult2 = funcLimiter.tryCallSuspend(300) { "协程尝试调用2" }
            
        }
    }
}
