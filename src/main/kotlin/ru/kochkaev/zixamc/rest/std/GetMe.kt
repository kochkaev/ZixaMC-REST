package ru.kochkaev.zixamc.rest.std

import io.ktor.http.HttpStatusCode
import ru.kochkaev.zixamc.rest.method.MethodResult
import ru.kochkaev.zixamc.rest.method.RestMapping
import ru.kochkaev.zixamc.rest.method.RestMethodType
import ru.kochkaev.zixamc.rest.openAPI.RestDescription

@RestDescription("Returns owner, mark and permissions of that token")
object GetMe: RestMethodType<Any, GetMe.MeInfo>(
    path = "std/getMe",
    requiredPermissions = listOf(),
    mapping = RestMapping.GET,
    params = mapOf(),
    bodyModel = null,
    result = MethodResult.create(),
    method = { sql, permissions, params, _ ->
        HttpStatusCode.OK to MeInfo(
            userId = sql.userId,
            mark = sql.mark,
            permissions = permissions,
        )
    }
) {
    data class MeInfo(
        val userId: Long,
        val mark: String?,
        val permissions: List<String>,
    )
}