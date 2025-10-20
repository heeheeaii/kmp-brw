package com.treevalue.beself.data

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import com.treevalue.beself.randomUUID

data class TabInfo(
    val id: String = randomUUID(),
    val initialUrl: String? = null,
    val initialHtml: String? = null,
    var title: MutableState<String> = mutableStateOf("Home")
)
