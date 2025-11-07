package ru.kochkaev.zixamc.rest

import com.google.gson.reflect.TypeToken
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.fabricmc.loader.api.entrypoint.PreLaunchEntrypoint
import ru.kochkaev.zixamc.api.Initializer
import ru.kochkaev.zixamc.api.config.ConfigManager
import ru.kochkaev.zixamc.api.config.GsonManager
import ru.kochkaev.zixamc.api.config.serialize.ChatDataTypeAdapter
import ru.kochkaev.zixamc.api.config.serialize.FeatureTypeAdapter
import ru.kochkaev.zixamc.api.sql.SQLChat
import ru.kochkaev.zixamc.api.sql.chatdata.ChatDataType
import ru.kochkaev.zixamc.api.sql.chatdata.ChatDataTypes
import ru.kochkaev.zixamc.api.sql.data.AccountType
import ru.kochkaev.zixamc.api.sql.data.MinecraftAccountData
import ru.kochkaev.zixamc.api.sql.data.MinecraftAccountType
import ru.kochkaev.zixamc.api.sql.data.NewProtectedData
import ru.kochkaev.zixamc.api.sql.feature.FeatureType
import ru.kochkaev.zixamc.api.sql.feature.FeatureTypes
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
                    description = "User information from the SQL database",
                    fields = mapOf(
                        "userId" to FieldOverride(
                            instance = 1281684202,
                            description = "Telegram user id"
                        ),
                        "nickname" to FieldOverride(
                            instance = "kleverdi",
                            description = "User's primary minecraft nickname"
                        ),
                        "nicknames" to FieldOverride(
                            instance = listOf("kleverdi"),
                            description = "User's minecraft nicknames (contains primary nickname)"
                        ),
                        "accountType" to FieldOverride(
                            instance = AccountType.ADMIN,
                            description = "User's account type (role on server)"
                        ),
                        "agreedWithRules" to FieldOverride(
                            instance = true,
                            description = "Is user agreed with server rules"
                        ),
                        "isRestricted" to FieldOverride(
                            instance = false,
                            description = "If true, user can't interact with server bots"
                        ),
                        "tempArray" to FieldOverride(
                            instance = listOf<Any>(),
                            description = "List with temporary info (like message ids to reply while request is pending)"
                        ),
                        "data" to FieldOverride(
                            instance = hashMapOf(ChatDataTypes.GREETING_ENABLE to Any()),
                            example = hashMapOf(
                                ChatDataTypes.MINECRAFT_ACCOUNTS to arrayListOf(
                                    MinecraftAccountData(
                                        nickname = "kleverdi",
                                        accountStatus = MinecraftAccountType.PLAYER,
                                    ),
                                ),
                            ),
                            description = "Other user data, declared by additional ZixaMC API modules"
                        ),
                    ),
                ),
                GroupData::class.java to SchemaOverride(
                    description = "Group information from the SQL database",
                    fields = mapOf(
                        "chatId" to FieldOverride(
                            instance = -1002186004415,
                            description = "Telegram chat id of group (always negative in Telegram Bot API, starts with -100 if supergroup, else just -)"
                        ),
                        "name" to FieldOverride(
                            instance = "zixa",
                            description = "Group primary name"
                        ),
                        "aliases" to FieldOverride(
                            instance = listOf("main"),
                            description = "Group name aliases (primary name is excluded from this list)"
                        ),
                        "members" to FieldOverride(
                            instance = listOf(1381684202),
                            description = "List of group members (bots and non players is included), contains their telegram user ids"
                        ),
                        "agreedWithRules" to FieldOverride(
                            instance = true,
                            description = "Is group administrator agreed with server rules"
                        ),
                        "isRestricted" to FieldOverride(
                            instance = false,
                            description = "If true, players can't add server bots to this group"
                        ),
                        "features" to FieldOverride(
                            instance = hashMapOf(FeatureTypes.PLAYERS_GROUP to PlayersGroupFeatureData()),
                            description = "Contains features (like chat synchronization) that enabled in this group and it's settings"
                        ),
                        "data" to FieldOverride(
                            instance = hashMapOf(ChatDataTypes.GREETING_ENABLE to Any()),
                            example = hashMapOf(
                                ChatDataTypes.GREETING_ENABLE to true,
                                ChatDataTypes.PROTECTED to hashMapOf(
                                    AccountType.PLAYER to listOf(NewProtectedData(
                                        messageId = 733,
                                        protectedType = NewProtectedData.ProtectedType.TEXT,
                                        senderBotId = 7630523429,
                                    )),
                                ),
                            ),
                            description = "Other group data, declared by additional ZixaMC API modules"
                        ),
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
                    type = String::class.java,
                    name = FeatureType::class.java.name,
                    simpleName = "FeatureType<? extends FeatureData>",
                    global = true,
                    typeAdapter = FeatureTypeAdapter(),
                    ifIsAssignable = true,
                ),
                ChatDataType::class.java to SchemaOverride(
                    instance = "minecraft_accounts",
                    type = String::class.java,
                    name = ChatDataType::class.java.name,
                    simpleName = "ChatDataType<?>",
                    global = true,
                    typeAdapter = ChatDataTypeAdapter(),
                    ifIsAssignable = true,
                ),
                object: TypeToken<Map<ChatDataType<*>, Any>>(){}.type to SchemaOverride(
                    instance = hashMapOf(
                        ChatDataTypes.PROTECTED to hashMapOf(
                            AccountType.PLAYER to listOf(NewProtectedData(
                                messageId = 733,
                                protectedType = NewProtectedData.ProtectedType.TEXT,
                                senderBotId = 7630523429,
                            )),
                        ),
                        ChatDataTypes.MINECRAFT_ACCOUNTS to arrayListOf(
                            MinecraftAccountData(
                                nickname = "kleverdi",
                                accountStatus = MinecraftAccountType.PLAYER,
                            ),
                        ),
                    ),
                    description = "Other data, declared by additional ZixaMC API modules, both for groups and users",
                    listOrMapValue = FieldOverride(
                        instance = Any(),
                        example = listOf(
                            hashMapOf(
                                AccountType.PLAYER to listOf(NewProtectedData(
                                    messageId = 733,
                                    protectedType = NewProtectedData.ProtectedType.TEXT,
                                    senderBotId = 7630523429,
                                )),
                            ),
                        ),
                    ),
                    ifIsAssignable = true,
                ),
                Any::class.java to SchemaOverride(
                    excludeFromGlobalSchemas = true,
                ),
                updateOpenApi = false,
            )
            RestManager.initServer(Config.config.port)
        }

    }
}
