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
import ru.kochkaev.zixamc.api.sql.chatdata.ChatDataType
import ru.kochkaev.zixamc.api.sql.chatdata.ChatDataTypes
import ru.kochkaev.zixamc.rest.RestMapping
import ru.kochkaev.zixamc.rest.RestMethodType
import ru.kochkaev.zixamc.rest.std.Permissions
import ru.kochkaev.zixamc.rest.std.user.UserData
import java.lang.reflect.Type

object SetGroupData: RestMethodType<SetGroupData.Request>(
    path = "std/setGroupData",
    requiredPermissions = listOf(Permissions.WRITE_GROUP_DATA),
    mapping = RestMapping.PUT,
    params = mapOf(),
    bodyModel = Request::class.java,
    method = { sql, permissions, params, body ->
        if (body == null) {
            HttpStatusCode.BadRequest to "Request body is required"
        } else if (body.key == null) {
            HttpStatusCode.BadRequest to "Invalid key"
        } else {
            val group = SQLGroup.get(body.chatId)
            if (group == null) {
                HttpStatusCode.NotFound to "Group not found: ${body.chatId}"
            } else {
                group.data.set(body.key, body.data)
                HttpStatusCode.OK to GroupData.get(group.id)
            }
        }
    }
) {
    @JsonAdapter(RequestAdapter::class)
    data class Request(
        val chatId: Long,
        val key: ChatDataType<*>?,
        val data: Any,
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
            val key = ChatDataTypes.entries[keyRaw]
            val data = key?.model?.let { context.deserialize(
                jsonObject.get("data"),
                it,
            ) } ?: Any()
            return Request(chatId, key, data)
        }
        override fun serialize(
            src: Request,
            typeOfSrc: Type,
            context: JsonSerializationContext
        ): JsonElement {
            val jsonObject = JsonObject()
            jsonObject.addProperty("chatId", src.chatId)
            jsonObject.addProperty("key", src.key?.serializedName)
            jsonObject.add("data", context.serialize(src.data))
            return jsonObject
        }

    }
}