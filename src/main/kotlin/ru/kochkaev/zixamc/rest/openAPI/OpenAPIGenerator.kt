package ru.kochkaev.zixamc.rest.openAPI

import com.google.gson.ExclusionStrategy
import com.google.gson.FieldAttributes
import com.google.gson.GsonBuilder
import com.google.gson.JsonElement
import com.google.gson.annotations.SerializedName
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.PathItem
import io.swagger.v3.oas.models.Paths
import io.swagger.v3.oas.models.Operation
import io.swagger.v3.oas.models.parameters.QueryParameter
import io.swagger.v3.oas.models.media.Schema
import io.swagger.v3.oas.models.media.StringSchema
import io.swagger.v3.oas.models.responses.ApiResponse
import io.swagger.v3.oas.models.responses.ApiResponses
import io.swagger.v3.oas.models.media.Content
import io.swagger.v3.oas.models.media.MediaType
import io.swagger.v3.oas.models.parameters.Parameter
import io.swagger.v3.oas.models.security.SecurityScheme
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.parameters.RequestBody
import io.swagger.v3.oas.models.servers.Server
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import ru.kochkaev.zixamc.api.Initializer
import ru.kochkaev.zixamc.api.config.GsonManager
import ru.kochkaev.zixamc.api.config.serialize.SimpleAdapter
import ru.kochkaev.zixamc.rest.RestManager
import ru.kochkaev.zixamc.rest.SQLClient
import ru.kochkaev.zixamc.rest.method.ReceiveFileMethodType
import ru.kochkaev.zixamc.rest.method.RestMapping
import ru.kochkaev.zixamc.rest.method.RestMethodType
import ru.kochkaev.zixamc.rest.method.SendFile
import ru.kochkaev.zixamc.rest.method.SendFileMethodType
import java.io.File
import java.lang.reflect.AccessFlag
import java.lang.reflect.Array
import java.lang.reflect.Field
import java.lang.reflect.GenericArrayType
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.lang.reflect.TypeVariable
import java.lang.reflect.WildcardType
import java.time.Instant
import java.util.Date
import java.util.TreeMap
import java.util.UUID

object OpenAPIGenerator {

    private val gson = GsonBuilder()
        .setExclusionStrategies(
            object : ExclusionStrategy {
                override fun shouldSkipField(field: FieldAttributes): Boolean {
                    return field.declaringClass == Parameter::class.java && field.name == "in" || field.name == "Companion"
                }
                override fun shouldSkipClass(clazz: Class<*>): Boolean = false
            },
        )
        .registerTypeAdapter(SecurityScheme.Type::class.java, SimpleAdapter(
            reader = { SecurityScheme.Type.valueOf(it.nextString().uppercase()) },
            writer = { out, type -> out.value(type.toString()) }
        ))
        .disableHtmlEscaping()
        .enableComplexMapKeySerialization()
        .setPrettyPrinting()
        .create()

    private val openApiBase: OpenAPI
        get() = OpenAPI()
            .info(Info().title("ZixaMC REST API").version("1.0"))
            .servers(listOf(Server().url("/api")))
            .components(
                Components().addSecuritySchemes(
                    "bearerAuth",
                    SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                )
            )

    suspend fun generateSpec(token: UUID? = null): OpenAPI = mutex.withLock {
        val openApi = openApiBase
        val paths = Paths()
        val tokenPerms = token?.let { SQLClient.get(it)?.permissions?.get() }

        val methods: Map<RestMethodType<*, *>, PathItem> = cachedMethods.filterKeys { method ->
            // Hidden if @RestHiddenIfNoPerm
            val hiddenAnn = method.javaClass.getAnnotation(RestHiddenIfNoPerm::class.java)
            !(hiddenAnn != null && hiddenAnn.value && tokenPerms?.containsAll(method.requiredPermissions) != true)
        }
        val schemas: Map<String, Schema<Any>> = cachedSchemas
            .filter { it.first.fold(false) { aac, method -> aac || methods.containsKey(method) } }
            .associate { it.second }

        openApi.components.schemas = schemas
        paths.putAll(methods.mapKeys { "/${it.key.path}" })
        openApi.paths = paths
        return openApi
    }

