package ru.kochkaev.zixamc.rest.std.group

import io.ktor.http.HttpStatusCode
import ru.kochkaev.zixamc.api.sql.SQLGroup
import ru.kochkaev.zixamc.api.sql.process.GroupWaitingNameProcessData
import ru.kochkaev.zixamc.api.sql.process.ProcessTypes
import ru.kochkaev.zixamc.rest.RestMapping
import ru.kochkaev.zixamc.rest.RestMethodType
import ru.kochkaev.zixamc.rest.std.Permissions
import ru.kochkaev.zixamc.rest.std.group.SetGroupName.checkValidName

object SetGroupName: RestMethodType<SetGroupName.Request>(
    path = "std/setGroupName",
    requiredPermissions = listOf(Permissions.WRITE_GROUP_NAMES),
    mapping = RestMapping.PUT,
    params = mapOf(),
    bodyModel = Request::class.java,
    method = { sql, permissions, params, body ->
        if (body == null) {
            HttpStatusCode.BadRequest to "Request body is required"
        } else {
            val group = SQLGroup.get(body.chatId)
            if (group == null) {
                HttpStatusCode.NotFound to "Group not found: ${body.chatId}"
            } else if (!checkValidName(body.name)) {
                HttpStatusCode.BadRequest to "Invalid group name: ${body.name}"
            } else if (!group.canTakeName(body.name)) {
                HttpStatusCode.Conflict to "This name is already taken: ${body.name}"
            } else {
                group.name = body.name
                group.aliases.remove(body.name)
                HttpStatusCode.OK to GroupData.get(group.id)
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