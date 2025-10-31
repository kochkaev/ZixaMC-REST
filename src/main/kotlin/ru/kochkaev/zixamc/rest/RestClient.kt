package ru.kochkaev.zixamc.rest

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import ru.kochkaev.zixamc.api.config.GsonManager.gson
import ru.kochkaev.zixamc.rest.method.RestMapping
import ru.kochkaev.zixamc.rest.method.RestMethodType
import ru.kochkaev.zixamc.rest.method.SendFile
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.TimeUnit
import kotlin.io.path.createParentDirectories

class RestClient(
    val token: String,
    val host: String = "localhost:${Config.config.port}",
) {
    val isClosed: Boolean
        get() = client.dispatcher.executorService.isShutdown

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .addHeader("Authorization", "Bearer $token")
                .build()
            chain.proceed(request)
        }
        .build()

    private val baseUrl get() = "http://$host/api"

    fun <T, R> send(
        method: RestMethodType<T, R>,
        body: T? = null,
        params: Map<String, Any> = emptyMap(),
        toPath: Path? = null,
    ): R = runBlocking {
        sendAsync(method, body, params, toPath)
    }

    suspend fun <T, R> sendAsync(
        method: RestMethodType<T, R>,
        body: T?,
        params: Map<String, Any>,
        toPath: Path? = null,
    ): R = withContext(Dispatchers.IO) {
        val url = buildUrl(method.path, params)
        val request = when (method.mapping) {
            RestMapping.GET -> Request.Builder().url(url).get()
            RestMapping.POST -> Request.Builder().url(url).post(createBody(body))
            RestMapping.PUT -> Request.Builder().url(url).put(createBody(body))
            RestMapping.DELETE -> Request.Builder().url(url).delete(createBody(body))
        }.build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw RestException(response.code, response.body?.string() ?: "Unknown error")
            }
            val responseBody = response.body
            @Suppress("UNCHECKED_CAST")
            when {
                responseBody != null && toPath != null -> {
                    if (method.result.typeClass != SendFile::class.java) {
                        throw IllegalArgumentException("toPath can only be used with File result type")
                    }
                    val file = toPath.let {
                        it.createParentDirectories()
                        Files.newOutputStream(it).use { output ->
                            responseBody.byteStream().copyTo(output)
                        }
                        it.toFile()
                    }
                    file as R
                }
                method.result.typeClass == Unit::class.java -> {
                    Unit as R
                }
                else -> {
                    val responseBodyString = responseBody?.string()?:""
                    if (responseBodyString.isBlank()) {
                        if (method.result.typeClass == String::class.java) {
                            "" as R
                        } else {
                            throw RestException(204, "No content")
                        }
                    } else gson.fromJson(responseBodyString, method.result.type) as R
                }
            }
        }
    }

    private fun createBody(body: Any?): RequestBody = when (body) {
        null -> "".toRequestBody(null)
        is File -> body.asRequestBody("application/octet-stream".toMediaType())
        else -> gson.toJson(body).toRequestBody("application/json".toMediaType())
    }

    private fun buildUrl(path: String, params: Map<String, Any>): String {
        val url = "$baseUrl/$path".toHttpUrlOrNull()!!.newBuilder()
        params.forEach { (k, v) -> url.addQueryParameter(k, v.toString()) }
        return url.build().toString()
    }

    fun close() = client.dispatcher.executorService.shutdown()
}