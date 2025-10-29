package ru.kochkaev.zixamc.rest.std

import ru.kochkaev.zixamc.api.sql.SQLGroup
import ru.kochkaev.zixamc.api.sql.SQLUser
import ru.kochkaev.zixamc.api.sql.chatdata.ChatDataType
import ru.kochkaev.zixamc.api.sql.data.AccountType
import ru.kochkaev.zixamc.api.sql.feature.FeatureType
import ru.kochkaev.zixamc.api.sql.feature.data.FeatureData

data class GroupData(
    val chatId: Long,
    val name: String? = null,
    val aliases: List<String>? = null,
    val members: List<Long>? = null,
    val agreedWithRules: Boolean? = null,
    val isRestricted: Boolean? = null,
    val features: Map<FeatureType<out FeatureData>, FeatureData>? = null,
    val data: Map<ChatDataType<*>, Any>? = null,
) {
    companion object {
        fun get(chatId: Long): GroupData? {
            val sql = SQLGroup.get(chatId) ?: return null
            return GroupData(
                chatId = chatId,
                name = sql.name,
                aliases = sql.aliases.get(),
                members = sql.members.get()?.map { it.id },
                agreedWithRules = sql.agreedWithRules,
                isRestricted = sql.isRestricted,
                features = sql.features.getAll(),
                data = sql.data.getAll(),
            )
        }
    }
}
