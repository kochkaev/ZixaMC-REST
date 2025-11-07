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
import ru.kochkaev.zixamc.rest.method.MethodResult
import ru.kochkaev.zixamc.rest.method.MethodResults
import ru.kochkaev.zixamc.rest.method.RestMapping
import ru.kochkaev.zixamc.rest.method.RestMethodType
import ru.kochkaev.zixamc.rest.method.methodResult
import ru.kochkaev.zixamc.rest.method.result
import ru.kochkaev.zixamc.rest.openAPI.RestDescription
import ru.kochkaev.zixamc.rest.std.Permissions
import java.lang.reflect.Type

@RestDescription("Set or update custom data with a specified key for a user")
object SetUserData: RestMethodType<SetUserData.Request, UserData>(
    path = "std/setUserData",
    requiredPermissions = listOf(Permissions.WRITE_USER_DATA),
    mapping = RestMapping.PUT,
    params = mapOf(),
    bodyModel = Request::class.java,
    result = MethodResults.create(HttpStatusCode.OK,
        HttpStatusCode.BadRequest to "Request body is empty or provided key is invalid".methodResult(),
        HttpStatusCode.NotFound to "User not found".methodResult(),
    ),
    method = { sql, permissions, params, body ->
        if (body == null) {
            HttpStatusCode.BadRequest.result("Request body is required")
        } else if (body.key == null) {
            HttpStatusCode.BadRequest.result("Invalid key")
        } else {
            val user = SQLUser.get(body.userId)
            if (user == null) {
                HttpStatusCode.NotFound.result("User not found: ${body.userId}")
            } else {
                user.data.set(body.key, body.data)
                HttpStatusCode.OK.result(UserData.get(user.id))
            }
        }
    }
) {
    @JsonAdapter(RequestAdapter::class)
    data class Request(
        val userId: Long,
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
            val userId = jsonObject.get("userId").asLong
            val keyRaw = if (jsonObject.has("key")) jsonObject.get("key").asString else null
            val key = ChatDataTypes.entries[keyRaw]
            val data = key?.model?.let { context.deserialize(
                jsonObject.get("data"),
                it,
            ) } ?: Any()
            return Request(userId, key, data)
        }
        override fun serialize(
            src: Request,
            typeOfSrc: Type,
            context: JsonSerializationContext
        ): JsonElement {
            val jsonObject = JsonObject()
            jsonObject.addProperty("userId", src.userId)
            jsonObject.addProperty("key", src.key?.serializedName)
            jsonObject.add("data", context.serialize(src.data))
            return jsonObject
        }

    }
}