package com.treevalue.beself.persistence

import kotlinx.serialization.Serializable

@Serializable
data class DeleteRecord(
    val hostOrId: String,
    val deleteCount: Int,
    val lastDeleteTime: Long
)

@Serializable
data class DeleteRecordState(
    val records: List<DeleteRecord> = emptyList()
)

data class DeleteRestriction(
    val message: String
) {
    companion object {
        val NONE = DeleteRestriction("")
        val ONE_MONTH = DeleteRestriction("此网站已被删除1次，删除后一个月内无法再添加")
        val SIX_MONTHS = DeleteRestriction("此网站已被删除2次，删除后半年内无法再添加")
        val PERMANENT = DeleteRestriction("此网站已被删除3次，删除后将永远无法再添加")
    }
}
