package ru.kochkaev.zixamc.rest.std.group

import io.ktor.http.HttpStatusCode
import ru.kochkaev.zixamc.api.sql.SQLGroup
import ru.kochkaev.zixamc.rest.RestMapping
import ru.kochkaev.zixamc.rest.RestMethodType
import ru.kochkaev.zixamc.rest.std.Permissions

object GetAllGroups: RestMethodType<Any>(
    path = "std/getAllGroups",
    requiredPermissions = listOf(Permissions.READ_ALL_USERS),
    mapping = RestMapping.GET,
    params = mapOf(),
    bodyModel = null,
    method = { sql, permissions, params, body ->
        HttpStatusCode.OK to SQLGroup.groups.map { it.id }
    }
)