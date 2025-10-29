package ru.kochkaev.zixamc.rest.std

import io.ktor.http.HttpStatusCode
import ru.kochkaev.zixamc.rest.RestMapping
import ru.kochkaev.zixamc.rest.RestMethodType

object GetMe: RestMethodType<Any>(
    path = "/std/getMe",
    requiredPermissions = listOf(),
    mapping = RestMapping.GET,
    params = mapOf(),
    bodyModel = null,
    method = { sql, permissions, params, _ ->
        HttpStatusCode.OK to mapOf(
            "userId" to sql.userId,
            "mark" to sql.mark,
            "permissions" to permissions
        )
    }
)