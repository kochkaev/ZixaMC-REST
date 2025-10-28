package ru.kochkaev.zixamc.rest

import io.ktor.http.HttpStatusCode
import net.fabricmc.loader.api.entrypoint.PreLaunchEntrypoint
import ru.kochkaev.zixamc.api.Initializer
import ru.kochkaev.zixamc.api.config.ConfigManager
import ru.kochkaev.zixamc.api.config.GsonManager

class ZixaMCRestPreLaunch : PreLaunchEntrypoint {

    override fun onPreLaunch() {
        GsonManager.registerAdapters(
            SQLClient::class.java to SQLClientAdapter(),
        )
        ConfigManager.registerConfig(Config)
        Initializer.registerSQLTable(SQLClient)
        RestManager.initServer(Config.config.port)
        RestManager.registerMethod(object: RestMethodType<Any?>(
            path = "test",
            requiredPermissions = listOf(),
            mapping = RestMapping.GET,
            bodyModel = null,
            method = { _, _, _, _ ->
                Pair(HttpStatusCode.OK, mapOf("status" to "ok"))
            }
        ){})
    }
}
