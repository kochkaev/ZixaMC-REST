package ru.kochkaev.zixamc.rest.std.group

import io.ktor.http.HttpStatusCode
import ru.kochkaev.zixamc.api.sql.SQLGroup
import ru.kochkaev.zixamc.api.sql.process.GroupWaitingNameProcessData
import ru.kochkaev.zixamc.api.sql.process.ProcessTypes
import ru.kochkaev.zixamc.rest.RestMapping
import ru.kochkaev.zixamc.rest.RestMethodType
import ru.kochkaev.zixamc.rest.std.Permissions
import ru.kochkaev.zixamc.rest.std.group.SetGroupName.checkValidName

object SetGroupRestricted: RestMethodType<SetGroupRestricted.Request>(
    path = "std/setGroupAgreedWithRules",
    requiredPermissions = listOf(Permissions.WRITE_GROUP_IS_RESTRICTED),
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
            } else {
                group.isRestricted = body.isRestricted
                // TODO: auto leave bot from group if it becomes restricted
                HttpStatusCode.OK to GroupData.get(group.id)
            }
        }
    }
) {
    data class Request(
        val chatId: Long,
        val isRestricted: Boolean,
    )
}