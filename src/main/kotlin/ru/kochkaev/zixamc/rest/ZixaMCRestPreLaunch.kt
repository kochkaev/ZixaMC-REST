package ru.kochkaev.zixamc.rest

import net.fabricmc.loader.api.entrypoint.PreLaunchEntrypoint
import ru.kochkaev.zixamc.api.Initializer
import ru.kochkaev.zixamc.api.config.ConfigManager
import ru.kochkaev.zixamc.api.config.GsonManager
import ru.kochkaev.zixamc.rest.std.*
import ru.kochkaev.zixamc.rest.std.group.*
import ru.kochkaev.zixamc.rest.std.user.*

class ZixaMCRestPreLaunch : PreLaunchEntrypoint {

    override fun onPreLaunch() {
        GsonManager.registerAdapters(
            SQLClient::class.java to SQLClientAdapter(),
            SetUserData.Request::class.java to SetUserData.RequestAdapter(),
            RemoveUserData.Request::class.java to RemoveUserData.RequestAdapter(),
            SetGroupData.Request::class.java to SetGroupData.RequestAdapter(),
            RemoveGroupData.Request::class.java to RemoveGroupData.RequestAdapter(),
            SetGroupFeature.Request::class.java to SetGroupFeature.RequestAdapter(),
            RemoveGroupFeature.Request::class.java to RemoveGroupFeature.RequestAdapter(),
        )
        ConfigManager.registerConfig(Config)
        Initializer.registerSQLTable(SQLClient)
        RestManager.initServer(Config.config.port)
        RestManager.registerMethods(
            GetMe,
            // File IO
            DownloadFile, UploadFile,
            // Users
            GetUser, GetAllUsers, UpdateUser, CreateUser, DeleteUser,
            SetUserNickname, AddUserNickname, RemoveUserNickname, SetUserAccountType, SetUserAgreedWithRules,
            SetUserRestricted, AddUserTempArray, RemoveUserTempArray, SetUserData, RemoveUserData,
            // Groups
            GetGroup, GetAllGroups, UpdateGroup, CreateGroup, DeleteGroup,
            SetGroupName, AddGroupAlias, RemoveGroupAlias, SetGroupAgreedWithRules, SetGroupRestricted, AddGroupMember,
            RemoveGroupMember, SetGroupFeature, RemoveGroupFeature, SetGroupData, RemoveGroupData,
        )
    }
}
