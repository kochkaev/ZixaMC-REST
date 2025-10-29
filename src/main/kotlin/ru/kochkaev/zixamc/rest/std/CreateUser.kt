package ru.kochkaev.zixamc.rest.std

import io.ktor.http.HttpStatusCode
import ru.kochkaev.zixamc.api.sql.SQLUser
import ru.kochkaev.zixamc.api.sql.chatdata.ChatDataType
import ru.kochkaev.zixamc.api.sql.chatdata.ChatDataTypes
import ru.kochkaev.zixamc.rest.RestMapping
import ru.kochkaev.zixamc.rest.RestMethodType
import kotlin.collections.contains

object CreateUser: RestMethodType<UserData>(
    path = "std/createUser",
    requiredPermissions = listOf(Permissions.CREATE_USER),
    mapping = RestMapping.POST,
    params = mapOf(),
    bodyModel = UserData::class.java,
    method = { sql, permissions, params, body ->
        if (body == null) {
            HttpStatusCode.BadRequest to "Request body is required"
        } else {
            if (SQLUser.exists(body.userId)) {
                HttpStatusCode.Conflict to "User already exists: ${body.userId}"
            }
            else if (body.nickname != null && SQLUser.exists(body.nickname)) {
                HttpStatusCode.Conflict to "Nickname already taken: ${body.nickname}"
            }
            else {
                body.nicknames?.firstNotNullOfOrNull { if (SQLUser.exists(it)) HttpStatusCode.Conflict to "Nickname already taken: $it" else null } ?: run {
                    SQLUser.create(
                        userId = body.userId,
                        nickname = body.nickname,
                        nicknames = body.nicknames,
                        accountType = body.accountType?.id,
                        data = body.data ?: mapOf<ChatDataType<*>, Any>()
                    )
                    HttpStatusCode.Created to UserData.get(body.userId)
                }
            }
        }
    }
)