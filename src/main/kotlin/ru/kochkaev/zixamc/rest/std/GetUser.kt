package ru.kochkaev.zixamc.rest.std

import io.ktor.http.HttpStatusCode
import ru.kochkaev.zixamc.rest.RestMapping
import ru.kochkaev.zixamc.rest.RestMethodType

object GetUser: RestMethodType<GetUser.GetUserRequest>(
    path = "std/getUser",
    requiredPermissions = listOf(Permissions.READ_USER),
    mapping = RestMapping.GET,
    params = mapOf(),
    bodyModel = GetUserRequest::class.java,
    method = { sql, permissions, params, body ->
        if (body == null) {
            HttpStatusCode.BadRequest to "Request body is required"
        } else {
            val user = UserData.get(body.userId)
            if (user == null) {
                HttpStatusCode.NotFound to "User not found: ${body.userId}"
            } else HttpStatusCode.OK to user
        }
    }
) {
    data class GetUserRequest(
        val userId: Long,
    )
}