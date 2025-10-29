package ru.kochkaev.zixamc.rest.std

import io.ktor.http.HttpStatusCode
import ru.kochkaev.zixamc.api.sql.SQLUser
import ru.kochkaev.zixamc.rest.RestMapping
import ru.kochkaev.zixamc.rest.RestMethodType

object DeleteUser: RestMethodType<DeleteUser.DeleteUserRequest>(
    path = "std/deleteUser",
    requiredPermissions = listOf(Permissions.DELETE_USER),
    mapping = RestMapping.DELETE,
    params = mapOf(),
    bodyModel = DeleteUserRequest::class.java,
    method = { sql, permissions, params, body ->
        if (body == null) {
            HttpStatusCode.BadRequest to "Request body is required"
        } else if (!SQLUser.exists(body.userId)) {
            HttpStatusCode.NotFound to "User not found: ${body.userId}"
        } else {
            // SQLUser.delete(body.userId)
            // HttpStatusCode.OK to "User deleted: ${body.userId}"
            HttpStatusCode.NotImplemented to "User deletion is not implemented yet"
        }
    }
) {
    data class DeleteUserRequest(
        val userId: Long,
    )
}