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
import ru.kochkaev.zixamc.rest.std.user.SetUserNickname.checkValidNickname

object AddUserNickname: RestMethodType<AddUserNickname.Request, UserData>(
    path = "std/addUserNickname",
    requiredPermissions = listOf(Permissions.WRITE_USER_NICKNAMES),
    mapping = RestMapping.PUT,
    params = mapOf(),
    bodyModel = Request::class.java,
    result = MethodResults.create(HttpStatusCode.OK,
        HttpStatusCode.BadRequest to "Request body is empty or provided nickname is invalid".methodResult(),
        HttpStatusCode.NotFound to "User not found".methodResult(),
        HttpStatusCode.Conflict to "Provided nickname is already taken".methodResult(),
    ),
    method = { sql, permissions, params, body ->
        if (body == null) {
            HttpStatusCode.BadRequest.result("Request body is required")
        } else if (!checkValidNickname(body.nickname)) {
            HttpStatusCode.BadRequest.result("Invalid nickname: ${body.nickname}")
        } else {
            val user = SQLUser.get(body.userId)
            if (user == null) {
                HttpStatusCode.NotFound.result("User not found: ${body.userId}")
            } else if (!user.canTakeNickname(body.nickname)) {
                HttpStatusCode.Conflict.result("Nickname already taken: ${body.nickname}")
            } else {
                user.nicknames.add(body.nickname)
                HttpStatusCode.OK.result(UserData.get(user.id))
            }
        }
    }
) {
    data class Request(
        val userId: Long,
        val nickname: String,
    )
}