package ru.kochkaev.zixamc.rest

import io.ktor.http.HttpStatusCode
import net.fabricmc.loader.api.entrypoint.PreLaunchEntrypoint
import ru.kochkaev.zixamc.api.Initializer
import ru.kochkaev.zixamc.api.config.ConfigManager
import ru.kochkaev.zixamc.api.config.GsonManager
import java.nio.file.Path

class ZixaMCRestPreLaunch : PreLaunchEntrypoint {

    override fun onPreLaunch() {
        GsonManager.registerAdapters(
            SQLClient::class.java to SQLClientAdapter(),
        )
        ConfigManager.registerConfig(Config)
        Initializer.registerSQLTable(SQLClient)
        RestManager.initServer(Config.config.port)
        RestManager.registerMethod(
            object : ReceiveFileMethodType(
                path = "upload/skin",
                requiredPermissions = listOf("admin"),
                mapping = RestMapping.POST,
                savePathSupplier = { sql, _, _, initial ->
                    initial.resolve("ZixaMC-Rest-Uploads/skins/${sql.userId}.png")
                },
                method = { _, _, _, file ->
                    HttpStatusCode.OK to mapOf(
                        "status" to "uploaded",
                        "path" to file?.path,
                        "size" to file?.length()
                    )
                }
            ) {}
        )
    }
}
