package ru.kochkaev.zixamc.rest

open class RestException(
    statusCode: Int,
    description: String,
): Exception("ZixaMC-REST Client Exception: $statusCode $description")