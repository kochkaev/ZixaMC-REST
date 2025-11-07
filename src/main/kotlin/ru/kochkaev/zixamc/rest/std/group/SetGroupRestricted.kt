package ru.kochkaev.zixamc.rest.std.group

import io.ktor.http.HttpStatusCode
import ru.kochkaev.zixamc.api.sql.SQLGroup
import ru.kochkaev.zixamc.rest.method.MethodResult
import ru.kochkaev.zixamc.rest.method.MethodResults
import ru.kochkaev.zixamc.rest.method.RestMapping
import ru.kochkaev.zixamc.rest.method.RestMethodType
import ru.kochkaev.zixamc.rest.method.methodResult
import ru.kochkaev.zixamc.rest.method.result
import ru.kochkaev.zixamc.rest.openAPI.RestDescription
import ru.kochkaev.zixamc.rest.std.Permissions

@RestDescription("Set whether a group is restricted from bots usage")
object SetGroupRestricted: RestMethodType<SetGroupRestricted.Request, GroupData>(
    path = "std/setGroupAgreedWithRules",
    requiredPermissions = listOf(Permissions.WRITE_GROUP_IS_RESTRICTED),
    mapping = RestMapping.PUT,
    params = mapOf(),
    bodyModel = Request::class.java,
    result = MethodResults.create(HttpStatusCode.OK,
        HttpStatusCode.BadRequest to "Request body is empty".methodResult(),
        HttpStatusCode.NotFound to "Group not found".methodResult(),
    ),
    method = { sql, permissions, params, body ->
        if (body == null) {
            HttpStatusCode.BadRequest.result("Request body is required")
        } else {
            val group = SQLGroup.get(body.chatId)
            if (group == null) {
                HttpStatusCode.NotFound.result("Group not found: ${body.chatId}")
            } else {
                group.isRestricted = body.isRestricted
                // TODO: auto leave bot from group if it becomes restricted
                HttpStatusCode.OK.result(GroupData.get(group.id))
            }
        }
    }
) {
    data class Request(
        val chatId: Long,
        val isRestricted: Boolean,
    )
}