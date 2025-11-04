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
import io.swagger.v3.oas.models.SpecVersion
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
import org.jetbrains.annotations.Nullable
import ru.kochkaev.zixamc.api.Initializer
import ru.kochkaev.zixamc.api.config.GsonManager
import ru.kochkaev.zixamc.api.config.serialize.SimpleAdapter
import ru.kochkaev.zixamc.rest.RestManager
import ru.kochkaev.zixamc.rest.SQLClient
import ru.kochkaev.zixamc.rest.method.ReceiveFileMethodType
import ru.kochkaev.zixamc.rest.method.RestMapping
import ru.kochkaev.zixamc.rest.method.RestMethodType
import ru.kochkaev.zixamc.rest.method.SendFile
import ru.kochkaev.zixamc.rest.Config.Companion.config
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
import kotlin.reflect.jvm.kotlinProperty

object OpenAPIGenerator {

    var enabled: Boolean = false
        private set
    private val gson = GsonBuilder()
        .setExclusionStrategies(
            object : ExclusionStrategy {
                override fun shouldSkipField(field: FieldAttributes): Boolean {
                    return field.declaringClass == Parameter::class.java && field.name == "in" || field.name == "Companion" || field.name == "specVersion" && field.declaringClass != OpenAPI::class.java
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
            .openapi("3.1.0")
            .specVersion(SpecVersion.V31)
            .info(Info()
                .title(config.openApi.title)
                .description(config.openApi.description)
                .version(config.openApi.version)
            )
            .servers(listOf(Server().url(config.openApi.urlPrefix)))
            .components(Components()
                .addSecuritySchemes(
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
            !((hiddenAnn != null && hiddenAnn.value || config.openApi.hideWithoutPermsByDefault) && tokenPerms?.containsAll(method.requiredPermissions) != true)
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

    private val mutex: Mutex = Mutex()
    private val cachedSchemas: MutableList<Pair<MutableList<RestMethodType<*, *>>, Pair<String, Schema<Any>>>> = arrayListOf()
    private val cachedMethods: MutableMap<RestMethodType<*, *>, PathItem> = TreeMap()
    private val schemaOverrides: MutableMap<Type, SchemaOverride> = hashMapOf()
    fun updateCache(ignoreUnswitched: Boolean = false) = Initializer.coroutineScope.launch { mutex.withLock {
        if (!ignoreUnswitched && enabled == config.openApi.enabled) return@launch
        cachedSchemas.clear()
        cachedMethods.clear()
        if (enabled && !config.openApi.enabled) {
            enabled = false
            return@launch
        }
        enabled = true

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
                    .schema(Schema<Any>().type(getSchemaType(type, getOverride(type))))
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

    fun overrideSchema(schemaType: Type, override: SchemaOverride, updateOpenApi: Boolean = true) {
        schemaOverrides[schemaType] = override
        if (updateOpenApi) updateCache(true)
    }
    fun overrideSchemas(vararg overrides: Pair<Type, SchemaOverride>, updateOpenApi: Boolean = true) {
        schemaOverrides.putAll(overrides.toMap())
        if (updateOpenApi) updateCache(true)
    }

    private fun getSchemaType(type: Class<*>, override: SchemaOverride? = null): String =
        override?.schemaType ?: when {
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
        name: String = resolveName(type),
        notGlobal: Boolean = false,
        schemaType: String? = null,
    ): Schema<Any>? {
        val override = getOverride(type)
        override?.exclude?.also { if (it) return null }
        val type = override?.type ?: type
        val default = override?.instance ?: default
        var schemaRef: Schema<Any>? = cachedSchemas.firstOrNull { (_, entry) -> entry.first == name } ?.second?.second
        var isPrimitive = schemaRef?.let { isPrimitive(it) }
        if (schemaRef == null) {
            schemaRef = (default
                ?.let { tryGetSchemaFromJson(it, type, schemas, override, schemaType) }
                ?: tryGetSchemaFromReflection(type, schemas, default, override, schemaType)
                ?: Schema<Any>().type("object"))
            schemaRef.title(resolveSimpleName(type))
            isPrimitive = isPrimitive(schemaRef)
            if (!isPrimitive)
                cachedSchemas.add(arrayListOf<RestMethodType<*, *>>() to (name to schemaRef))
        }
        if (!schemas.containsKey(name) && !isPrimitive!! && !notGlobal) {
            schemas[name] = schemaRef
        }
        val schema = if (!isPrimitive!!) cloneSchema(schemaRef) else schemaRef
        schema.title(resolveSimpleName(type))
        return schema
    }

    private fun getOverride(type: Type): SchemaOverride? {
        val clazz = resolveClass(type)
        val override: SchemaOverride? = schemaOverrides[type] ?: schemaOverrides
            .filter {
                it.value.ignoreParameters && it.key == clazz ||
                        it.value.ifIsAssignable && resolveClass(it.key).isAssignableFrom(clazz)
            } .firstNotNullOfOrNull { it.value }
        return override
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
        override: SchemaOverride? = null,
        schemaType: String? = null,
    ): Schema<Any>? {
        val schema = Schema<Any>()
        when (type) {
            is ParameterizedType -> {
                val raw = type.rawType as Class<*>
                when {
                    Collection::class.java.isAssignableFrom(raw) -> {
                        schema.type("array")
                        if (override?.listOrMapValue?.exclude == true) return schema
                        val itemType = override?.listOrMapValue?.type ?: type.actualTypeArguments[0]
                        val itemDefault = override?.listOrMapValue?.instance ?: (default as? Collection<*>)?.first()
                        val itemSchema = getSchemaRef(
                            type = itemType,
                            schemas = schemas,
                            default = itemDefault,
                            notGlobal = override?.listOrMapValue?.excludeFromSchemas ?: false,
                        )
                        itemSchema?.also { processField(
                            field = null,
                            schema = it,
                            fieldDefault = itemDefault,
                            override = override?.listOrMapValue,
                        ) }
                        schema.items = itemSchema
                    }
                    Map::class.java.isAssignableFrom(raw) -> {
                        schema.type("object")
                        if (override?.mapKey?.exclude == true || override?.listOrMapValue?.exclude == true) return schema
                        val keyType = override?.mapKey?.type ?: type.actualTypeArguments[0]
                        val valueType = override?.listOrMapValue?.type ?: type.actualTypeArguments[1]
                        val map = (default as? Map<*, *>)
                        val keyDefault = override?.mapKey?.instance ?: map?.keys?.first()
                        val valueDefault = override?.listOrMapValue?.instance ?: map?.values?.first()
                        val keySchema = getSchemaRef(
                            type = keyType,
                            schemas = schemas,
                            default = keyDefault,
                            notGlobal = override?.mapKey?.excludeFromSchemas ?: false,
                        )
                        val valueSchema = getSchemaRef(
                            type = valueType,
                            schemas = schemas,
                            default = valueDefault,
                            notGlobal = override?.mapKey?.excludeFromSchemas ?: false,
                        )
                        keySchema?.also { processField(
                            field = null,
                            schema = it,
                            fieldDefault = keyDefault,
                            override = override?.mapKey,
                        ) }
                        valueSchema?.also { processField(
                            field = null,
                            schema = it,
                            fieldDefault = valueDefault,
                            override = override?.listOrMapValue,
                        ) }
                        schema.propertyNames = keySchema
                        schema.additionalProperties = valueSchema
                    }
                    else -> schema.type("object")
                }
            }
            is Class<*> -> {
                val resolvedClass = type
                schema.type(getSchemaType(resolvedClass, override))
                if (schema.type == "object") {
                    val props = mutableMapOf<String, Schema<Any>>()
                    val required = mutableListOf<String>()
                    resolvedClass.declaredFields.forEach { field ->
                        try {
                            field.isAccessible = true
                        } catch (_: Exception) {
                            return@forEach
                        }
                        if (shouldSkipField(field)) return@forEach
                        val fieldOverride = override?.fields[field.name]
                        fieldOverride?.exclude?.let { if (it) return@forEach }
                        val fieldType = fieldOverride?.type ?: field.genericType
                        val fieldDefault = fieldOverride?.instance ?: if (default != null) try {
                            field.get(default)
                        } catch (_: Exception) {
                            null
                        } else null
//                        val propertyName = resolveName(fieldType)
                        val propertySchema = getSchemaRef(
                            type = fieldType,
                            schemas = schemas,
                            default = fieldDefault,
                            notGlobal = fieldOverride?.excludeFromSchemas ?: false
                        ) ?: return@forEach // Excluded
                        processField(
                            field = field,
                            schema = propertySchema,
                            fieldDefault = fieldDefault,
                            override = fieldOverride,
                            requiredList = required
                        )
                        props[field.name] = propertySchema
                    }
                    schema.properties = props
                    schema.required = required
                } else if (resolvedClass.isEnum) {
                    schema._enum(resolvedClass.enumConstants.map { it.toString() })
                }
            }
            is WildcardType -> {
                val upperBounds = type.upperBounds
                if (upperBounds.isNotEmpty()) {
                    return tryGetSchemaFromReflection(upperBounds[0], schemas, default)
                }
                val lowerBounds = type.lowerBounds
                if (lowerBounds.isNotEmpty()) {
                    return tryGetSchemaFromReflection(lowerBounds[0], schemas, default)
                }
                schema.type("object")
            }
            is GenericArrayType -> {
                schema.type("array")
                val componentType = type.genericComponentType
                schema.items(getSchemaRef(componentType, schemas, default))
            }
            is TypeVariable<*> -> {
                val bounds = type.bounds
                if (bounds.isNotEmpty() && bounds[0] != Any::class.java) {
                    return tryGetSchemaFromReflection(bounds[0], schemas, default)
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
        override: SchemaOverride? = null,
        schemaType: String? = null,
    ): Schema<Any>? {
        if (instance == null) return null
        val jsonTree = GsonManager.gson.toJsonTree(instance)
        return processJsonToSchema(jsonTree, instance, type, schemas, override, schemaType)
    }

    private fun processJsonToSchema(
        elem: JsonElement,
        instance: Any,
        type: Type,
        schemas: MutableMap<String, Schema<Any>>,
        override: SchemaOverride? = null,
        schemaType: String? = null,
    ): Schema<Any>? {
        val clazz = resolveClass(type)
        return when {
            elem.isJsonObject -> {
                val obj = elem.asJsonObject
                val schema = Schema<Any>().type("object")
                when {
                    Map::class.java.isAssignableFrom(clazz) -> {
                        val entries = obj.entrySet().toList()
                        val parameterized = type as? ParameterizedType
                        if (override?.mapKey?.exclude == true || override?.listOrMapValue?.exclude == true) return schema
                        else if (parameterized != null) {
                            val keyType = override?.mapKey?.type ?: type.actualTypeArguments[0]
                            val valueType = override?.listOrMapValue?.type ?: type.actualTypeArguments[1]
                            val keyDefault = override?.mapKey?.instance ?: entries.firstOrNull()?.key
                            val valueDefault = override?.listOrMapValue?.instance ?: entries.firstOrNull()?.value
                            val keySchema = getSchemaRef(
                                type = keyType,
                                schemas = schemas,
                                default = keyDefault,
                                notGlobal = override?.mapKey?.excludeFromSchemas ?: false,
                            )
                            val valueSchema = getSchemaRef(
                                type = valueType,
                                schemas = schemas,
                                default = valueDefault,
                                notGlobal = override?.mapKey?.excludeFromSchemas ?: false,
                            )
                            keySchema?.also { processField(
                                field = null,
                                schema = it,
                                fieldDefault = keyDefault,
                                override = override?.mapKey,
                            ) }
                            valueSchema?.also { processField(
                                field = null,
                                schema = it,
                                fieldDefault = valueDefault,
                                override = override?.listOrMapValue,
                            ) }
                            schema.propertyNames = keySchema
                            schema.additionalProperties = valueSchema
                        }
                        schema
                    }
                    else -> {
                        val props = mutableMapOf<String, Schema<Any>>()
                        val required = mutableListOf<String>()
                        obj.entrySet().forEach { (key, _) ->
                            val field = clazz.declaredFields.firstOrNull {
                                it.name == key || it.getAnnotation(SerializedName::class.java)?.value == key
                            } ?: return@forEach
                            try {
                                field.isAccessible = true
                            } catch (_: Exception) { return@forEach }
                            val fieldOverride = override?.fields[field.name]
                            fieldOverride?.exclude?.let { if (it) return@forEach }
                            val itemType = fieldOverride?.type ?: field.genericType
                            val fieldDefault = fieldOverride?.instance ?:try {
                                field.get(instance)
                            } catch (_: Exception) { null }
                            val itemName = resolveName(itemType)
                            val itemSchema = getSchemaRef(
                                type = itemType,
                                schemas = schemas,
                                default = fieldDefault,
                                name = itemName,
                                notGlobal = fieldOverride?.excludeFromSchemas ?: false,
                            ) ?: return@forEach
                            processField(
                                field = field,
                                schema = itemSchema,
                                fieldDefault = fieldDefault,
                                override = fieldOverride,
                                requiredList = required,
                            )
                            props[key] = itemSchema
                        }
                        schema.properties = props
                        schema.required = required
                    }
                }
                schema
            }
            elem.isJsonArray -> {
                val arr = elem.asJsonArray
                val schema = Schema<Any>().type("array")
                if (override?.listOrMapValue?.exclude == true) return schema
                val itemType = override?.listOrMapValue?.type ?: (type as? ParameterizedType)?.actualTypeArguments[0]
                if (itemType!=null) {
                    val itemName = resolveName(itemType)
                    val itemDefault = override?.listOrMapValue?.instance ?: if (arr.size() > 0) arr[0] else null
                    val itemSchema = getSchemaRef(
                        type = itemType,
                        schemas = schemas,
                        default = itemDefault,
                        name = itemName,
                        notGlobal = override?.listOrMapValue?.excludeFromSchemas ?: false,
                    )
                    itemSchema?.also { processField(
                        field = null,
                        schema = it,
                        fieldDefault = itemDefault,
                        override = override?.listOrMapValue,
                    ) }
                    schema.items = itemSchema
                }
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

    private fun processField(
        field: Field?,
        schema: Schema<Any>,
        fieldDefault: Any? = null,
        override: FieldOverride? = null,
        requiredList: MutableList<String> = mutableListOf()
    ) {
        val fieldType = field?.genericType
        val fieldClass = fieldType?.let { resolveClass(it) }

        // Required
        val required = override?.required ?: (
            field?.kotlinProperty?.returnType?.isMarkedNullable != true
            && field?.getAnnotation(Nullable::class.java) == null
//            && field.getAnnotation(NotNull::class.java) != null
        )
        if (required) {
            field?.name?.let { requiredList.add(it) }
        }
        // Nullable
        if (override?.nullable ?: !required) schema.nullable = true
        // Description
        val desc = field?.getAnnotation(RestDescription::class.java)?.value
        if (desc != null) schema.description(desc)
        // Example
        val example = override?.example ?: field?.getAnnotation(RestExample::class.java)?.value ?: fieldDefault
        if (example != null) {
            schema.example(example)
        }
        // Default
        try {
            schema._default(override?.example ?: fieldDefault)
        } catch (_: Exception) {}
        // Enum
        if (fieldClass?.isEnum == true) {
            schema._enum(fieldClass.enumConstants.map { it.toString() })
        }
        // Format
        if (override?.format?.isNotEmpty() ?: (fieldClass == Date::class.java || fieldClass == Instant::class.java)) {
            schema.format(override?.format ?: "date-time")
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
        is Class<*> -> type.name.substringAfterLast('.')
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
        else -> type.typeName.substringAfterLast('.')
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

    private fun isPrimitive(schema: Schema<Any>) =
        schema.type != "object" && !(schema.type == "string" && schema.enum?.isNotEmpty() ?: false)
    private fun cloneSchema(original: Schema<*>): Schema<Any> {
        val clone = Schema<Any>()
        clone.type = original.type
        clone.format = original.format
        clone.title = original.title
        clone.description = original.description
        clone.default = original.getDefault()
        clone.example = original.example
        clone.enum = original.enum
        clone.nullable = original.nullable
        original.properties?.let { props ->
            clone.properties = mutableMapOf<String, Schema<Any>>().apply {
                props.forEach { (key, value) ->
                    this[key] = cloneSchema(value)
                }
            }
        }
        original.items?.let { clone.items = cloneSchema(it) }
        (original.additionalProperties as? Schema<*>)?.let { clone.additionalProperties = cloneSchema(it) }
        return clone
    }
}
