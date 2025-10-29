package ru.kochkaev.zixamc.rest.method

import com.google.gson.reflect.TypeToken
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import java.lang.reflect.Type

open class MethodResult<T> (
    val typeClass: Class<T>,
    val type: Type = object: TypeToken<T>(){}.type,
) {
    companion object {
        inline fun <reified T> create() : MethodResult<T> {
            return MethodResult( T::class.java, object: TypeToken<T>(){}.type)
        }
    }

    @Suppress("UNCHECKED_CAST")
    open class Result<T> internal constructor(
        val status: HttpStatusCode,
        val result: Any?,
        val schema: MethodResult<T>,
    ) {
        fun isSuccess(): Boolean {
            return status.isSuccess() && result as? T != null
        }
        fun getResultCasted(): T {
            if (!isSuccess()) {
                throw IllegalStateException("Cannot get result from unsuccessful MethodResult.Result: code=$status, result=$result")
            }
            return result as T
        }
    }

    fun write(code: HttpStatusCode, result: Any?): Result<T> {
        return Result(code, result, this)
    }
}