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

@RestDescription("Add an alternative name (alias) for a group")
object AddGroupAlias: RestMethodType<AddGroupAlias.Request, GroupData>(
    path = "std/addGroupAlias",
    requiredPermissions = listOf(Permissions.WRITE_GROUP_NAMES),
    mapping = RestMapping.PUT,
    params = mapOf(),
    bodyModel = Request::class.java,
    result = MethodResults.create(HttpStatusCode.OK,
        HttpStatusCode.BadRequest to "Request body is empty or invalid group alias".methodResult(),
        HttpStatusCode.NotFound to "Group not found".methodResult(),
        HttpStatusCode.Conflict to "Provided alias is already taken or group already has this alias as name".methodResult(),
    ),
    method = { sql, permissions, params, body ->
        if (body == null) {
            HttpStatusCode.BadRequest.result("Request body is required")
        } else {
            val group = SQLGroup.get(body.chatId)
            if (group == null) {
                HttpStatusCode.NotFound.result("Group not found: ${body.chatId}")
            } else if (!SetGroupName.checkValidName(body.alias)) {
                HttpStatusCode.BadRequest.result("Invalid group alias: ${body.alias}")
            } else if (!group.canTakeName(body.alias)) {
                HttpStatusCode.Conflict.result("This alias is already taken: ${body.alias}")
            } else if (group.name == body.alias) {
                HttpStatusCode.Conflict.result("Group already has this alias as name: ${body.alias}")
            } else {
                group.aliases.add(body.alias)
                HttpStatusCode.OK.result(GroupData.get(group.id))
            }
        }
    }
) {
    data class Request(
        val chatId: Long,
        val alias: String,
    )
}