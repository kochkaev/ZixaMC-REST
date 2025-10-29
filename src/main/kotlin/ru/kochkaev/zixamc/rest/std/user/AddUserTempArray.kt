package ru.kochkaev.zixamc.rest.std.user

import io.ktor.http.HttpStatusCode
import ru.kochkaev.zixamc.api.sql.SQLUser
import ru.kochkaev.zixamc.rest.RestMapping
import ru.kochkaev.zixamc.rest.RestMethodType
import ru.kochkaev.zixamc.rest.std.Permissions
import ru.kochkaev.zixamc.rest.std.user.SetUserNickname.checkValidNickname

object AddUserTempArray: RestMethodType<AddUserTempArray.Request>(
    path = "std/addUserTempArray",
    requiredPermissions = listOf(Permissions.WRITE_USER_TEMP_ARRAY),
    mapping = RestMapping.PUT,
    params = mapOf(),
    bodyModel = Request::class.java,
    method = { sql, permissions, params, body ->
        if (body == null) {
            HttpStatusCode.BadRequest to "Request body is required"
        } else {
            val user = SQLUser.get(body.userId)
            if (user == null) {
                HttpStatusCode.NotFound to "User not found: ${body.userId}"
            } else {
                user.tempArray.add(body.value)
                HttpStatusCode.OK to UserData.get(user.id)
            }
        }
    }
) {
    data class Request(
        val userId: Long,
        val value: String,
    )
}