    suspend fun json(token: UUID? = null): String =
        gson.toJson(generateSpec(token))

    private var mutex: Mutex = Mutex()
    private lateinit var cachedSchemas: MutableList<Pair<MutableList<RestMethodType<*, *>>, Pair<String, Schema<Any>>>>
    private lateinit var cachedMethods: MutableMap<RestMethodType<*, *>, PathItem>
    fun updateCache() = Initializer.coroutineScope.launch { mutex.withLock {
        cachedSchemas = arrayListOf()
        cachedMethods = TreeMap()

        RestManager.registeredMethods.values.forEach { method ->
            val schemas = hashMapOf<String, Schema<Any>>()

            val pathSegments = method.path.split("/").filter { it.isNotBlank() }
            val operation = Operation()
            val extensions = hashMapOf<String, Any>()

            // Security
            if (method.requiredPermissions.isNotEmpty()) {
                operation.addSecurityItem(SecurityRequirement().addList("bearerAuth", emptyList<String>()))
            }
            extensions["x-permissions"] = method.requiredPermissions.toTypedArray()

            // Description
            val clazz = method::class.java
            val definedDescription = clazz.getAnnotation(RestDescription::class.java)?.value
            val description = (definedDescription?.let { "$it\n\n" } ?: "") + method.requiredPermissions.let { if (it.isNotEmpty()) "Required permissions: ${it.joinToString(", ")}" else "No required permissions" }
            operation.description(description)

            // Tags
            pathSegments.dropLast(1).forEach { operation.addTagsItem(it) }

            // Parameters
            method.params.forEach { (name, pair) ->
                val (type, required) = pair
                operation.addParametersItem(QueryParameter()
                    .name(name)
                    .required(required)
                    .schema(Schema<Any>().type(getSchemaType(type)))
                    .`in`("query")
                )
            }

            // Request Body
            val requestBody = Content()
            if (method.bodyModel != null && method.bodyModel != Any::class.java && method.bodyModel != Unit::class.java && method.bodyModel != Nothing::class.java) {
                val ref = getSchemaRef(
                    type = method.bodyModel,
                    schemas = schemas,
                    default = method.bodyDefault,
                )
                requestBody.addMediaType("application/json", MediaType().schema(ref))
            } else if (method is ReceiveFileMethodType<*>) {
                requestBody.addMediaType("application/octet-stream", MediaType()
                    .schema(StringSchema().format("binary")))
            }
            operation.requestBody(RequestBody().content(requestBody).required(method.bodyModel != null))

            // Response
            val responses = ApiResponses()
            method.result.results.forEach { (code, result) ->
                val content = Content()
                val isSendFile = result.type == File::class.java || result.type == SendFile::class.java
                val isString = result.type == String::class.java
                val hasResponse = result.type != Unit::class.java && result.type != Nothing::class.java && result.type != Any::class.java
                var description: String? = null
                if (isSendFile) {
                    content.addMediaType(
                        "application/octet-stream", MediaType()
                            .schema(StringSchema().format("binary"))
                    )
                } else if (isString) {
                    description = result.default as String?
                } else if (hasResponse) {
                    val ref = getSchemaRef(
                        type = result.type,
                        schemas = schemas,
                        default = result.default,
                    )
                    content.addMediaType("application/json", MediaType().schema(ref))
                }
                responses.addApiResponse(
                    code.value.toString(), ApiResponse()
                        .description(if (hasResponse || isSendFile || isString) "${code.description}. ${description?:""}" else "No content")
                        .content(content)
                )
            }
            operation.responses(responses)

            // Path Item
            operation.extensions(extensions)
            val pathItem = PathItem()
            when (method.mapping) {
                RestMapping.GET -> pathItem.get(operation)
                RestMapping.POST -> pathItem.post(operation)
                RestMapping.PUT -> pathItem.put(operation)
                RestMapping.DELETE -> pathItem.delete(operation)
            }

            cachedSchemas.forEach {
                if (schemas.keys.contains(it.second.first))
                    it.first.add(method)
            }
            cachedMethods[method] = pathItem
        }
    } }

