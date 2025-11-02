package ru.kochkaev.zixamc.rest.openAPI

import com.google.gson.ExclusionStrategy
import com.google.gson.FieldAttributes
import com.google.gson.GsonBuilder
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.PathItem
import io.swagger.v3.oas.models.Paths
import io.swagger.v3.oas.models.Operation
import io.swagger.v3.oas.models.parameters.QueryParameter
import io.swagger.v3.oas.models.media.Schema
import io.swagger.v3.oas.models.media.ObjectSchema
import io.swagger.v3.oas.models.media.StringSchema
import io.swagger.v3.oas.models.media.ArraySchema
import io.swagger.v3.oas.models.responses.ApiResponse
import io.swagger.v3.oas.models.responses.ApiResponses
import io.swagger.v3.oas.models.media.Content
import io.swagger.v3.oas.models.media.MediaType
import io.swagger.v3.oas.models.security.SecurityScheme
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.parameters.RequestBody
import ru.kochkaev.zixamc.api.config.serialize.SimpleAdapter
import ru.kochkaev.zixamc.rest.RestManager
import ru.kochkaev.zixamc.rest.method.ReceiveFileMethodType
import ru.kochkaev.zixamc.rest.method.RestMapping
import ru.kochkaev.zixamc.rest.method.SendFile
import ru.kochkaev.zixamc.rest.method.SendFileMethodType
import java.io.File
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type

object OpenAPIGenerator {

    private val gson = GsonBuilder()
        .setExclusionStrategies(
            object : ExclusionStrategy {
                override fun shouldSkipField(field: FieldAttributes): Boolean {
                    return field.declaringClass == io.swagger.v3.oas.models.parameters.Parameter::class.java && field.name == "in" || field.name == "Companion"
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

    @Volatile
    private var cachedSpec: OpenAPI? = null

    fun generateSpec(): OpenAPI {
        val openApi = OpenAPI()
            .info(Info().title("ZixaMC REST API").version("1.0"))
            .components(Components())

        val paths = Paths()
        val schemas = mutableMapOf<String, Schema<Any>>()

        openApi.components.addSecuritySchemes("bearerAuth", SecurityScheme()
            .type(SecurityScheme.Type.HTTP)
            .scheme("bearer")
            .bearerFormat("JWT")
//            .name("Authorization")
//            .`in`(SecurityScheme.In.HEADER)
        )

        val classNames = hashMapOf<String, ArrayList<Class<*>>>()
        RestManager.registeredMethods.values.forEach { method ->
            if (method.bodyModel != null && method.bodyModel != Any::class.java && method.bodyModel != Unit::class.java && method.bodyModel != Nothing::class.java && method.bodyModel != File::class.java) {
                val clazz = method.bodyModel
                val className = clazz.simpleName
                val classes = classNames[className]
                if (classes == null) classNames[className] = arrayListOf(clazz)
                else if (!classes.contains(clazz)) classes.add(clazz)
            }
            method.result.results.values.forEach {
                if (it.type != Unit::class.java && it.type != Nothing::class.java && it.type != Any::class.java && method !is SendFileMethodType) {
                    val clazz = resolveClass(it.type)
                    val className = clazz.simpleName
                    val classes = classNames[className]
                    if (classes == null) classNames[className] = arrayListOf(clazz)
                    else if (!classes.contains(clazz)) classes.add(clazz)
                }
            }
        }

        RestManager.registeredMethods.values.forEach { method ->
            val path = "/${method.path}"
            val pathSegments = method.path.split("/").filter { it.isNotBlank() }
            val operation = Operation()
            val extensions = hashMapOf<String, Any>()

            // Description
            val clazz = method::class.java
            val definedDescription = clazz.getAnnotation(RestDescription::class.java)?.value
            val description = (definedDescription?.let { "$it\n\n" } ?: "") + "Required permissions: ${method.requiredPermissions.joinToString(", ")}"
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
                    clazz = method.bodyModel,
                    schemas = schemas,
                    classNamePrefix = if ((classNames[method.bodyModel.simpleName]?.size?:0)>1) "${method.path.replace("/", "$")}$" else ""
                )
                requestBody.addMediaType("application/json", MediaType().schema(ref))
            } else if (method is ReceiveFileMethodType<*>) {
                requestBody.addMediaType("application/octet-stream", MediaType()
                    .schema(StringSchema().format("binary")))
            }
            operation.requestBody(RequestBody().content(requestBody).required(true))

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
                    val ref = getSchemaRefForType(
                        type = result.type,
                        schemas = schemas,
                        classNamePrefix = if ((classNames[resolveClass(result.type).simpleName]?.size
                                ?: 0) > 1
                        ) "${method.path.replace("/", "$")}$" else ""
                    )
                    content.addMediaType("application/json", MediaType().schema(ref))
                }
                responses.addApiResponse(
                    code.toString(), ApiResponse()
                        .description(if (hasResponse || isSendFile || isString) description else "No content")
                        .content(content)
                )
            }
            operation.responses(responses)

            // Security
            if (method.requiredPermissions.isNotEmpty()) {
                operation.addSecurityItem(SecurityRequirement().addList("bearerAuth", emptyList<String>()))
            }
            extensions["x-permissions"] = method.requiredPermissions.toTypedArray()

            // Hidden if @RestHiddenIfNoPerm
            val hiddenAnn = method.javaClass.getAnnotation(RestHiddenIfNoPerm::class.java)
            if (hiddenAnn != null && hiddenAnn.value) {
                extensions["x-hidden"] = true
            }

            // Path Item
            operation.extensions(extensions)
            val pathItem = PathItem()
            when (method.mapping) {
                RestMapping.GET -> pathItem.get(operation)
                RestMapping.POST -> pathItem.post(operation)
                RestMapping.PUT -> pathItem.put(operation)
                RestMapping.DELETE -> pathItem.delete(operation)
            }

            paths.addPathItem(path, pathItem)
        }

