package ru.kochkaev.zixamc.rest.std.group

import io.ktor.http.HttpStatusCode
import ru.kochkaev.zixamc.api.sql.SQLGroup
import ru.kochkaev.zixamc.rest.RestMapping
import ru.kochkaev.zixamc.rest.RestMethodType
import ru.kochkaev.zixamc.rest.std.Permissions

object AddGroupMember: RestMethodType<AddGroupMember.Request>(
    path = "std/addGroupMember",
    requiredPermissions = listOf(Permissions.WRITE_GROUP_MEMBERS),
    mapping = RestMapping.PUT,
    params = mapOf(),
    bodyModel = Request::class.java,
    method = { sql, permissions, params, body ->
        if (body == null) {
            HttpStatusCode.BadRequest to "Request body is required"
        } else {
            val group = SQLGroup.get(body.chatId)
            if (group == null) {
                HttpStatusCode.NotFound to "Group not found: ${body.chatId}"
            } else {
                group.members.add(body.userId)
                HttpStatusCode.OK to GroupData.get(group.id)
            }
        }
    }
) {
    data class Request(
        val chatId: Long,
        val userId: Long,
    )
}