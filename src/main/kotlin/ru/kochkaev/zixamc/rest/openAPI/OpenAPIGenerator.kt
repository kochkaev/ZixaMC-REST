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
import ru.kochkaev.zixamc.rest.RestManager
import ru.kochkaev.zixamc.rest.method.RestMapping
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
            if (method.result.type != Unit::class.java && method.result.type != Nothing::class.java && method.result.type != Any::class.java && method !is SendFileMethodType) {
                val clazz = method.result.typeClass
                val className = clazz.simpleName
                val classes = classNames[className]
                if (classes == null) classNames[className] = arrayListOf(clazz)
                else if (!classes.contains(clazz)) classes.add(clazz)
            }
        }

        RestManager.registeredMethods.values.forEach { method ->
            val path = "/${method.path}"
            val pathSegments = method.path.split("/").filter { it.isNotBlank() }
            val operation = Operation()

            // Description
            val clazz = method::class.java
            val desc = clazz.getAnnotation(RestDescription::class.java)?.value
            if (desc != null) operation.description(desc)

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
            if (method.bodyModel != null && method.bodyModel != Any::class.java && method.bodyModel != Unit::class.java && method.bodyModel != Nothing::class.java) {
                val isFile = method.bodyModel == File::class.java
                val content = Content()
                if (isFile) {
                    content.addMediaType("application/octet-stream", MediaType()
                        .schema(StringSchema().format("binary")))
                } else {
                    val ref = getSchemaRef(
                        clazz = method.bodyModel,
                        schemas = schemas,
                        classNamePrefix = if ((classNames[method.bodyModel.simpleName]?.size?:0)>1) "${method.path.replace("/", "$")}$" else ""
                    )
                    content.addMediaType("application/json", MediaType().schema(ref))
                }
                operation.requestBody(RequestBody().content(content).required(true))
            }

            // Response
            val isSendFile = method is SendFileMethodType
            val content = Content()
            val hasResponse = method.result.type != Unit::class.java && method.result.type != Nothing::class.java && method.result.type != Any::class.java

            if (isSendFile) {
                content.addMediaType("application/octet-stream", MediaType()
                    .schema(StringSchema().format("binary")))
            } else if (hasResponse) {
                val ref = getSchemaRefForType(
                    type = method.result.type,
                    schemas = schemas,
                    classNamePrefix = if ((classNames[method.result.typeClass.simpleName]?.size?:0)>1) "${method.path.replace("/", "$")}$" else ""
                )
                content.addMediaType("application/json", MediaType().schema(ref))
            }

            operation.responses(ApiResponses().addApiResponse("200", ApiResponse()
                .description(if (hasResponse || isSendFile) "Success" else "No content")
                .content(content)
            ))

            // Security
            if (method.requiredPermissions.isNotEmpty()) {
                operation.addSecurityItem(SecurityRequirement().addList("bearerAuth", method.requiredPermissions))
            }

            // Path Item
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
            val schema = ObjectSchema()
            schemas[name] = schema
            clazz.declaredFields.forEach { field ->
                field.isAccessible = true
                if (field.isSynthetic ||
                    field.name == "Companion" ||
                    field.name.endsWith("\$serializer") ||
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
}