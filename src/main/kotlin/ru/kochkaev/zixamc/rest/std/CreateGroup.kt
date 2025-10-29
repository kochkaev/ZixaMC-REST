package ru.kochkaev.zixamc.rest.std

import io.ktor.http.HttpStatusCode
import ru.kochkaev.zixamc.api.sql.SQLGroup
import ru.kochkaev.zixamc.api.sql.SQLUser
import ru.kochkaev.zixamc.api.sql.chatdata.ChatDataType
import ru.kochkaev.zixamc.api.sql.chatdata.ChatDataTypes
import ru.kochkaev.zixamc.rest.RestMapping
import ru.kochkaev.zixamc.rest.RestMethodType
import kotlin.collections.contains

object CreateGroup: RestMethodType<GroupData>(
    path = "std/createGroup",
    requiredPermissions = listOf(Permissions.CREATE_GROUP),
    mapping = RestMapping.POST,
    params = mapOf(),
    bodyModel = GroupData::class.java,
    method = { sql, permissions, params, body ->
        if (body == null) {
            HttpStatusCode.BadRequest to "Request body is required"
        } else {
            if (SQLGroup.exists(body.chatId)) {
                HttpStatusCode.Conflict to "Group already exists: ${body.chatId}"
            }
            else if (body.name != null && SQLGroup.exists(body.name)) {
                HttpStatusCode.Conflict to "Name or alias already taken: ${body.name}"
            }
            else {
                body.aliases?.firstNotNullOfOrNull { if (SQLGroup.exists(it)) HttpStatusCode.Conflict to "Name or alias already taken: $it" else null } ?: run {
                    SQLGroup.create(
                        chatId = body.chatId,
                        name = body.name,
                        aliases = body.aliases,
                        members = body.members?.map { it.toString() } ?: listOf(),
                        agreedWithRules = body.agreedWithRules ?: false,
                        isRestricted = body.isRestricted ?: false,
                        features = body.features ?: mapOf(),
                        data = body.data ?: mapOf<ChatDataType<*>, Any>(),
                    )
                    HttpStatusCode.Created to UserData.get(body.chatId)
                }
            }
        }
    }
)