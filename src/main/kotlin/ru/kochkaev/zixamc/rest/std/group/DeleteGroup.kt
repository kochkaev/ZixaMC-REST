package ru.kochkaev.zixamc.rest.std.group

import io.ktor.http.HttpStatusCode
import ru.kochkaev.zixamc.api.sql.SQLUser
import ru.kochkaev.zixamc.rest.method.MethodResult
import ru.kochkaev.zixamc.rest.method.MethodResults
import ru.kochkaev.zixamc.rest.method.RestMapping
import ru.kochkaev.zixamc.rest.method.RestMethodType
import ru.kochkaev.zixamc.rest.method.methodResult
import ru.kochkaev.zixamc.rest.method.result
import ru.kochkaev.zixamc.rest.openAPI.RestHiddenIfNoPerm
import ru.kochkaev.zixamc.rest.std.Permissions

object DeleteGroup: RestMethodType<DeleteGroup.Request, Unit>(
    path = "std/deleteGroup",
    requiredPermissions = listOf(Permissions.DELETE_GROUP),
    mapping = RestMapping.DELETE,
    params = mapOf(),
    bodyModel = Request::class.java,
    result = MethodResults.create(HttpStatusCode.NotImplemented,
        HttpStatusCode.NotImplemented to "Group deletion is not implemented yet".methodResult(),
        HttpStatusCode.NotFound to "Group not found".methodResult(),
        HttpStatusCode.BadRequest to "Request body is empty".methodResult(),
    ),
    method = { sql, permissions, params, body ->
        if (body == null) {
            HttpStatusCode.BadRequest.result("Request body is required")
        } else if (!SQLUser.exists(body.chatId)) {
            HttpStatusCode.NotFound.result("Group not found: ${body.chatId}")
        } else {
            // SQLGroup.delete(body.chatId)
            // HttpStatusCode.OK to "Group deleted: ${body.chatId}"
            HttpStatusCode.NotImplemented
        }
    }
) {
    data class Request(
        val chatId: Long,
    )
}