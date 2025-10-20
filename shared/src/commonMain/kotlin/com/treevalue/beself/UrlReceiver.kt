package com.treevalue.beself

import com.treevalue.beself.net.isUrlAllowed
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow

data class UrlEvent(val url: String, private val timestamp: Long = System.currentTimeMillis())

object UrlReceiver {

    private val _newUrlState = MutableStateFlow<UrlEvent?>(null)
    private val _hasNewUrl = atomic(false)
    val urlHotFlow = _newUrlState.asSharedFlow()
    private val _hasInit = atomic(false)
    fun canExecOnce(): Boolean {
        if (_hasInit.value) {
            return false
        }
        return _hasInit.compareAndSet(false, update = true)
    }

    fun receiveUrl(url: String) {
        _newUrlState.value = UrlEvent(url)
        _hasNewUrl.value = true
    }

    fun getAndClearUrl(): String? {
        if (_hasNewUrl.compareAndSet(expect = true, update = false)) {
            val currentUrl = _newUrlState.value
            _newUrlState.value = null
            return currentUrl?.url
        }
        return null
    }
}
