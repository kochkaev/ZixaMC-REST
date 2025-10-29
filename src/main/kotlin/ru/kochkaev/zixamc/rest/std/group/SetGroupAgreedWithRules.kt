package ru.kochkaev.zixamc.rest.std.group

import io.ktor.http.HttpStatusCode
import ru.kochkaev.zixamc.api.sql.SQLGroup
import ru.kochkaev.zixamc.rest.method.MethodResult
import ru.kochkaev.zixamc.rest.method.RestMapping
import ru.kochkaev.zixamc.rest.method.RestMethodType
import ru.kochkaev.zixamc.rest.std.Permissions

object SetGroupAgreedWithRules: RestMethodType<SetGroupAgreedWithRules.Request, GroupData>(
    path = "std/setGroupAgreedWithRules",
    requiredPermissions = listOf(Permissions.WRITE_GROUP_AGREED_WITH_RULES),
    mapping = RestMapping.PUT,
    params = mapOf(),
    bodyModel = Request::class.java,
    result = MethodResult.create(),
    method = { sql, permissions, params, body ->
        if (body == null) {
            HttpStatusCode.BadRequest to "Request body is required"
        } else {
            val group = SQLGroup.get(body.chatId)
            if (group == null) {
                HttpStatusCode.NotFound to "Group not found: ${body.chatId}"
            } else {
                group.agreedWithRules = body.agreedWithRules
                HttpStatusCode.OK to GroupData.get(group.id)
            }
        }
    }
) {
    data class Request(
        val chatId: Long,
        val agreedWithRules: Boolean,
    )
}