package ru.kochkaev.zixamc.rest.method

import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import net.fabricmc.loader.api.FabricLoader
import ru.kochkaev.zixamc.rest.SQLClient
import java.io.File
import java.nio.file.Path
import kotlin.collections.get

open class SendFileMethodType<T>(
    path: String,
    requiredPermissions: List<String>,
    mapping: RestMapping,
    params: Map<String, Pair<Class<*>, Boolean>> = mapOf(),
    bodyModel: Class<T>?,
    result: MethodResults<SendFile> = MethodResults.create(),
    method: suspend (SQLClient, List<String>, Map<String, Any?>, T?) -> Comparable<HttpStatusCode>
): RestMethodType<T, SendFile>(
    path = path,
    requiredPermissions = requiredPermissions,
    mapping = mapping,
    params = params,
    bodyModel = bodyModel,
    result = result,
    method = method
) {
    @Suppress("UNCHECKED_CAST")
    override suspend fun invoke(
        sql: SQLClient,
        permissions: List<String>,
        params: Map<String, Any?>,
        body: Any?
    ): ResultedHttpStatusCode<*> {
        val returned = method(sql, permissions, params, body as T?)
        val (code, data) = when (returned) {
            is HttpStatusCode ->
                returned to result.results[returned]?.default
            is ResultedHttpStatusCode<*> ->
                returned.toCode() to returned.result
            else -> {
                val code = HttpStatusCode.fromValue(returned.compareTo(HttpStatusCode.OK) + 200)
                code to result.results[code]?.default
            }
        }
        val tried0 = data as? SendFile
        val tried1 = data as? File
        if (tried0 == null && tried1 == null && code.isSuccess()) {
            throw IllegalStateException("ZixaMC REST: Method $path returned invalid type: ${data?.javaClass}, expected: ${result.typeOfSuccess} or ${File::class.java}")
        }
        val finalFile = tried0 ?: SendFile(tried1!!)
        return code.result(finalFile)
    }
}