    private fun getSchemaType(type: Class<*>): String = when {
        type.isEnum -> "string"
        type == String::class.java || type == CharSequence::class.java -> "string"
        type == Int::class.java || type == Integer::class.java || type == Short::class.java || type == Byte::class.java -> "integer"
        type == Long::class.java || type == java.lang.Long::class.java -> "integer"
        type == Float::class.java || type == Double::class.java || type == Number::class.java -> "number"
        type == Boolean::class.java -> "boolean"
        type == java.lang.Boolean::class.java -> "boolean"
        type == Date::class.java || type == Instant::class.java -> "string"  // format: "date-time"
        type.isArray || Collection::class.java.isAssignableFrom(type) -> "array"
        Map::class.java.isAssignableFrom(type) -> "object"
        else -> "object"
    }

    private fun getSchemaRef(
        type: Type,
        schemas: MutableMap<String, Schema<Any>>,
        default: Any? = null,
        visited: MutableSet<String> = mutableSetOf(),
        name: String = resolveName(type),
    ): Schema<Any> {
        if (visited.contains(name)) {
            return Schema<Any>().`$ref`("#/components/schemas/$name")
        }
        visited.add(name)
        var schema: Schema<Any>? = cachedSchemas.firstOrNull { (_, entry) -> entry.first == name } ?.second?.second
        if (schema == null) {
            schema = default
                ?.let { tryGetSchemaFromJson(it, type, schemas, visited) }
                ?: tryGetSchemaFromReflection(type, schemas, default, visited)
                ?: Schema<Any>().type("object")
            schema.title(resolveSimpleName(type))
            cachedSchemas.add(arrayListOf<RestMethodType<*, *>>() to (name to schema))
        }
        if (!schemas.containsKey(name)) {
            schema.title(resolveSimpleName(type))
            schemas[name] = schema
        }
        return Schema<Any>().`$ref`("#/components/schemas/$name")
    }

    private val hasNoAccess = listOf(AccessFlag.PRIVATE, AccessFlag.PROTECTED)
    private fun shouldSkipField(field: Field): Boolean {
        return field.isSynthetic ||
                hasNoAccess.fold(true) { aac, it -> aac && !field.accessFlags().contains(it) } ||
                field.name == "Companion" ||
                field.name.endsWith($$"$serializer") ||
                field.getAnnotation(Transient::class.java) != null
    }

