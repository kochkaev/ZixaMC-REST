package ru.kochkaev.zixamc.rest.std.user

import io.ktor.http.HttpStatusCode
import ru.kochkaev.zixamc.api.sql.SQLUser
import ru.kochkaev.zixamc.rest.method.MethodResult
import ru.kochkaev.zixamc.rest.method.MethodResults
import ru.kochkaev.zixamc.rest.method.RestMapping
import ru.kochkaev.zixamc.rest.method.RestMethodType
import ru.kochkaev.zixamc.rest.method.methodResult
import ru.kochkaev.zixamc.rest.method.result
import ru.kochkaev.zixamc.rest.openAPI.RestDescription
import ru.kochkaev.zixamc.rest.std.Permissions

@RestDescription("Update user account information with new data")
object UpdateUser: RestMethodType<UserData, UserData>(
    path = "std/updateUser",
    requiredPermissions = listOf(Permissions.WRITE_USER_OVERRIDE),
    mapping = RestMapping.PUT,
    params = mapOf(),
    bodyModel = UserData::class.java,
    result = MethodResults.create(HttpStatusCode.OK,
        HttpStatusCode.BadRequest to "Request body is empty or provided nickname is invalid".methodResult(),
        HttpStatusCode.NotFound to "User not found".methodResult(),
        HttpStatusCode.Conflict to "Provided nickname is already is taken".methodResult(),
    ),
    method = { sql, permissions, params, body ->
        if (body == null) {
            HttpStatusCode.BadRequest.result("Request body is required")
        } else {
            val sql = SQLUser.get(body.userId)
            if (sql == null) HttpStatusCode.NotFound.result("User not found: ${body.userId}")
            else if (body.nickname != null && !sql.canTakeNickname(body.nickname)) {
                HttpStatusCode.Conflict.result("Nickname already taken: ${body.nickname}")
            }
            else if (body.nickname != null && !SetUserNickname.checkValidNickname(body.nickname)) {
                HttpStatusCode.BadRequest.result("Invalid nickname: ${body.nickname}")
            }
            else body.nicknames?.firstNotNullOfOrNull {
                if (SQLUser.exists(it)) HttpStatusCode.Conflict.result("Nickname already taken: $it")
                else if (!SetUserNickname.checkValidNickname(it)) HttpStatusCode.BadRequest.result("Invalid nickname: $it")
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
                    HttpStatusCode.Forbidden
                } else {
                    HttpStatusCode.OK.result(UserData.get(body.userId))
                }
            }
        }
    }
)