package ru.kochkaev.zixamc.rest

import io.ktor.http.HttpStatusCode

open class RestMethodType<T>(
    val path: String,
    val requiredPermissions: List<String>,
    val mapping: RestMapping,
    /** Map of parameter name to Pair of (Type, isRequired) */
    val params: Map<String, Pair<Class<*>, Boolean>> = mapOf(),
    val bodyModel: Class<T>?,
    private val method: suspend (SQLClient, List<String>, Map<String, Any?>, T?) -> Pair<HttpStatusCode, Any?>
) {
    @Suppress("UNCHECKED_CAST")
    suspend fun invoke(
        sql: SQLClient,
        permissions: List<String>,
        params: Map<String, Any?>,
        body: Any?
    ): Pair<HttpStatusCode, Any?> {
        return method(sql, permissions, params, body as T?)
    }
}