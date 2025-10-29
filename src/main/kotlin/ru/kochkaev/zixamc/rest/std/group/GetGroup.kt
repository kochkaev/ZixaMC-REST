package ru.kochkaev.zixamc.rest.std.group

import io.ktor.http.HttpStatusCode
import ru.kochkaev.zixamc.rest.RestMapping
import ru.kochkaev.zixamc.rest.RestMethodType
import ru.kochkaev.zixamc.rest.std.Permissions

object GetGroup: RestMethodType<GetGroup.Request>(
    path = "std/getGroup",
    requiredPermissions = listOf(Permissions.READ_GROUP),
    mapping = RestMapping.GET,
    params = mapOf(),
    bodyModel = Request::class.java,
    method = { sql, permissions, params, body ->
        if (body == null) {
            HttpStatusCode.BadRequest to "Request body is required"
        } else {
            val group = GroupData.get(body.chatId)
            if (group == null) {
                HttpStatusCode.NotFound to "Group not found: ${body.chatId}"
            } else HttpStatusCode.OK to group
        }
    }
) {
    data class Request(
        val chatId: Long,
    )
}