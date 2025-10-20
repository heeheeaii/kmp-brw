package com.treevalue.beself.net

import com.treevalue.beself.randomUUID


enum class SiteStatus {
    PENDING,    // 等待验证
    COMPLETED,  // 验证成功
    FAILED      // 验证失败
}

data class SiteInfo(
    val id: String = randomUUID(),
    val label: String,
    val host: String,
    val status: SiteStatus = SiteStatus.PENDING,
    val originalUrl: String? = null
)
