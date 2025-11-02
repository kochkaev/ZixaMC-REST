package ru.kochkaev.zixamc.rest.std.group

import io.ktor.http.HttpStatusCode
import ru.kochkaev.zixamc.api.sql.SQLGroup
import ru.kochkaev.zixamc.rest.method.MethodResult
import ru.kochkaev.zixamc.rest.method.MethodResults
import ru.kochkaev.zixamc.rest.method.RestMapping
import ru.kochkaev.zixamc.rest.method.RestMethodType
import ru.kochkaev.zixamc.rest.method.methodResult
import ru.kochkaev.zixamc.rest.method.result
import ru.kochkaev.zixamc.rest.std.Permissions
import ru.kochkaev.zixamc.rest.std.group.SetGroupName.checkValidName

object SetGroupName: RestMethodType<SetGroupName.Request, GroupData>(
    path = "std/setGroupName",
    requiredPermissions = listOf(Permissions.WRITE_GROUP_NAMES),
    mapping = RestMapping.PUT,
    params = mapOf(),
    bodyModel = Request::class.java,
    result = MethodResults.create(HttpStatusCode.OK,
        HttpStatusCode.BadRequest to "Request body is empty or group name is invalid".methodResult(),
        HttpStatusCode.NotFound to "Group not found".methodResult(),
        HttpStatusCode.Conflict to "Provided name is already taken".methodResult(),
    ),
    method = { sql, permissions, params, body ->
        if (body == null) {
            HttpStatusCode.BadRequest.result("Request body is required")
        } else {
            val group = SQLGroup.get(body.chatId)
            if (group == null) {
                HttpStatusCode.NotFound.result("Group not found: ${body.chatId}")
            } else if (!checkValidName(body.name)) {
                HttpStatusCode.BadRequest.result("Invalid group name: ${body.name}")
            } else if (!group.canTakeName(body.name)) {
                HttpStatusCode.Conflict.result("This name is already taken: ${body.name}")
            } else {
                group.name = body.name
                group.aliases.remove(body.name)
                HttpStatusCode.OK.result(GroupData.get(group.id))
            }
        }
    }
) {
    data class Request(
        val chatId: Long,
        val name: String,
    )
    fun checkValidName(name: String): Boolean {
        val regex = "^[a-zA-Z0-9 _-]{3,16}$".toRegex()
        return regex.matches(name)
    }
}