    private fun tryGetSchemaFromReflection(
        type: Type,
        schemas: MutableMap<String, Schema<Any>>,
        default: Any? = null,
        visited: MutableSet<String> = mutableSetOf(),
    ): Schema<Any>? {
        val schema = Schema<Any>()
        when (type) {
            is ParameterizedType -> {
                val raw = type.rawType as Class<*>
                when {
                    Collection::class.java.isAssignableFrom(raw) -> {
                        schema.type("array")
                        val itemType = type.actualTypeArguments[0]
                        schema.items(getSchemaRef(itemType, schemas, (default as? Collection<*>)?.first(), visited))
                    }
                    Map::class.java.isAssignableFrom(raw) -> {
                        schema.type("object")
                        val keyType = type.actualTypeArguments[0]
                        val valueType = type.actualTypeArguments[1]
                        val map = (default as? Map<*, *>)
                        getSchemaRef(keyType, schemas, map?.keys?.first(), visited)
                        schema.additionalProperties(getSchemaRef(valueType, schemas, map?.values?.first(), visited))
                    }
                    else -> schema.type("object")
                }
            }
            is Class<*> -> {
                val resolvedClass = type
                schema.type(getSchemaType(resolvedClass))
                if (schema.type == "object") {
                    resolvedClass.declaredFields.forEach { field ->
                        try {
                            field.isAccessible = true
                        } catch (_: Exception) {
                            return@forEach
                        }
                        if (shouldSkipField(field)) return@forEach
                        val fieldType = field.genericType
                        val fieldDefault = if (default != null) try {
                            field.get(default)
                        } catch (_: Exception) {
                            null
                        } else null
                        val propertySchema = getSchemaRef(fieldType, schemas, fieldDefault, visited)
                        processField(field, propertySchema, fieldDefault)
                        schema.addProperty(field.name, propertySchema)
                    }
                } else if (resolvedClass.isEnum) {
                    schema._enum(resolvedClass.enumConstants.map { it.toString() })
                }
            }
            is WildcardType -> {
                val upperBounds = type.upperBounds
                if (upperBounds.isNotEmpty()) {
                    return tryGetSchemaFromReflection(upperBounds[0], schemas, default, visited)
                }
                val lowerBounds = type.lowerBounds
                if (lowerBounds.isNotEmpty()) {
                    return tryGetSchemaFromReflection(lowerBounds[0], schemas, default, visited)
                }
                schema.type("object")
            }
            is GenericArrayType -> {
                schema.type("array")
                val componentType = type.genericComponentType
                schema.items(getSchemaRef(componentType, schemas, default, visited))
            }
            is TypeVariable<*> -> {
                val bounds = type.bounds
                if (bounds.isNotEmpty() && bounds[0] != Any::class.java) {
                    return tryGetSchemaFromReflection(bounds[0], schemas, default, visited)
                }
                schema.type("object")
            }
            else -> return null
        }
        return schema
    }
    private fun tryGetSchemaFromJson(
        instance: Any? = null,
        type: Type,
        schemas: MutableMap<String, Schema<Any>>,
        visited: MutableSet<String> = mutableSetOf(),
    ): Schema<Any>? {
        if (instance == null) return null
        val jsonTree = GsonManager.gson.toJsonTree(instance)
        return processJsonToSchema(jsonTree, instance, type, schemas, visited)
    }

    private fun processJsonToSchema(
        elem: JsonElement,
        instance: Any,
        type: Type,
        schemas: MutableMap<String, Schema<Any>>,
        visited: MutableSet<String> = mutableSetOf(),
    ): Schema<Any>? {
        val clazz = resolveClass(type)
        return when {
            elem.isJsonObject -> {
                val obj = elem.asJsonObject
                val schema = Schema<Any>().type("object")
                when {
                    Map::class.java.isAssignableFrom(clazz) -> {
                        val entries = obj.entrySet().toList()
                        val parameterized = type as ParameterizedType
                        val keyType = parameterized.actualTypeArguments[0]
                        val keyName = resolveName(keyType)
                        val valueType = parameterized.actualTypeArguments[1]
                        val valueName = resolveName(valueType)
                        getSchemaRef(keyType, schemas, if (entries.isNotEmpty()) entries[0].key else null, visited, keyName)
                        getSchemaRef(valueType, schemas, if (entries.isNotEmpty()) entries[0].value else null, visited, valueName)
                        schema.additionalProperties = Schema<Any>().`$ref`("#/components/schemas/$valueName")
                        schema
                    }
                    else -> {
                        val props = mutableMapOf<String, Schema<Any>>()
                        obj.entrySet().forEach { (key, value) ->
                            val field = clazz.declaredFields.first {
                                it.name == key || it.getAnnotation(SerializedName::class.java)?.value == key
                            }
                            val fieldDefault = field.get(instance)
                            val itemType = field.genericType
                            val itemName = resolveName(itemType)
                            val itemSchema = getSchemaRef(itemType, schemas, fieldDefault, visited, itemName)
                            processField(field, itemSchema, fieldDefault)
                            props[key] = Schema<Any>().`$ref`("#/components/schemas/$itemName")
                        }
                        schema.properties = props
                    }
                }
                schema
            }
            elem.isJsonArray -> {
                val arr = elem.asJsonArray
                val schema = Schema<Any>().type("array")
                val itemType = (type as ParameterizedType).actualTypeArguments[0]
                val itemName = resolveName(itemType)
                getSchemaRef(itemType, schemas, if (arr.size() > 0) arr[0] else null, visited, itemName)
                schema.items = Schema<Any>().`$ref`("#/components/schemas/$itemName")
                schema
            }
            elem.isJsonPrimitive -> {
                val p = elem.asJsonPrimitive
                Schema<Any>().apply {
                    type(when {
                        p.isBoolean -> "boolean"
                        p.isNumber -> if (p.asString.contains('.')) "number" else "integer"
                        else -> "string"
                    })
                    if (p.isBoolean) _default(p.asBoolean)
                    else if (p.isNumber) _default(p.asNumber)
                    else if (p.isString) _default(p.asString)
                }
            }
            else -> null
        }
    }

