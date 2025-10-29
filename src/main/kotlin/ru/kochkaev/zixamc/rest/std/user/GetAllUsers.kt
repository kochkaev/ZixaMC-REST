package ru.kochkaev.zixamc.rest.std.user

import io.ktor.http.HttpStatusCode
import ru.kochkaev.zixamc.api.sql.SQLUser
import ru.kochkaev.zixamc.rest.RestMapping
import ru.kochkaev.zixamc.rest.RestMethodType
import ru.kochkaev.zixamc.rest.std.Permissions

object GetAllUsers: RestMethodType<Any>(
    path = "std/getAllUsers",
    requiredPermissions = listOf(Permissions.READ_ALL_USERS),
    mapping = RestMapping.GET,
    params = mapOf(),
    bodyModel = null,
    method = { sql, permissions, params, body ->
        HttpStatusCode.OK to SQLUser.users.map { it.id }
    }
)