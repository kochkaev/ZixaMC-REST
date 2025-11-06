package ru.kochkaev.zixamc.rest.openAPI

import com.google.gson.TypeAdapter
import java.lang.reflect.Type

data class SchemaOverride(
    val instance: Any? = null,
    val description: String? = null,
    val type: Type? = null,
    val name: String? = null,
    val simpleName: String? = null,
    val global: Boolean? = null,
    val typeAdapter: TypeAdapter<*>? = null,
    val oneOf: List<OneOfOverride>? = null,
    val fields: Map<String, FieldOverride> = mapOf(),
    val mapKey: FieldOverride? = null,
    val listOrMapValue: FieldOverride? = null,
    val exclude: Boolean = false,
    val ignoreParameters: Boolean = false,
    val ifIsAssignable: Boolean = false,
)
