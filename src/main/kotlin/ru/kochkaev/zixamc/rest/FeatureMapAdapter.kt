package ru.kochkaev.zixamc.rest

import com.google.gson.*
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import ru.kochkaev.zixamc.api.sql.chatdata.ChatDataType
import ru.kochkaev.zixamc.api.sql.chatdata.ChatDataTypes
import ru.kochkaev.zixamc.api.sql.feature.FeatureType
import ru.kochkaev.zixamc.api.sql.feature.FeatureTypes
import ru.kochkaev.zixamc.api.sql.feature.data.FeatureData

class FeatureMapAdapter : TypeAdapter<Map<FeatureType<out FeatureData>, FeatureData>>() {
    override fun write(
        out: JsonWriter,
        value: Map<FeatureType<out FeatureData>, FeatureData>?
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

    override fun read(input: JsonReader): Map<FeatureType<out FeatureData>, FeatureData> {
        val result = mutableMapOf<FeatureType<out FeatureData>, FeatureData>()
        input.beginObject()
        while (input.hasNext()) {
            val keyName = input.nextName()
            val key = FeatureTypes.entries[keyName] ?: continue
            val valueType = key.model
            val value = Gson().fromJson<FeatureData>(input, valueType)
            result[key] = value
        }
        input.endObject()
        return result
    }
}