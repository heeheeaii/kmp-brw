package com.treevalue.beself.data

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.reflect.KClass

class BrowserViewModel(private val savedStateHandle: SavedStateHandle) : ViewModel() {
    private val _forceDark = MutableStateFlow(savedStateHandle.get<Boolean>("forceDark") ?: true)
    val forceDark: StateFlow<Boolean> = _forceDark.asStateFlow()

    fun toggleForceDark() {
        _forceDark.update { !it }
        savedStateHandle["forceDark"] = _forceDark.value
    }
}


class CustomViewModelFactory : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: KClass<T>, extras: CreationExtras): T {
        val savedStateHandle = extras.createSavedStateHandle()
        return when {
            modelClass == BrowserViewModel::class -> {
                @Suppress("UNCHECKED_CAST")
                BrowserViewModel(savedStateHandle) as T
            }

            else -> {
                throw IllegalArgumentException("Unknown ViewModel class")
            }
        }
    }
}
