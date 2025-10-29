package ru.kochkaev.zixamc.rest.std

import ru.kochkaev.zixamc.api.sql.SQLUser
import ru.kochkaev.zixamc.api.sql.chatdata.ChatDataType
import ru.kochkaev.zixamc.api.sql.data.AccountType

data class UserData(
    val userId: Long,
    val nickname: String? = null,
    val nicknames: List<String>? = null,
    val accountType: AccountType? = null,
    val agreedWithRules: Boolean? = null,
    val isRestricted: Boolean? = null,
    val tempArray: List<String>? = null,
    val data: Map<ChatDataType<*>, Any>? = null,
) {
    companion object {
        fun get(userId: Long): UserData? {
            val sql = SQLUser.get(userId) ?: return null
            return UserData(
                userId = userId,
                nickname = sql.nickname,
                nicknames = sql.nicknames.get(),
                accountType = sql.accountType,
                agreedWithRules = sql.agreedWithRules,
                isRestricted = sql.isRestricted,
                tempArray = sql.tempArray.get(),
                data = sql.data.getAll(),
            )
        }
    }
}
