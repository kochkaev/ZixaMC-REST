package ru.kochkaev.zixamc.rest.std.user

import io.ktor.http.HttpStatusCode
import ru.kochkaev.zixamc.api.sql.SQLUser
import ru.kochkaev.zixamc.rest.method.MethodResult
import ru.kochkaev.zixamc.rest.method.MethodResults
import ru.kochkaev.zixamc.rest.method.RestMapping
import ru.kochkaev.zixamc.rest.method.RestMethodType
import ru.kochkaev.zixamc.rest.method.methodResult
import ru.kochkaev.zixamc.rest.method.result
import ru.kochkaev.zixamc.rest.std.Permissions

object DeleteUser: RestMethodType<DeleteUser.Request, Unit>(
    path = "std/deleteUser",
    requiredPermissions = listOf(Permissions.DELETE_USER),
    mapping = RestMapping.DELETE,
    params = mapOf(),
    bodyModel = Request::class.java,
    result = MethodResults.create(HttpStatusCode.NotImplemented,
        HttpStatusCode.NotImplemented to "User deletion is not implemented yet".methodResult(),
        HttpStatusCode.BadRequest to "Request body is empty".methodResult(),
        HttpStatusCode.NotFound to "User not found".methodResult(),
    ),
    method = { sql, permissions, params, body ->
        if (body == null) {
            HttpStatusCode.BadRequest.result("Request body is required")
        } else if (!SQLUser.exists(body.userId)) {
            HttpStatusCode.NotFound.result("User not found: ${body.userId}")
        } else {
            // SQLUser.delete(body.userId)
            // HttpStatusCode.OK to "User deleted: ${body.userId}"
            HttpStatusCode.NotImplemented
        }
    }
) {
    data class Request(
        val userId: Long,
    )
}