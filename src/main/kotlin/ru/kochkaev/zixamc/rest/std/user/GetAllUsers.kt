package ru.kochkaev.zixamc.rest.std.user

import io.ktor.http.HttpStatusCode
import ru.kochkaev.zixamc.api.sql.SQLUser
import ru.kochkaev.zixamc.rest.method.MethodResult
import ru.kochkaev.zixamc.rest.method.RestMapping
import ru.kochkaev.zixamc.rest.method.RestMethodType
import ru.kochkaev.zixamc.rest.std.Permissions

object GetAllUsers: RestMethodType<Unit, List<Long>>(
    path = "std/getAllUsers",
    requiredPermissions = listOf(Permissions.READ_ALL_USERS),
    mapping = RestMapping.GET,
    params = mapOf(),
    bodyModel = null,
    result = MethodResult.create(),
    method = { sql, permissions, params, body ->
        HttpStatusCode.OK to SQLUser.users.map { it.id }
    }
)