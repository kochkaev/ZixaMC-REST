package ru.kochkaev.zixamc.rest.openAPI

import java.lang.reflect.Type

data class SchemaOverride<T>(
    val instance: T? = null,
    val type: Type? = null,
    val ignoreParameters: Boolean = true,
)
