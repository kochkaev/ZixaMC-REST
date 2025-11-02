package ru.kochkaev.zixamc.rest.std.group

import io.ktor.http.HttpStatusCode
import ru.kochkaev.zixamc.api.sql.SQLGroup
import ru.kochkaev.zixamc.api.sql.chatdata.ChatDataType
import ru.kochkaev.zixamc.rest.method.MethodResult
import ru.kochkaev.zixamc.rest.method.MethodResults
import ru.kochkaev.zixamc.rest.method.RestMapping
import ru.kochkaev.zixamc.rest.method.RestMethodType
import ru.kochkaev.zixamc.rest.method.methodResult
import ru.kochkaev.zixamc.rest.method.result
import ru.kochkaev.zixamc.rest.std.Permissions

object CreateGroup: RestMethodType<GroupData, GroupData>(
    path = "std/createGroup",
    requiredPermissions = listOf(Permissions.CREATE_GROUP),
    mapping = RestMapping.POST,
    params = mapOf(),
    bodyModel = GroupData::class.java,
    result = MethodResults.create(HttpStatusCode.Created,
        HttpStatusCode.BadRequest to "Request body is empty".methodResult(),
        HttpStatusCode.Conflict to "Group with that chatId is already exists or name or alias is already taken".methodResult(),
    ),
    method = { sql, permissions, params, body ->
        if (body == null) {
            HttpStatusCode.BadRequest.result("Request body is required")
        } else {
            if (SQLGroup.exists(body.chatId)) {
                HttpStatusCode.Conflict.result("Group already exists: ${body.chatId}")
            }
            else if (body.name != null && SQLGroup.exists(body.name)) {
                HttpStatusCode.Conflict.result("Name or alias already taken: ${body.name}")
            }
            else {
                body.aliases?.firstNotNullOfOrNull { if (SQLGroup.exists(it)) HttpStatusCode.Conflict.result("Name or alias already taken: $it") else null } ?: run {
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
                    HttpStatusCode.Created.result(GroupData.get(body.chatId))
                }
            }
        }
    }
)