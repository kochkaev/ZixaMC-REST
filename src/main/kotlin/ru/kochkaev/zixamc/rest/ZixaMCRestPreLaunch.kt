package ru.kochkaev.zixamc.rest

import net.fabricmc.loader.api.entrypoint.PreLaunchEntrypoint
import ru.kochkaev.zixamc.api.Initializer
import ru.kochkaev.zixamc.api.config.ConfigManager
import ru.kochkaev.zixamc.api.config.GsonManager
import ru.kochkaev.zixamc.rest.std.*

class ZixaMCRestPreLaunch : PreLaunchEntrypoint {

    override fun onPreLaunch() {
        GsonManager.registerAdapters(
            SQLClient::class.java to SQLClientAdapter(),
        )
        ConfigManager.registerConfig(Config)
        Initializer.registerSQLTable(SQLClient)
        RestManager.initServer(Config.config.port)
        RestManager.registerMethods(
            GetMe,
            DownloadFile, UploadFile,
            GetUser, GetAllUsers, UpdateUser, CreateUser, DeleteUser,
            GetGroup, GetAllGroups, UpdateGroup, CreateGroup, DeleteGroup,
        )
    }
}
