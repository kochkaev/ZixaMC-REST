package ru.kochkaev.zixamc.rest

import com.google.gson.reflect.TypeToken
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.fabricmc.loader.api.entrypoint.PreLaunchEntrypoint
import ru.kochkaev.zixamc.api.Initializer
import ru.kochkaev.zixamc.api.config.ConfigManager
import ru.kochkaev.zixamc.api.config.GsonManager
import ru.kochkaev.zixamc.api.sql.MySQL
import ru.kochkaev.zixamc.api.sql.SQLChat
import ru.kochkaev.zixamc.api.sql.SQLGroup
import ru.kochkaev.zixamc.api.sql.SQLUser
import ru.kochkaev.zixamc.api.sql.chatdata.ChatDataType
import ru.kochkaev.zixamc.api.sql.chatdata.ChatDataTypes
import ru.kochkaev.zixamc.api.sql.data.AccountType
import ru.kochkaev.zixamc.api.sql.data.MinecraftAccountData
import ru.kochkaev.zixamc.api.sql.data.MinecraftAccountType
import ru.kochkaev.zixamc.api.sql.feature.FeatureType
import ru.kochkaev.zixamc.api.sql.feature.FeatureTypes
import ru.kochkaev.zixamc.api.sql.feature.data.FeatureData
import ru.kochkaev.zixamc.api.sql.feature.data.PlayersGroupFeatureData
import ru.kochkaev.zixamc.api.sql.util.AbstractSQLArray
import ru.kochkaev.zixamc.api.sql.util.AbstractSQLField
import ru.kochkaev.zixamc.api.sql.util.AbstractSQLMap
import ru.kochkaev.zixamc.rest.openAPI.FieldOverride
import ru.kochkaev.zixamc.rest.openAPI.OpenAPIGenerator
import ru.kochkaev.zixamc.rest.openAPI.SchemaOverride
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
        ServerLifecycleEvents.SERVER_STARTED.register { server ->
            Initializer.registerSQLTable(SQLClient)
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
                // TODO: access to telegram bot api as server bot, callback and processes tables
                // TODO: interact with Minecraft
            )
            OpenAPIGenerator.overrideSchemas(
                UserData::class.java to SchemaOverride(
                    instance = UserData(
                        userId = 1381684202,
                        nickname = "kleverdi",
                        nicknames = listOf("kleverdi"),
                        accountType = AccountType.ADMIN,
                        agreedWithRules = true,
                        isRestricted = false,
                        tempArray = listOf(),
                        data = hashMapOf(ChatDataTypes.MINECRAFT_ACCOUNTS to arrayListOf(MinecraftAccountData("kleverdi", MinecraftAccountType.PLAYER))),
                    ),
                ),
                GroupData::class.java to SchemaOverride(
                    instance = GroupData(
                        chatId = -1002186004415,
                        name = "zixa",
                        aliases = listOf("main"),
                        members = listOf(1381684202),
                        agreedWithRules = true,
                        isRestricted = false,
                        // I didn't know WTF it's not works...
//                        features = hashMapOf(FeatureTypes.PLAYERS_GROUP to PlayersGroupFeatureData()),
                        data = hashMapOf(ChatDataTypes.MINECRAFT_ACCOUNTS to arrayListOf(MinecraftAccountData("kleverdi", MinecraftAccountType.PLAYER))),
                    ),
                ),
                SQLChat::class.java to SchemaOverride(
                    exclude = true,
                    ifIsAssignable = true,
                ),
                AbstractSQLMap::class.java to SchemaOverride(
                    exclude = true,
                    ifIsAssignable = true,
                ),
                AbstractSQLArray::class.java to SchemaOverride(
                    exclude = true,
                    ifIsAssignable = true,
                ),
                AbstractSQLField::class.java to SchemaOverride(
                    exclude = true,
                    ifIsAssignable = true,
                ),
                FeatureType::class.java to SchemaOverride(
                    instance = "PLAYERS_GROUP",
                    schemaType = "string",
                    ifIsAssignable = true,
                ),
                object: TypeToken<Map<FeatureType<out FeatureData>, FeatureData>>(){}.type to SchemaOverride(
                    mapKey = FieldOverride(
                        example = "PLAYERS_GROUP",
                    ),
                ),
                object: TypeToken<Map<ChatDataType<*>, *>>(){}.type to SchemaOverride(
                    mapKey = FieldOverride(
                        example = "minecraft_accounts",
                    ),
                    listOrMapValue = FieldOverride(
                        example = arrayListOf(MinecraftAccountData("kleverdi", MinecraftAccountType.PLAYER)),
                        type = Any::class.java,
                    )
                ),
                ChatDataType::class.java to SchemaOverride(
                    instance = "minecraft_accounts",
                    schemaType = "string",
                    ifIsAssignable = true,
                ),
                updateOpenApi = false,
            )
            RestManager.initServer(Config.config.port)
        }

    }
}
