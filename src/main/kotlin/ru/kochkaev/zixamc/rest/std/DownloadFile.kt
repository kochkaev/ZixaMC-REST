package ru.kochkaev.zixamc.rest.std

import io.ktor.http.HttpStatusCode
import net.fabricmc.loader.api.FabricLoader
import ru.kochkaev.zixamc.rest.RestMapping
import ru.kochkaev.zixamc.rest.RestMethodType
import ru.kochkaev.zixamc.rest.SendFile

object DownloadFile: RestMethodType<DownloadFile.DownloadFileRequest>(
    path = "std/downloadFile",
    requiredPermissions = listOf(Permissions.DOWNLOAD_FILES),
    mapping = RestMapping.GET,
    params = mapOf(),
    bodyModel = DownloadFileRequest::class.java,
    method = { sql, permissions, params, body ->
        if (body == null) {
            HttpStatusCode.BadRequest to "Request body is required"
        } else {
            val file = FabricLoader.getInstance().gameDir.resolve(body.filePath).toFile()
            if (!file.exists() || !file.isFile) {
                HttpStatusCode.NotFound to "File not found: ${body.filePath}"
            } else if (!file.canRead()) {
                HttpStatusCode.Forbidden to "Cannot read file: ${body.filePath}"
            } else HttpStatusCode.OK to SendFile(file)
        }
    }
) {
    data class DownloadFileRequest(
        /** Path to the file to be downloaded from game dir */
        val filePath: String,
    )
}