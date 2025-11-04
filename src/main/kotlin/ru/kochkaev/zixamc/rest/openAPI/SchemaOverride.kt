package ru.kochkaev.zixamc.rest.openAPI

import java.lang.reflect.Type

data class SchemaOverride(
    val instance: Any? = null,
    val type: Type? = null,
    val schemaType: String? = null,
    val fields: Map<String, FieldOverride> = mapOf(),
    val mapKey: FieldOverride? = null,
    val listOrMapValue: FieldOverride? = null,
    val exclude: Boolean = false,
    val ignoreParameters: Boolean = false,
    val ifIsAssignable: Boolean = false,
)
