package ru.kochkaev.zixamc.rest.std.group

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import com.google.gson.annotations.JsonAdapter
import io.ktor.http.HttpStatusCode
import ru.kochkaev.zixamc.api.sql.SQLGroup
import ru.kochkaev.zixamc.api.sql.feature.FeatureType
import ru.kochkaev.zixamc.api.sql.feature.FeatureTypes
import ru.kochkaev.zixamc.api.sql.feature.data.FeatureData
import ru.kochkaev.zixamc.rest.method.MethodResult
import ru.kochkaev.zixamc.rest.method.MethodResults
import ru.kochkaev.zixamc.rest.method.RestMapping
import ru.kochkaev.zixamc.rest.method.RestMethodType
import ru.kochkaev.zixamc.rest.method.methodResult
import ru.kochkaev.zixamc.rest.method.result
import ru.kochkaev.zixamc.rest.openAPI.RestDescription
import ru.kochkaev.zixamc.rest.std.Permissions
import java.lang.reflect.Type

@RestDescription("Disable and remove a feature from a group")
object RemoveGroupFeature: RestMethodType<RemoveGroupFeature.Request, GroupData>(
    path = "std/removeGroupFeature",
    requiredPermissions = listOf(Permissions.WRITE_GROUP_FEATURES),
    mapping = RestMapping.DELETE,
    params = mapOf(),
    bodyModel = Request::class.java,
    result = MethodResults.create(HttpStatusCode.OK,
        HttpStatusCode.BadRequest to "Request body is empty or key is invalid".methodResult(),
        HttpStatusCode.NotFound to "Group not found".methodResult(),
    ),
    method = { sql, permissions, params, body ->
        if (body == null) {
            HttpStatusCode.BadRequest.result("Request body is required")
        } else if (body.key == null) {
            HttpStatusCode.BadRequest.result("Invalid key")
        } else {
            val group = SQLGroup.get(body.chatId)
            if (group == null) {
                HttpStatusCode.NotFound.result("Group not found: ${body.chatId}")
            } else {
                group.features.remove(body.key)
                HttpStatusCode.OK.result(GroupData.get(group.id))
            }
        }
    }
) {
    @JsonAdapter(RequestAdapter::class)
    data class Request(
        val chatId: Long,
        val key: FeatureType<out FeatureData>?,
    )
    class RequestAdapter: JsonDeserializer<Request>, JsonSerializer<Request> {
        override fun deserialize(
            json: JsonElement,
            typeOfT: Type,
            context: JsonDeserializationContext
        ): Request {
            val jsonObject = json.asJsonObject
            val chatId = jsonObject.get("chatId").asLong
            val keyRaw = if (jsonObject.has("key")) jsonObject.get("key").asString else null
            val key = FeatureTypes.entries[keyRaw]
            return Request(chatId, key)
        }
        override fun serialize(
            src: Request,
            typeOfSrc: Type,
            context: JsonSerializationContext
        ): JsonElement {
            val jsonObject = JsonObject()
            jsonObject.addProperty("chatId", src.chatId)
            jsonObject.addProperty("key", src.key?.serializedName)
            return jsonObject
        }

    }
}