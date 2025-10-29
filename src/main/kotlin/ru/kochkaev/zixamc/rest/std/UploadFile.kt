package ru.kochkaev.zixamc.rest.std

import io.ktor.http.HttpStatusCode
import ru.kochkaev.zixamc.rest.ReceiveFileMethodType
import ru.kochkaev.zixamc.rest.RestMapping

object UploadFile: ReceiveFileMethodType(
    path = "std/uploadFile",
    requiredPermissions = listOf(Permissions.UPLOAD_FILES),
    mapping = RestMapping.POST,
    params = mapOf(
        "filePath" to (String::class.java to true)
    ),
    savePathSupplier = { _, _, params, initial ->
        initial.resolve(params["filePath"].toString())
    },
    method = { sql, permissions, params, file ->
        if (file == null || !file.exists() || !file.isFile) {
            HttpStatusCode.BadRequest to "File is required in the request body"
        } else {
            HttpStatusCode.OK to mapOf(
                "filePath" to file.path,
                "fileSize" to file.length()
            )
        }
    }
)