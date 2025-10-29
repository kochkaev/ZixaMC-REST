package ru.kochkaev.zixamc.rest.method

import io.ktor.http.HttpStatusCode
import net.fabricmc.loader.api.FabricLoader
import ru.kochkaev.zixamc.rest.SQLClient
import java.io.File
import java.nio.file.Path

open class ReceiveFileMethodType<R>(
    path: String,
    requiredPermissions: List<String>,
    mapping: RestMapping,
    params: Map<String, Pair<Class<*>, Boolean>> = mapOf(),
    private val savePathSupplier: suspend (SQLClient, List<String>, Map<String, Any?>, Path) -> Path,
    result: MethodResult<R>,
    method: suspend (SQLClient, List<String>, Map<String, Any?>, File?) -> Pair<HttpStatusCode, Any?>
): RestMethodType<File, R>(
    path = path,
    requiredPermissions = requiredPermissions,
    mapping = mapping,
    params = params,
    bodyModel = null,
    result = result,
    method = method
) {
    suspend fun getPath(
        sql: SQLClient,
        permissions: List<String>,
        params: Map<String, Any?>,
        initial: Path = FabricLoader.getInstance().gameDir
    ): Path {
        return savePathSupplier(sql, permissions, params, initial)
    }
}