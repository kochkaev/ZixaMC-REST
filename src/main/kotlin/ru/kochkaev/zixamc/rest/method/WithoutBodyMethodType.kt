package ru.kochkaev.zixamc.rest.method

import io.ktor.http.HttpStatusCode
import ru.kochkaev.zixamc.rest.SQLClient

open class WithoutBodyMethodType<R>(
    path: String,
    requiredPermissions: List<String>,
    mapping: RestMapping,
    params: Map<String, Pair<Class<*>, Boolean>> = mapOf(),
    result: MethodResults<R>,
    method: suspend (SQLClient, List<String>, Map<String, Any?>) -> Comparable<HttpStatusCode>
): RestMethodType<Unit, R>(
    path = path,
    requiredPermissions = requiredPermissions,
    mapping = mapping,
    params = params,
    bodyModel = null,
    result = result,
    method = { sql, permissions, params, body -> method(sql, permissions, params) }
)