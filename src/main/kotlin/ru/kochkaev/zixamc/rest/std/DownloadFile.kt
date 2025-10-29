package ru.kochkaev.zixamc.rest.std

import io.ktor.http.HttpStatusCode
import net.fabricmc.loader.api.FabricLoader
import ru.kochkaev.zixamc.rest.method.RestMapping
import ru.kochkaev.zixamc.rest.method.SendFileMethodType

object DownloadFile: SendFileMethodType<DownloadFile.Request>(
    path = "std/downloadFile",
    requiredPermissions = listOf(Permissions.DOWNLOAD_FILES),
    mapping = RestMapping.POST,
    params = mapOf(),
    bodyModel = Request::class.java,
    method = { sql, permissions, params, body ->
        if (body == null) {
            HttpStatusCode.BadRequest to "Request body is required"
        } else {
            val file = FabricLoader.getInstance().gameDir.resolve(body.filePath).toFile()
            if (!file.exists() || !file.isFile) {
                HttpStatusCode.NotFound to "File not found: ${body.filePath}"
            } else if (!file.canRead()) {
                HttpStatusCode.Forbidden to "Cannot read file: ${body.filePath}"
            } else HttpStatusCode.OK to file
        }
    }
) {
    data class Request(
        /** Path to the file to be downloaded from game dir */
        val filePath: String,
    )
}