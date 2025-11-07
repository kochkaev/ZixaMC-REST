package ru.kochkaev.zixamc.rest.std.user

import io.ktor.http.HttpStatusCode
import ru.kochkaev.zixamc.api.sql.SQLUser
import ru.kochkaev.zixamc.api.sql.data.AccountType
import ru.kochkaev.zixamc.rest.method.MethodResult
import ru.kochkaev.zixamc.rest.method.MethodResults
import ru.kochkaev.zixamc.rest.method.RestMapping
import ru.kochkaev.zixamc.rest.method.RestMethodType
import ru.kochkaev.zixamc.rest.method.methodResult
import ru.kochkaev.zixamc.rest.method.result
import ru.kochkaev.zixamc.rest.openAPI.RestDescription
import ru.kochkaev.zixamc.rest.std.Permissions

@RestDescription("Change user's account type/role on the server")
object SetUserAccountType: RestMethodType<SetUserAccountType.Request, UserData>(
    path = "std/setUserAccountType",
    requiredPermissions = listOf(Permissions.WRITE_USER_ACCOUNT_TYPE),
    mapping = RestMapping.PUT,
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
            val user = SQLUser.get(body.userId)
            if (user == null) {
                HttpStatusCode.NotFound.result("User not found: ${body.userId}")
            } else {
                user.accountType = body.accountType
                HttpStatusCode.OK.result(UserData.get(user.id))
            }
        }
    }
) {
    data class Request(
        val userId: Long,
        val accountType: AccountType,
    )
}