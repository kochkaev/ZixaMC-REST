package ru.kochkaev.zixamc.rest.method

import io.ktor.http.HttpStatusCode
import ru.kochkaev.zixamc.rest.SQLClient

open class WithoutResultMethodType<T>(
    path: String,
    requiredPermissions: List<String>,
    mapping: RestMapping,
    params: Map<String, Pair<Class<*>, Boolean>> = mapOf(),
    bodyModel: Class<T>?,
    method: suspend (SQLClient, List<String>, Map<String, Any?>, T?) -> Comparable<HttpStatusCode>
): RestMethodType<T, Unit>(
    path = path,
    requiredPermissions = requiredPermissions,
    mapping = mapping,
    params = params,
    bodyModel = bodyModel,
    result = MethodResults.create(),
    method = method
)