    private fun processField(field: Field, schema: Schema<Any>, fieldDefault: Any? = null) {
        val fieldType = field.genericType
        val fieldClass = resolveClass(fieldType)

//        // Description
//        val desc = field.getAnnotation(RestDescription::class.java)?.value
//        if (desc != null) schema.description(desc)
//        // Example
//        val example = field.getAnnotation(RestExample::class.java)?.value ?: fieldDefault
//        if (example != null) schema.addExample(example)
//        // Default
//        try {
//            schema._default(fieldDefault ?: if (fieldType == Boolean::class.java) false else null)
//        } catch (_: Exception) {}
        // Enum
        if (fieldClass.isEnum) {
            schema._enum(fieldClass.enumConstants.map { it.toString() })
        }
        // Format
        if (fieldClass == Date::class.java || fieldClass == Instant::class.java) {
            schema.format("date-time")
        }
    }

    private fun resolveName(type: Type): String = when (type) {
        is Class<*> -> type.name
        is ParameterizedType -> {
            "${resolveName(type.rawType)}<${type.actualTypeArguments.joinToString(",") { resolveName(it) }}>"
        }
        is WildcardType -> {
            val upper = type.upperBounds
            val lower = type.lowerBounds
            if (upper.isNotEmpty() && upper[0] != Any::class.java) {
                "? extends ${resolveName(upper[0])}"
            } else if (lower.isNotEmpty()) {
                "? super ${resolveName(lower[0])}"
            } else {
                "?"
            }
        }
        is GenericArrayType -> "${resolveName(type.genericComponentType)}[]"
        is TypeVariable<*> -> type.name
        else -> type.typeName
    }

    private fun resolveSimpleName(type: Type): String = when (type) {
        is Class<*> -> type.simpleName
        is ParameterizedType -> {
            "${resolveSimpleName(type.rawType)}<${type.actualTypeArguments.joinToString(",") { resolveSimpleName(it) }}>"
        }
        is WildcardType -> {
            val upper = type.upperBounds
            val lower = type.lowerBounds
            if (upper.isNotEmpty() && upper[0] != Any::class.java) {
                "? extends ${resolveSimpleName(upper[0])}"
            } else if (lower.isNotEmpty()) {
                "? super ${resolveSimpleName(lower[0])}"
            } else {
                "?"
            }
        }
        is GenericArrayType -> "${resolveSimpleName(type.genericComponentType)}[]"
        is TypeVariable<*> -> type.name
        else -> type.typeName
    }

    private fun resolveClass(type: Type): Class<*> = when (type) {
        is Class<*> -> type
        is ParameterizedType -> type.rawType as Class<*>
        is WildcardType -> {
            val upper = type.upperBounds
            if (upper.isNotEmpty()) resolveClass(upper[0]) else Any::class.java
        }
        is GenericArrayType -> {
            val component = type.genericComponentType
            val componentClass = resolveClass(component)
            Array.newInstance(componentClass, 0).javaClass
        }
        is TypeVariable<*> -> {
            val bounds = type.bounds
            if (bounds.isNotEmpty() && bounds[0] != Any::class.java) resolveClass(bounds[0])
            else Any::class.java
        }
        else -> Any::class.java
    }
}
