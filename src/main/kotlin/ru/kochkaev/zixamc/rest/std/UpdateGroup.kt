package ru.kochkaev.zixamc.rest.std

import io.ktor.http.HttpStatusCode
import ru.kochkaev.zixamc.api.sql.SQLGroup
import ru.kochkaev.zixamc.api.sql.SQLUser
import ru.kochkaev.zixamc.rest.RestMapping
import ru.kochkaev.zixamc.rest.RestMethodType

object UpdateGroup: RestMethodType<GroupData>(
    path = "std/updateGroup",
    requiredPermissions = listOf(Permissions.WRITE_GROUP_OVERRIDE),
    mapping = RestMapping.PUT,
    params = mapOf(),
    bodyModel = GroupData::class.java,
    method = { sql, permissions, params, body ->
        if (body == null) {
            HttpStatusCode.BadRequest to "Request body is required"
        } else {
            val sql = SQLGroup.get(body.chatId)
            if (sql == null) HttpStatusCode.NotFound to "Group not found: ${body.chatId}"
            else if (body.name != null && !sql.canTakeName(body.name)) {
                HttpStatusCode.Conflict to "Name or alias already taken: ${body.name}"
            }
            else body.aliases?.firstNotNullOfOrNull { if (!sql.canTakeName(it)) HttpStatusCode.Conflict to "Name or alias already taken: $it" else null } ?: run {
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
                    HttpStatusCode.Forbidden to null
                } else {
                    HttpStatusCode.OK to GroupData.get(body.chatId)
                }
            }
        }
    }
)