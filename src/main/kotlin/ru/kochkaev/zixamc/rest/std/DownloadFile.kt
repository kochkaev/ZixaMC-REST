package ru.kochkaev.zixamc.rest.std

import io.ktor.http.HttpStatusCode
import net.fabricmc.loader.api.FabricLoader
import ru.kochkaev.zixamc.rest.method.MethodResults
import ru.kochkaev.zixamc.rest.method.RestMapping
import ru.kochkaev.zixamc.rest.method.RestMethodType
import ru.kochkaev.zixamc.rest.method.SendFile
import ru.kochkaev.zixamc.rest.method.SendFileMethodType
import ru.kochkaev.zixamc.rest.method.methodResult
import ru.kochkaev.zixamc.rest.method.result

object DownloadFile: SendFileMethodType<DownloadFile.Request>(
    path = "std/downloadFile",
    requiredPermissions = listOf(Permissions.DOWNLOAD_FILES),
    mapping = RestMapping.POST,
    params = mapOf(),
    bodyModel = Request::class.java,
    result = MethodResults.create(HttpStatusCode.OK,
        HttpStatusCode.BadRequest to "Request body is empty".methodResult(),
        HttpStatusCode.NotFound to "File not found".methodResult(),
        HttpStatusCode.Forbidden to "Cannot read this file".methodResult(),

    ),
    method = { sql, permissions, params, body ->
        if (body == null) {
            HttpStatusCode.BadRequest.result("Request body is required")
        } else {
            val file = FabricLoader.getInstance().gameDir.resolve(body.filePath).toFile()
            if (!file.exists() || !file.isFile) {
                HttpStatusCode.NotFound.result("File not found: ${body.filePath}")
            } else if (!file.canRead()) {
                HttpStatusCode.Forbidden.result("Cannot read file: ${body.filePath}")
            } else HttpStatusCode.OK.result(file)
        }
    }
) {
    data class Request(
        /** Path to the file to be downloaded from game dir */
        val filePath: String,
    )
}