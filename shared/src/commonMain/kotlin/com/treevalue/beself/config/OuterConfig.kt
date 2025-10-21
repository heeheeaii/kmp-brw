package com.treevalue.beself.config

import com.treevalue.beself.backend.FunctionBackend

enum class LangType {
    CN, EN
}

object OuterConfig {
    var langType = FunctionBackend.getLanguage()
}
