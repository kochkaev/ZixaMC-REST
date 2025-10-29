package ru.kochkaev.zixamc.rest.method

import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import ru.kochkaev.zixamc.rest.SQLClient

open class RestMethodType<T, R>(
    val path: String,
    val requiredPermissions: List<String>,
    val mapping: RestMapping,
    /** Map of parameter name to Pair of (Type, isRequired) */
    val params: Map<String, Pair<Class<*>, Boolean>> = mapOf(),
    val bodyModel: Class<T>?,
    val result: MethodResult<R>,
    protected val method: suspend (SQLClient, List<String>, Map<String, Any?>, T?) -> Pair<HttpStatusCode, Any?>
) {
    @Suppress("UNCHECKED_CAST")
    open suspend fun invoke(
        sql: SQLClient,
        permissions: List<String>,
        params: Map<String, Any?>,
        body: Any?
    ): MethodResult.Result<R> {
        val returned = method(sql, permissions, params, body as T?)
        val tried = returned.second as? R
        if (returned.second != null && tried == null && returned.first.isSuccess()) {
            throw IllegalStateException("ZixaMC REST: Method $path returned invalid type: ${returned.second?.javaClass}, expected: ${result.typeClass}")
        }
        return result.write(returned.first, returned.second)
    }

//    protected fun write(code: HttpStatusCode, returned: R?): MethodResult.Result<R> {
//        return result.write(code, returned)
//    }
//    protected fun write(code: HttpStatusCode, returned: String): MethodResult.Result<R> {
//        return result.write(code, returned)
//    }
//    protected open fun method(
//        sql: SQLClient,
//        permissions: List<String>,
//        params: Map<String, Any?>,
//        body: Any?
//    ): MethodResult.Result<R> {
//        return write(HttpStatusCode.OK, null)
//    }
}