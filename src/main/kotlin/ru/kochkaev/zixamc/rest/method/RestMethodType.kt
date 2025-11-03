package ru.kochkaev.zixamc.rest.method

import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import kotlinx.serialization.EncodeDefault
import ru.kochkaev.zixamc.rest.SQLClient

open class RestMethodType<T, R>(
    val path: String,
    val requiredPermissions: List<String>,
    val mapping: RestMapping,
    /** Map of parameter name to Pair of (Type, isRequired) */
    val params: Map<String, Pair<Class<*>, Boolean>> = mapOf(),
    val bodyModel: Class<T>?,
    val bodyDefault: T? = null,
    val result: MethodResults<R>,
    protected val method: suspend (SQLClient, List<String>, Map<String, Any?>, T?) -> Comparable<HttpStatusCode>
) {
    @Suppress("UNCHECKED_CAST")
    open suspend fun invoke(
        sql: SQLClient,
        permissions: List<String>,
        params: Map<String, Any?>,
        body: Any?
    ): ResultedHttpStatusCode<*> {
        val code = method(sql, permissions, params, body as T?)
        return when (code) {
            is HttpStatusCode ->
                code.result(result.results[code]?.default)
            is ResultedHttpStatusCode<*> -> code
            else ->
                HttpStatusCode
                    .fromValue(code.compareTo(HttpStatusCode.OK) + 200)
                    .result(result.results[code]?.default)
        }
    }
}