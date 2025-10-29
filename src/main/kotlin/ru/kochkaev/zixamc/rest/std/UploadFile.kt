package ru.kochkaev.zixamc.rest.std

import io.ktor.http.HttpStatusCode
import ru.kochkaev.zixamc.rest.method.MethodResult
import ru.kochkaev.zixamc.rest.method.ReceiveFileMethodType
import ru.kochkaev.zixamc.rest.method.RestMapping

object UploadFile: ReceiveFileMethodType<UploadFile.Answer>(
    path = "std/uploadFile",
    requiredPermissions = listOf(Permissions.UPLOAD_FILES),
    mapping = RestMapping.POST,
    params = mapOf(
        "filePath" to (String::class.java to true)
    ),
    savePathSupplier = { _, _, params, initial ->
        initial.resolve(params["filePath"].toString())
    },
    result = MethodResult.create(),
    method = { sql, permissions, params, file ->
        if (file == null || !file.exists() || !file.isFile) {
            HttpStatusCode.BadRequest to "File is required in the request body"
        } else {
            HttpStatusCode.OK to Answer(
                filePath = file.path,
                fileSize = file.length(),
            )
        }
    }
) {
    data class Answer(
        val filePath: String,
        val fileSize: Long,
    )
}