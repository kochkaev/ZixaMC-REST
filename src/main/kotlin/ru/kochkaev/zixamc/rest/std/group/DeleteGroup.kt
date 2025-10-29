package ru.kochkaev.zixamc.rest.std.group

import io.ktor.http.HttpStatusCode
import ru.kochkaev.zixamc.api.sql.SQLUser
import ru.kochkaev.zixamc.rest.RestMapping
import ru.kochkaev.zixamc.rest.RestMethodType
import ru.kochkaev.zixamc.rest.std.Permissions

object DeleteGroup: RestMethodType<DeleteGroup.Request>(
    path = "std/deleteUser",
    requiredPermissions = listOf(Permissions.DELETE_GROUP),
    mapping = RestMapping.DELETE,
    params = mapOf(),
    bodyModel = Request::class.java,
    method = { sql, permissions, params, body ->
        if (body == null) {
            HttpStatusCode.BadRequest to "Request body is required"
        } else if (!SQLUser.exists(body.chatId)) {
            HttpStatusCode.NotFound to "Group not found: ${body.chatId}"
        } else {
            // SQLGroup.delete(body.chatId)
            // HttpStatusCode.OK to "Group deleted: ${body.chatId}"
            HttpStatusCode.NotImplemented to "Group deletion is not implemented yet"
        }
    }
) {
    data class Request(
        val chatId: Long,
    )
}