package ru.kochkaev.zixamc.rest.openAPI

data class OneOfOverride(
    val constant: Any,
    val constantAsIs: Boolean = false,
    val constantSerialized: Any? = null,
    val schemaType: String? = null,
    val title: String? = null,
    val description: String? = null,
    /** If constant is equals to serialized constant of enum */
    val exclude: Boolean = false,
)
