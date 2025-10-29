package ru.kochkaev.zixamc.rest

import io.ktor.http.HttpStatusCode
import io.ktor.serialization.gson.gson
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.Principal
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.bearer
import io.ktor.server.auth.principal
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.request.receiveChannel
import io.ktor.server.request.receiveText
import io.ktor.server.response.respond
import io.ktor.server.response.respondFile
import io.ktor.server.response.respondText
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.routing
import io.ktor.utils.io.jvm.javaio.copyTo
import ru.kochkaev.zixamc.api.config.GsonManager.gson
import java.util.UUID
import kotlin.io.path.createFile
import kotlin.io.path.createParentDirectories
import kotlin.io.path.deleteIfExists
import kotlin.io.path.exists
import kotlin.io.path.outputStream

object RestManager {
    private val methods = mutableMapOf<String, RestMethodType<*>>()
    private lateinit var app: Application
    var initialized = false
        private set

    fun registerMethod(methodType: RestMethodType<*>) {
        methods[methodType.path] = methodType
        if (initialized) routeMethod(methodType)
    }

    private fun routeMethod(methodType: RestMethodType<*>) {
        app.routing {
            authenticate("tokenAuth") {
                when (methodType.mapping) {
                    RestMapping.GET -> get("/api/${methodType.path}") { handleMethod(call, methodType) }
                    RestMapping.POST -> post("/api/${methodType.path}") { handleMethod(call, methodType) }
                    RestMapping.PUT -> put("/api/${methodType.path}") { handleMethod(call, methodType) }
                    RestMapping.DELETE -> delete("/api/${methodType.path}") { handleMethod(call, methodType) }
                }
            }
        }
    }

    suspend fun handleMethod(call: ApplicationCall, methodType: RestMethodType<*>) = with(call) {
//        val token = UUID.fromString(parameters["token"]!!)
        val principal = principal<RestPrincipal>()!!
        if (!principal.permissions.containsAll(methodType.requiredPermissions)) {
            respond(HttpStatusCode.Forbidden)
            return
        }
        val params = methodType.params.mapValues { (key, pair) ->
            parameters[key]?.let { value -> gson.fromJson(value, pair.first) } ?: if (pair.second) {
                respond(HttpStatusCode.BadRequest)
                return
            } else null
        }
        val body = when(methodType.bodyModel) {
            null -> {
                if (methodType is ReceiveFileMethodType) {
                    val file = methodType.getPath(
                        sql = principal.sql,
                        permissions = principal.permissions,
                        params = params
                    ).let {
                        it.createParentDirectories()
                        it.deleteIfExists()
                        it.createFile()
                        it.toFile()
                    }
                    file.outputStream().use { output ->
                        receiveChannel().copyTo(output)
                    }
                    file
                } else null
            }
            else -> {
                try {
                    gson.fromJson(receiveText(), methodType.bodyModel)
                } catch (_: Exception) {
                    respond(HttpStatusCode.BadRequest)
                    return
                }
            }
        }
        val (code, responseBody) = methodType.invoke(
            principal.sql, principal.permissions, params, body
        )
        when (responseBody) {
            is String -> respondText(responseBody, status = code)
            is SendFile -> respondFile(responseBody.file, responseBody.configure)
            null -> respond(code)
            else -> respond(status = code, message = responseBody)
        }
    }

    fun initServer(configPort: Int) {
        embeddedServer(Netty, port = configPort) {
            app = this
            install(Authentication) {
                bearer("tokenAuth") {
                    authenticate { tokenCredential ->
                        val sql = SQLClient.get(UUID.fromString(tokenCredential.token))
                        if (sql != null) RestPrincipal(sql, sql.permissions.get()?:listOf())
                        else null
                    }
                }
            }
            install(ContentNegotiation) { gson() }
            methods.values.forEach { routeMethod(it) }
            initialized = true
        }.start(wait = false)
    }
    data class RestPrincipal(val sql: SQLClient, val permissions: List<String>) : Principal
}