package ru.kochkaev.zixamc.rest.std.group

import io.ktor.http.HttpStatusCode
import ru.kochkaev.zixamc.api.sql.SQLGroup
import ru.kochkaev.zixamc.rest.method.MethodResult
import ru.kochkaev.zixamc.rest.method.RestMapping
import ru.kochkaev.zixamc.rest.method.RestMethodType
import ru.kochkaev.zixamc.rest.std.Permissions

object AddGroupAlias: RestMethodType<AddGroupAlias.Request, GroupData>(
    path = "std/addGroupAlias",
    requiredPermissions = listOf(Permissions.WRITE_GROUP_NAMES),
    mapping = RestMapping.PUT,
    params = mapOf(),
    bodyModel = Request::class.java,
    result = MethodResult.create(),
    method = { sql, permissions, params, body ->
        if (body == null) {
            HttpStatusCode.BadRequest to "Request body is required"
        } else {
            val group = SQLGroup.get(body.chatId)
            if (group == null) {
                HttpStatusCode.NotFound to "Group not found: ${body.chatId}"
            } else if (!SetGroupName.checkValidName(body.alias)) {
                HttpStatusCode.BadRequest to "Invalid group alias: ${body.alias}"
            } else if (!group.canTakeName(body.alias)) {
                HttpStatusCode.Conflict to "This alias is already taken: ${body.alias}"
            } else if (group.name == body.alias) {
                HttpStatusCode.Conflict to "Group already has this alias as name: ${body.alias}"
            } else {
                group.aliases.add(body.alias)
                HttpStatusCode.OK to GroupData.get(group.id)
            }
        }
    }
) {
    data class Request(
        val chatId: Long,
        val alias: String,
    )
}