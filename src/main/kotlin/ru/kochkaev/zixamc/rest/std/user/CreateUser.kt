package ru.kochkaev.zixamc.rest.std.user

import io.ktor.http.HttpStatusCode
import ru.kochkaev.zixamc.api.sql.SQLUser
import ru.kochkaev.zixamc.api.sql.chatdata.ChatDataType
import ru.kochkaev.zixamc.api.sql.chatdata.ChatDataTypes
import ru.kochkaev.zixamc.api.sql.data.AccountType
import ru.kochkaev.zixamc.api.sql.data.MinecraftAccountData
import ru.kochkaev.zixamc.api.sql.data.MinecraftAccountType
import ru.kochkaev.zixamc.rest.method.MethodResult
import ru.kochkaev.zixamc.rest.method.MethodResults
import ru.kochkaev.zixamc.rest.method.RestMapping
import ru.kochkaev.zixamc.rest.method.RestMethodType
import ru.kochkaev.zixamc.rest.method.methodResult
import ru.kochkaev.zixamc.rest.method.result
import ru.kochkaev.zixamc.rest.std.Permissions

object CreateUser: RestMethodType<UserData, UserData>(
    path = "std/createUser",
    requiredPermissions = listOf(Permissions.CREATE_USER),
    mapping = RestMapping.POST,
    params = mapOf(),
    bodyModel = UserData::class.java,
    bodyDefault = UserData(
        userId = 0,
        nickname = "kleverdi",
        nicknames = listOf("kleverdi"),
        accountType = AccountType.ADMIN,
        agreedWithRules = true,
        isRestricted = false,
        tempArray = listOf(),
        data = hashMapOf(ChatDataTypes.MINECRAFT_ACCOUNTS to arrayListOf(MinecraftAccountData("kleverdi", MinecraftAccountType.PLAYER))),
    ),
    result = MethodResults.create(HttpStatusCode.Created,
        HttpStatusCode.BadRequest to "Request body is empty, or provided nickname is invalid".methodResult(),
        HttpStatusCode.Conflict to "User with provided userID is already exists or provided nickname is already taken".methodResult(),
    ),
    method = { sql, permissions, params, body ->
        if (body == null) {
            HttpStatusCode.BadRequest.result("Request body is required")
        } else {
            if (SQLUser.exists(body.userId)) {
                HttpStatusCode.Conflict.result("User already exists: ${body.userId}")
            }
            else if (body.nickname != null && SQLUser.exists(body.nickname)) {
                HttpStatusCode.Conflict.result("Nickname already taken: ${body.nickname}")
            }
            else if (body.nickname != null && !SetUserNickname.checkValidNickname(body.nickname)) {
                HttpStatusCode.BadRequest.result("Invalid nickname: ${body.nickname}")
            }
            else {
                body.nicknames?.firstNotNullOfOrNull {
                    if (SQLUser.exists(it)) HttpStatusCode.Conflict.result("Nickname already taken: $it")
                    else if (!SetUserNickname.checkValidNickname(it)) HttpStatusCode.BadRequest.result("Invalid nickname: $it")
                    else null
                } ?: run {
                    SQLUser.create(
                        userId = body.userId,
                        nickname = body.nickname,
                        nicknames = body.nicknames,
                        accountType = body.accountType?.id,
                        data = body.data ?: mapOf<ChatDataType<*>, Any>()
                    )
                    HttpStatusCode.Created.result(UserData.get(body.userId))
                }
            }
        }
    }
)