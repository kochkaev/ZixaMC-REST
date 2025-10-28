package ru.kochkaev.zixamc.rest

import io.ktor.http.HttpStatusCode

open class RestMethodType<T>(
    val path: String,
    val requiredPermissions: List<String>,
    val mapping: RestMapping,
    val parameters: Map<String, Class<*>> = mapOf(),
    val bodyModel: Class<T>?,
    val method: suspend (SQLClient, List<String>, Map<String, Any>, T?) -> Pair<HttpStatusCode, Any?>
) {
    @Suppress("UNCHECKED_CAST")
    suspend fun invoke(
        sql: SQLClient,
        permissions: List<String>,
        params: Map<String, Any>,
        body: Any?
    ): Pair<HttpStatusCode, Any?> {
        return method(sql, permissions, params, body as T?)
    }
}