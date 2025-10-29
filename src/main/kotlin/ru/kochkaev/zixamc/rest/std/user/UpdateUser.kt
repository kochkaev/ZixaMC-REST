package ru.kochkaev.zixamc.rest.std.user

import io.ktor.http.HttpStatusCode
import ru.kochkaev.zixamc.api.sql.SQLUser
import ru.kochkaev.zixamc.rest.method.MethodResult
import ru.kochkaev.zixamc.rest.method.RestMapping
import ru.kochkaev.zixamc.rest.method.RestMethodType
import ru.kochkaev.zixamc.rest.std.Permissions

object UpdateUser: RestMethodType<UserData, UserData>(
    path = "std/updateUser",
    requiredPermissions = listOf(Permissions.WRITE_USER_OVERRIDE),
    mapping = RestMapping.PUT,
    params = mapOf(),
    bodyModel = UserData::class.java,
    result = MethodResult.create(),
    method = { sql, permissions, params, body ->
        if (body == null) {
            HttpStatusCode.BadRequest to "Request body is required"
        } else {
            val sql = SQLUser.get(body.userId)
            if (sql == null) HttpStatusCode.NotFound to "User not found: ${body.userId}"
            else if (body.nickname != null && !sql.canTakeNickname(body.nickname)) {
                HttpStatusCode.Conflict to "Nickname already taken: ${body.nickname}"
            }
            else if (body.nickname != null && !SetUserNickname.checkValidNickname(body.nickname)) {
                HttpStatusCode.BadRequest to "Invalid nickname: ${body.nickname}"
            }
            else body.nicknames?.firstNotNullOfOrNull {
                if (SQLUser.exists(it)) HttpStatusCode.Conflict to "Nickname already taken: $it"
                else if (!SetUserNickname.checkValidNickname(it)) HttpStatusCode.BadRequest to "Invalid nickname: $it"
                else null
            } ?: run {
                var forbidden = false
                when {
                    permissions.contains(Permissions.WRITE_USER_NICKNAMES) && body.nickname != null ->
                        sql.nickname = body.nickname
                    permissions.contains(Permissions.WRITE_USER_NICKNAMES) && body.nicknames != null ->
                        sql.nicknames.set(body.nicknames)
                    permissions.contains(Permissions.WRITE_USER_ACCOUNT_TYPE) && body.accountType != null ->
                        sql.accountType = body.accountType
                    permissions.contains(Permissions.WRITE_USER_TEMP_ARRAY) && body.tempArray != null ->
                        sql.tempArray.set(body.tempArray)
                    permissions.contains(Permissions.WRITE_USER_AGREED_WITH_RULES) && body.agreedWithRules != null ->
                        sql.agreedWithRules = body.agreedWithRules
                    permissions.contains(Permissions.WRITE_USER_IS_RESTRICTED) && body.isRestricted != null ->
                        sql.isRestricted = body.isRestricted
                    permissions.contains(Permissions.WRITE_USER_DATA) && body.data != null ->
                        sql.data.setAll(body.data)
                    else -> forbidden = true
                }
                if (forbidden) {
                    HttpStatusCode.Forbidden to null
                } else {
                    HttpStatusCode.OK to UserData.get(body.userId)
                }
            }
        }
    }
)