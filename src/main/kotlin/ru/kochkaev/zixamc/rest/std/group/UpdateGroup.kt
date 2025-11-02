package ru.kochkaev.zixamc.rest.std.group

import io.ktor.http.HttpStatusCode
import ru.kochkaev.zixamc.api.sql.SQLGroup
import ru.kochkaev.zixamc.api.sql.SQLUser
import ru.kochkaev.zixamc.rest.method.MethodResult
import ru.kochkaev.zixamc.rest.method.MethodResults
import ru.kochkaev.zixamc.rest.method.RestMapping
import ru.kochkaev.zixamc.rest.method.RestMethodType
import ru.kochkaev.zixamc.rest.method.methodResult
import ru.kochkaev.zixamc.rest.method.result
import ru.kochkaev.zixamc.rest.std.Permissions

object UpdateGroup: RestMethodType<GroupData, GroupData>(
    path = "std/updateGroup",
    requiredPermissions = listOf(Permissions.WRITE_GROUP_OVERRIDE),
    mapping = RestMapping.PUT,
    params = mapOf(),
    bodyModel = GroupData::class.java,
    result = MethodResults.create(HttpStatusCode.OK,
        HttpStatusCode.BadRequest to "Request body is empty".methodResult(),
        HttpStatusCode.NotFound to "Group not found".methodResult(),
        HttpStatusCode.Conflict to "Name or alias is already taken".methodResult(),
    ),
    method = { sql, permissions, params, body ->
        if (body == null) {
            HttpStatusCode.BadRequest.result("Request body is required")
        } else {
            val sql = SQLGroup.get(body.chatId)
            if (sql == null) HttpStatusCode.NotFound.result("Group not found: ${body.chatId}")
            else if (body.name != null && !sql.canTakeName(body.name)) {
                HttpStatusCode.Conflict.result("Name or alias already taken: ${body.name}")
            }
            else body.aliases?.firstNotNullOfOrNull { if (!sql.canTakeName(it)) HttpStatusCode.Conflict.result("Name or alias already taken: $it") else null } ?: run {
                var forbidden = false
                when {
                    permissions.contains(Permissions.WRITE_GROUP_NAMES) && body.name != null ->
                        sql.name = body.name
                    permissions.contains(Permissions.WRITE_GROUP_NAMES) && body.aliases != null ->
                        sql.aliases.set(body.aliases)
                    permissions.contains(Permissions.WRITE_GROUP_MEMBERS) && body.members != null ->
                        sql.members.set(body.members.map { SQLUser.getWithoutCheck(it) })
                    permissions.contains(Permissions.WRITE_GROUP_AGREED_WITH_RULES) && body.agreedWithRules != null ->
                        sql.agreedWithRules = body.agreedWithRules
                    permissions.contains(Permissions.WRITE_GROUP_IS_RESTRICTED) && body.isRestricted != null ->
                        sql.isRestricted = body.isRestricted
                    permissions.contains(Permissions.WRITE_GROUP_FEATURES) && body.features != null ->
                        sql.features.setAll(body.features)
                    permissions.contains(Permissions.WRITE_GROUP_DATA) && body.data != null ->
                        sql.data.setAll(body.data)
                    else -> forbidden = true
                }
                if (forbidden) {
                    HttpStatusCode.Forbidden
                } else {
                    HttpStatusCode.OK.result(GroupData.get(body.chatId))
                }
            }
        }
    }
)