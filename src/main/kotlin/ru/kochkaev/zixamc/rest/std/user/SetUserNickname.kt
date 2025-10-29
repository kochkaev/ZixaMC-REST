package ru.kochkaev.zixamc.rest.std.user

import io.ktor.http.HttpStatusCode
import ru.kochkaev.zixamc.api.sql.SQLUser
import ru.kochkaev.zixamc.rest.RestMapping
import ru.kochkaev.zixamc.rest.RestMethodType
import ru.kochkaev.zixamc.rest.std.Permissions
import ru.kochkaev.zixamc.rest.std.user.SetUserNickname.checkValidNickname

object SetUserNickname: RestMethodType<SetUserNickname.Request>(
    path = "std/setUserNickname",
    requiredPermissions = listOf(Permissions.WRITE_USER_NICKNAMES),
    mapping = RestMapping.PUT,
    params = mapOf(),
    bodyModel = Request::class.java,
    method = { sql, permissions, params, body ->
        if (body == null) {
            HttpStatusCode.BadRequest to "Request body is required"
        } else if (!checkValidNickname(body.nickname)) {
            HttpStatusCode.BadRequest to "Invalid nickname: ${body.nickname}"
        } else {
            val user = SQLUser.get(body.userId)
            if (user == null) {
                HttpStatusCode.NotFound to "User not found: ${body.userId}"
            } else if (!user.canTakeNickname(body.nickname)) {
                HttpStatusCode.Conflict to "Nickname already taken: ${body.nickname}"
            } else {
                user.nickname = body.nickname
                user.nicknames.add(body.nickname)
                HttpStatusCode.OK to UserData.get(user.id)
            }
        }
    }
) {
    data class Request(
        val userId: Long,
        val nickname: String,
    )
    fun checkValidNickname(nickname: String): Boolean {
        val nicknameRegex = "^[a-zA-Z0-9_]{3,16}$".toRegex()
        return nickname.matches(nicknameRegex)
    }
}