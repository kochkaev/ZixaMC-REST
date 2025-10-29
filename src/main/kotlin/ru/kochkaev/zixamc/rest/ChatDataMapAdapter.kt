package ru.kochkaev.zixamc.rest

import com.google.gson.*
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import ru.kochkaev.zixamc.api.sql.chatdata.ChatDataType
import ru.kochkaev.zixamc.api.sql.chatdata.ChatDataTypes

class ChatDataMapAdapter : TypeAdapter<Map<ChatDataType<*>, Any>>() {
    override fun write(
        out: JsonWriter,
        value: Map<ChatDataType<*>, Any>?
    ) {
        if (value == null) {
            out.nullValue()
            return
        }
        out.beginObject()
        value.forEach { (key, v) ->
            out.name(key.serializedName)
            Gson().toJson(v, v::class.java, out)
        }
        out.endObject()
    }

    override fun read(input: JsonReader): Map<ChatDataType<*>, Any> {
        val result = mutableMapOf<ChatDataType<*>, Any>()
        input.beginObject()
        while (input.hasNext()) {
            val keyName = input.nextName()
            val key = ChatDataTypes.entries[keyName] ?: continue
            val valueType = key.model
            val value = Gson().fromJson<Any>(input, valueType)
            result[key] = value
        }
        input.endObject()
        return result
    }
}