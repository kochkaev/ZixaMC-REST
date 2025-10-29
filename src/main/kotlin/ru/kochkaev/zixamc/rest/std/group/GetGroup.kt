package ru.kochkaev.zixamc.rest.std.group

import io.ktor.http.HttpStatusCode
import ru.kochkaev.zixamc.rest.method.MethodResult
import ru.kochkaev.zixamc.rest.method.RestMapping
import ru.kochkaev.zixamc.rest.method.RestMethodType
import ru.kochkaev.zixamc.rest.std.Permissions

object GetGroup: RestMethodType<GetGroup.Request, GroupData>(
    path = "std/getGroup",
    requiredPermissions = listOf(Permissions.READ_GROUP),
    mapping = RestMapping.POST,
    params = mapOf(),
    bodyModel = Request::class.java,
    result = MethodResult.create(),
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