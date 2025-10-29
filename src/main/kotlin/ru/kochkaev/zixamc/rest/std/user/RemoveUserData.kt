package ru.kochkaev.zixamc.rest.std.user

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import com.google.gson.annotations.JsonAdapter
import io.ktor.http.HttpStatusCode
import ru.kochkaev.zixamc.api.sql.SQLUser
import ru.kochkaev.zixamc.api.sql.chatdata.ChatDataType
import ru.kochkaev.zixamc.api.sql.chatdata.ChatDataTypes
import ru.kochkaev.zixamc.rest.RestMapping
import ru.kochkaev.zixamc.rest.RestMethodType
import ru.kochkaev.zixamc.rest.std.Permissions
import java.lang.reflect.Type

object RemoveUserData: RestMethodType<RemoveUserData.Request>(
    path = "std/removeUserData",
    requiredPermissions = listOf(Permissions.WRITE_USER_DATA),
    mapping = RestMapping.DELETE,
    params = mapOf(),
    bodyModel = Request::class.java,
    method = { sql, permissions, params, body ->
        if (body == null) {
            HttpStatusCode.BadRequest to "Request body is required"
        } else if (body.key == null) {
            HttpStatusCode.BadRequest to "Invalid key"
        } else {
            val user = SQLUser.get(body.userId)
            if (user == null) {
                HttpStatusCode.NotFound to "User not found: ${body.userId}"
            } else {
                user.data.remove(body.key)
                HttpStatusCode.OK to UserData.get(user.id)
            }
        }
    }
) {
    @JsonAdapter(RequestAdapter::class)
    data class Request(
        val userId: Long,
        val key: ChatDataType<*>?,
    )
    class RequestAdapter: JsonDeserializer<Request>, JsonSerializer<Request> {
        override fun deserialize(
            json: JsonElement,
            typeOfT: Type,
            context: JsonDeserializationContext
        ): Request {
            val jsonObject = json.asJsonObject
            val userId = jsonObject.get("userId").asLong
            val keyRaw = if (jsonObject.has("key")) jsonObject.get("key").asString else null
            val key = ChatDataTypes.entries[keyRaw]
            return Request(userId, key)
        }
        override fun serialize(
            src: Request,
            typeOfSrc: Type,
            context: JsonSerializationContext
        ): JsonElement {
            val jsonObject = JsonObject()
            jsonObject.addProperty("userId", src.userId)
            jsonObject.addProperty("key", src.key?.serializedName)
            return jsonObject
        }

    }
}