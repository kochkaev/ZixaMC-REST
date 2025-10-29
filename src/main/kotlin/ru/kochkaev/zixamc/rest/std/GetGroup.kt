package ru.kochkaev.zixamc.rest.std

import io.ktor.http.HttpStatusCode
import ru.kochkaev.zixamc.rest.RestMapping
import ru.kochkaev.zixamc.rest.RestMethodType

object GetGroup: RestMethodType<GetGroup.GetGroupRequest>(
    path = "std/getGroup",
    requiredPermissions = listOf(Permissions.READ_GROUP),
    mapping = RestMapping.GET,
    params = mapOf(),
    bodyModel = GetGroupRequest::class.java,
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
    data class GetGroupRequest(
        val chatId: Long,
    )
}