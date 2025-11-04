package ru.kochkaev.zixamc.rest.openAPI

import java.lang.reflect.Type

data class FieldOverride(
    val instance: Any? = null,
    val example: Any? = null,
    val type: Type? = null,
    val schemaType: String? = null,
    val required: Boolean? = null,
    val nullable: Boolean? = null,
    val format: String? = null,
    val exclude: Boolean = false,
    val excludeFromSchemas: Boolean = false,
)
