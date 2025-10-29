package ru.kochkaev.zixamc.rest.std

import io.ktor.http.HttpStatusCode
import ru.kochkaev.zixamc.api.sql.SQLGroup
import ru.kochkaev.zixamc.api.sql.SQLUser
import ru.kochkaev.zixamc.rest.RestMapping
import ru.kochkaev.zixamc.rest.RestMethodType

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