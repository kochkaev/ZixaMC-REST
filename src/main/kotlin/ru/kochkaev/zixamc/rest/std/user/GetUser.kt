package ru.kochkaev.zixamc.rest.std.user

import io.ktor.http.HttpStatusCode
import ru.kochkaev.zixamc.rest.method.MethodResult
import ru.kochkaev.zixamc.rest.method.MethodResults
import ru.kochkaev.zixamc.rest.method.RestMapping
import ru.kochkaev.zixamc.rest.method.RestMethodType
import ru.kochkaev.zixamc.rest.method.methodResult
import ru.kochkaev.zixamc.rest.method.result
import ru.kochkaev.zixamc.rest.openAPI.RestDescription
import ru.kochkaev.zixamc.rest.std.Permissions

@RestDescription("Get detailed information about a specific user by their ID")
object GetUser: RestMethodType<GetUser.Request, UserData>(
    path = "std/getUser",
    requiredPermissions = listOf(Permissions.READ_USER),
    mapping = RestMapping.POST,
    params = mapOf(),
    bodyModel = Request::class.java,
    result = MethodResults.create(HttpStatusCode.OK,
        HttpStatusCode.BadRequest to "Request body is empty".methodResult(),
        HttpStatusCode.NotFound to "User not found".methodResult(),
    ),
    method = { sql, permissions, params, body ->
        if (body == null) {
            HttpStatusCode.BadRequest.result("Request body is required")
        } else {
            val user = UserData.get(body.userId)
            if (user == null) {
                HttpStatusCode.NotFound.result("User not found: ${body.userId}")
            } else HttpStatusCode.OK.result(user)
        }
    }
) {
    data class Request(
        val userId: Long,
    )
}