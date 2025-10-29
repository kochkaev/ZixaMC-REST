package ru.kochkaev.zixamc.rest.method

import io.ktor.http.content.OutgoingContent
import java.io.File

data class SendFile(
    val file: File,
    val configure: OutgoingContent.() -> Unit = {}
)