        openApi.components.schemas = schemas
        openApi.paths = paths
        return openApi
    }

    fun update() {
        cachedSpec = generateSpec()
    }

    val json: String
        get() = gson.toJson(cachedSpec ?: generateSpec().also { cachedSpec = it })

    private fun getSchemaType(type: Class<*>): String = when (type) {
        String::class.java -> "string"
        Int::class.java, Integer::class.java -> "integer"
        Long::class.java, java.lang.Long::class.java -> "integer"
        Boolean::class.java -> "boolean"
        else -> "object"
    }

    private fun getSchemaRef(clazz: Class<*>, schemas: MutableMap<String, Schema<Any>>, classNamePrefix: String = ""): Schema<Any> {
        val name = classNamePrefix + clazz.simpleName
        if (!schemas.containsKey(name)) {
            val schema = Schema<Any>().type(getSchemaType(clazz))
            schemas[name] = schema
            if (schema.type == "object") clazz.declaredFields.forEach { field ->
                field.isAccessible = true
                if (field.isSynthetic ||
                    field.name == "Companion" ||
                    field.name.endsWith($$"$serializer") ||
                    field.getAnnotation(Transient::class.java) != null
                ) return@forEach
                schema.addProperty(field.name, Schema<Any>()
                    .type(getSchemaType(field.type))
                    .apply {
                        val desc = field.getAnnotation(RestDescription::class.java)?.value
                        if (desc != null) description(desc)
                        val example = field.getAnnotation(RestExample::class.java)?.value
                        if (example != null) example(example)
                    }
                )
            }
        }
        return Schema<Any>().`$ref`("#/components/schemas/$name")
    }

    private fun getSchemaRefForType(type: Type, schemas: MutableMap<String, Schema<Any>>, classNamePrefix: String = ""): Schema<Any> {
        return when (type) {
            is Class<*> -> getSchemaRef(type, schemas, classNamePrefix)
            is ParameterizedType -> {
                val raw = type.rawType as Class<*>
                when (raw) {
                    List::class.java -> ArraySchema().items(getSchemaRefForType(type.actualTypeArguments[0], schemas))
                    Map::class.java -> ObjectSchema().additionalProperties(getSchemaRefForType(type.actualTypeArguments[1], schemas))
                    else -> Schema<Any>().type("object")
                }
            }
            else -> Schema<Any>().type("object")
        }
    }

    private fun resolveClass(type: Type): Class<*> {
        return when (type) {
            is Class<*> -> type
            is ParameterizedType -> type.rawType as Class<*>
            else -> Any::class.java
        }
    }
}