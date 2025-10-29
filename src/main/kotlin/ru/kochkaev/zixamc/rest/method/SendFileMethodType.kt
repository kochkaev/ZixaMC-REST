package ru.kochkaev.zixamc.rest.method

import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import net.fabricmc.loader.api.FabricLoader
import ru.kochkaev.zixamc.rest.SQLClient
import java.io.File
import java.nio.file.Path

open class SendFileMethodType<T>(
    path: String,
    requiredPermissions: List<String>,
    mapping: RestMapping,
    params: Map<String, Pair<Class<*>, Boolean>> = mapOf(),
    bodyModel: Class<T>?,
    method: suspend (SQLClient, List<String>, Map<String, Any?>, T?) -> Pair<HttpStatusCode, Any?>
): RestMethodType<T, SendFile>(
    path = path,
    requiredPermissions = requiredPermissions,
    mapping = mapping,
    params = params,
    bodyModel = bodyModel,
    result = MethodResult.create(),
    method = method
) {
    @Suppress("UNCHECKED_CAST")
    override suspend fun invoke(
        sql: SQLClient,
        permissions: List<String>,
        params: Map<String, Any?>,
        body: Any?
    ): MethodResult.Result<SendFile> {
        val returned = method(sql, permissions, params, body as T?)
        val tried0 = returned.second as? SendFile
        val tried1 = returned.second as? File
        if (tried0 == null && tried1 == null && returned.first.isSuccess()) {
            throw IllegalStateException("ZixaMC REST: Method $path returned invalid type: ${returned.second?.javaClass}, expected: ${result.typeClass} or ${File::class.java}")
        }
        val finalFile = tried0 ?: SendFile(tried1!!)
        return result.write(returned.first, finalFile)
    }
}