package ru.kochkaev.zixamc.rest.method

import com.google.gson.reflect.TypeToken
import io.ktor.http.HttpStatusCode
import java.lang.reflect.Type

class MethodResults<T>(
    val results: Map<HttpStatusCode, MethodResult<*>>,
    val typeOfSuccess: Type,
) {
    companion object {
        inline fun <reified T> create(successCode: HttpStatusCode = HttpStatusCode.OK, vararg results: Pair<HttpStatusCode, MethodResult<*>>): MethodResults<T> {
            val type = object: TypeToken<T>(){}.type
            val resultsMap = hashMapOf<HttpStatusCode, MethodResult<*>>(successCode to MethodResult<T>(type))
            results.forEach { (code, data) ->
//                resultsMap[code] = when (data) {
//                    is MethodResult<*> -> data
//                    else -> MethodResult.of(data)
//                }
                resultsMap[code] = data
            }
            return MethodResults(resultsMap, type)
        }
    }
}
class MethodResult<T>(
    val default: T?,
    val type: Type,
) {
    companion object {
        inline fun <reified T> of(default: T) =
            MethodResult(default, object: TypeToken<T>(){}.type)
    }
    constructor(type: Type): this(null, type)
}

inline fun <reified T> HttpStatusCode.result(result: T): ResultedHttpStatusCode<T> =
    ResultedHttpStatusCode(
        value = value,
        description = description,
        result = result,
        resultType = object: TypeToken<T>(){}.type,
    )
inline fun <reified T> T.methodResult() =
    MethodResult.of<T>(this)

data class ResultedHttpStatusCode<T>(
    val value: Int,
    val description: String,
    val result: T,
    val resultType: Type
): Comparable<HttpStatusCode> {
    override fun toString(): String = "$value $description"
    override fun equals(other: Any?): Boolean =
        other is HttpStatusCode && other.value == value ||
        other is ResultedHttpStatusCode<*> && other.value == value
    override fun hashCode(): Int = value.hashCode()
    override fun compareTo(other: HttpStatusCode): Int = value - other.value
    fun compareTo(other: ResultedHttpStatusCode<*>): Int = value - other.value
    fun toCode() = HttpStatusCode(value, description)
}