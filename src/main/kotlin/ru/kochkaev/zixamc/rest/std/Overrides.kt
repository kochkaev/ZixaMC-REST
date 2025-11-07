package ru.kochkaev.zixamc.rest.std

import com.google.gson.reflect.TypeToken
import ru.kochkaev.zixamc.api.config.serialize.ChatDataTypeAdapter
import ru.kochkaev.zixamc.api.config.serialize.FeatureTypeAdapter
import ru.kochkaev.zixamc.rest.std.group.*
import ru.kochkaev.zixamc.rest.std.user.*
import ru.kochkaev.zixamc.api.sql.SQLChat
import ru.kochkaev.zixamc.api.sql.chatdata.ChatDataType
import ru.kochkaev.zixamc.api.sql.chatdata.ChatDataTypes
import ru.kochkaev.zixamc.api.sql.data.AccountType
import ru.kochkaev.zixamc.api.sql.data.MinecraftAccountData
import ru.kochkaev.zixamc.api.sql.data.MinecraftAccountType
import ru.kochkaev.zixamc.api.sql.data.NewProtectedData
import ru.kochkaev.zixamc.api.sql.feature.FeatureType
import ru.kochkaev.zixamc.api.sql.feature.FeatureTypes
import ru.kochkaev.zixamc.api.sql.feature.data.FeatureData
import ru.kochkaev.zixamc.api.sql.feature.data.PlayersGroupFeatureData
import ru.kochkaev.zixamc.api.sql.util.AbstractSQLArray
import ru.kochkaev.zixamc.api.sql.util.AbstractSQLField
import ru.kochkaev.zixamc.api.sql.util.AbstractSQLMap
import ru.kochkaev.zixamc.rest.openAPI.FieldOverride
import ru.kochkaev.zixamc.rest.openAPI.SchemaOverride
import ru.kochkaev.zixamc.rest.std.group.GroupData
import ru.kochkaev.zixamc.rest.std.user.UserData
import java.lang.reflect.Type

object Overrides: HashMap<Type, SchemaOverride>(hashMapOf(
    GetMe.MeInfo::class.java to SchemaOverride(
        description = "Current user information and permissions",
        fields = mapOf(
            "userId" to FieldOverride(
                instance = 1281684202,
                description = "Telegram user ID of the authenticated user"
            ),
            "mark" to FieldOverride(
                instance = "admin",
                description = "Token special mark or role indicator"
            ),
            "permissions" to FieldOverride(
                instance = listOf(Permissions.READ_USER, Permissions.WRITE_USER_NICKNAMES, Permissions.READ_ALL_USERS),
                description = "List of permission keys granted to the user"
            )
        )
    ),
    DownloadFile.Request::class.java to SchemaOverride(
        description = "File download request parameters",
        fields = mapOf(
            "filePath" to FieldOverride(
                instance = "./fabrictailor_uploads/kleverdi.png",
                description = "Server path (from game dir) to the file that needs to be downloaded"
            )
        )
    ),
    UploadFile.Answer::class.java to SchemaOverride(
        description = "File upload operation result",
        fields = mapOf(
            "filePath" to FieldOverride(
                instance = "./fabrictailor_uploads/kleverdi.png",
                description = "Server path where the file was saved"
            ),
            "fileSize" to FieldOverride(
                instance = 889,
                description = "Uploaded file length in bytes"
            )
        )
    ),
    SetGroupFeature.Request::class.java to SchemaOverride(
        description = "Group feature configuration",
        fields = mapOf(
            "chatId" to FieldOverride(
                instance = -1002186004415,
                description = "Telegram chat ID of the target group"
            ),
            "feature" to FieldOverride(
                instance = FeatureTypes.PLAYERS_GROUP,
                description = "Feature identifier to be modified"
            ),
            "data" to FieldOverride(
                instance = PlayersGroupFeatureData(),
                description = "New data of the feature"
            )
        )
    ),
    SetGroupName.Request::class.java to SchemaOverride(
        description = "Update group primary name request",
        fields = mapOf(
            "chatId" to FieldOverride(
                instance = -1002186004415,
                description = "Telegram chat ID of the target group"
            ),
            "name" to FieldOverride(
                instance = "newname",
                description = "New primary name for the group"
            )
        )
    ),
    SetGroupRestricted.Request::class.java to SchemaOverride(
        description = "Update group restriction status",
        fields = mapOf(
            "chatId" to FieldOverride(
                instance = -1002186004415,
                description = "Telegram chat ID of the target group"
            ),
            "isRestricted" to FieldOverride(
                instance = false,
                description = "New restriction status for the group. If true, players won't be able to add server bots to this group"
            )
        )
    ),
    GetUser.Request::class.java to SchemaOverride(
        description = "Get user information by it's Telegram user ID",
        fields = mapOf(
            "userId" to FieldOverride(
                instance = 1281684202,
                description = "Telegram user ID to lookup"
            )
        )
    ),
    SetUserNickname.Request::class.java to SchemaOverride(
        description = "Set user's primary Minecraft nickname",
        fields = mapOf(
            "userId" to FieldOverride(
                instance = 1281684202,
                description = "Telegram user ID"
            ),
            "nickname" to FieldOverride(
                instance = "newname",
                description = "New primary Minecraft nickname"
            )
        )
    ),
    SetUserAccountType.Request::class.java to SchemaOverride(
        description = "Update user's account type/role",
        fields = mapOf(
            "userId" to FieldOverride(
                instance = 1281684202,
                description = "Telegram user ID"
            ),
            "accountType" to FieldOverride(
                instance = AccountType.PLAYER,
                description = "New account type to set"
            )
        )
    ),
    SetUserRestricted::class.java to SchemaOverride(
        description = "Set user's restriction status",
        fields = mapOf(
            "userId" to FieldOverride(
                instance = 1281684202,
                description = "Telegram user ID"
            ),
            "restricted" to FieldOverride(
                instance = false,
                description = "Whether the user should be restricted. If true, user won't interact with server bots"
            )
        )
    ),
    SetUserData.Request::class.java to SchemaOverride(
        description = "Set custom user data",
        fields = mapOf(
            "userId" to FieldOverride(
                instance = 1281684202,
                description = "Telegram user ID"
            ),
            "key" to FieldOverride(
                instance = ChatDataTypes.MINECRAFT_ACCOUNTS,
                description = "Data key to set"
            ),
            "data" to FieldOverride(
                instance = listOf(MinecraftAccountData("kochkaev", MinecraftAccountType.ADMIN)),
                description = "New data"
            )
        )
    ),
    AddUserNickname.Request::class.java to SchemaOverride(
        description = "Request to add additional Minecraft nickname",
        fields = mapOf(
            "userId" to FieldOverride(
                instance = 1281684202,
                description = "Telegram user ID"
            ),
            "nickname" to FieldOverride(
                instance = "kochkaev",
                description = "Additional Minecraft nickname to add"
            )
        )
    ),
    AddUserTempArray.Request::class.java to SchemaOverride(
        description = "Request to add value to user's temporary storage",
        fields = mapOf(
            "userId" to FieldOverride(
                instance = 1281684202,
                description = "Telegram user ID"
            ),
            "value" to FieldOverride(
                instance = 12345,
                description = "Value to add to temporary storage array"
            )
        )
    ),
    RemoveUserNickname.Request::class.java to SchemaOverride(
        description = "Request to remove a Minecraft nickname",
        fields = mapOf(
            "userId" to FieldOverride(
                instance = 1281684202,
                description = "Telegram user ID"
            ),
            "nickname" to FieldOverride(
                instance = "kochkaev",
                description = "Minecraft nickname to remove"
            )
        )
    ),
    RemoveUserTempArray.Request::class.java to SchemaOverride(
        description = "Request to remove value from user's temporary storage",
        fields = mapOf(
            "userId" to FieldOverride(
                instance = 1281684202,
                description = "Telegram user ID"
            ),
            "value" to FieldOverride(
                instance = 12345,
                description = "Value to remove from temporary storage array"
            )
        )
    ),
    SetUserAgreedWithRules.Request::class.java to SchemaOverride(
        description = "Request to update user's agreement with rules",
        fields = mapOf(
            "userId" to FieldOverride(
                instance = 1281684202,
                description = "Telegram user ID"
            ),
            "agreedWithRules" to FieldOverride(
                instance = true,
                description = "Whether user agrees with server rules"
            )
        )
    ),
    DeleteUser.Request::class.java to SchemaOverride(
        description = "Request to delete a user",
        fields = mapOf(
            "userId" to FieldOverride(
                instance = 1281684202,
                description = "Telegram user ID to delete"
            )
        )
    ),
    AddGroupAlias.Request::class.java to SchemaOverride(
        description = "Request to add group alias",
        fields = mapOf(
            "chatId" to FieldOverride(
                instance = -1002186004415,
                description = "Telegram chat ID of the target group"
            ),
            "alias" to FieldOverride(
                instance = "newAlias",
                description = "New alias to add"
            )
        )
    ),
    RemoveGroupAlias.Request::class.java to SchemaOverride(
        description = "Request to remove group alias",
        fields = mapOf(
            "chatId" to FieldOverride(
                instance = -1002186004415,
                description = "Telegram chat ID of the target group"
            ),
            "alias" to FieldOverride(
                instance = "oldAlias",
                description = "Alias to remove"
            )
        )
    ),
    AddGroupMember.Request::class.java to SchemaOverride(
        description = "Request to add member to group",
        fields = mapOf(
            "chatId" to FieldOverride(
                instance = -1002186004415,
                description = "Telegram chat ID of the target group"
            ),
            "userId" to FieldOverride(
                instance = 1281684202,
                description = "Telegram user ID to add"
            )
        )
    ),
    RemoveGroupMember.Request::class.java to SchemaOverride(
        description = "Request to remove member from group",
        fields = mapOf(
            "chatId" to FieldOverride(
                instance = -1002186004415,
                description = "Telegram chat ID of the target group"
            ),
            "userId" to FieldOverride(
                instance = 1281684202,
                description = "Telegram user ID to remove"
            )
        )
    ),
    DeleteGroup.Request::class.java to SchemaOverride(
        description = "Request to delete a group",
        fields = mapOf(
            "chatId" to FieldOverride(
                instance = -1002186004415,
                description = "Telegram chat ID to delete"
            )
        )
    ),
    GetGroup.Request::class.java to SchemaOverride(
        description = "Request to get group information",
        fields = mapOf(
            "chatId" to FieldOverride(
                instance = -1002186004415,
                description = "Telegram chat ID to lookup"
            )
        )
    ),
    SetGroupAgreedWithRules.Request::class.java to SchemaOverride(
        description = "Request to update group's agreement with rules",
        fields = mapOf(
            "chatId" to FieldOverride(
                instance = -1002186004415,
                description = "Telegram chat ID of the target group"
            ),
            "agreed" to FieldOverride(
                instance = true,
                description = "Whether group administrator agrees with server rules"
            )
        )
    ),
    RemoveGroupFeature.Request::class.java to SchemaOverride(
        description = "Request to remove a feature from group",
        fields = mapOf(
            "chatId" to FieldOverride(
                instance = -1002186004415,
                description = "Telegram chat ID of the target group"
            ),
            "feature" to FieldOverride(
                instance = FeatureTypes.PLAYERS_GROUP,
                description = "Feature to remove"
            )
        )
    ),
    SetGroupData.Request::class.java to SchemaOverride(
        description = "Request to set custom data for a group",
        fields = mapOf(
            "chatId" to FieldOverride(
                instance = -1002186004415,
                description = "Telegram chat ID of the target group"
            ),
            "key" to FieldOverride(
                instance = ChatDataTypes.GREETING_ENABLE,
                description = "Data key to set"
            ),
            "data" to FieldOverride(
                instance = true,
                description = "New data value"
            )
        )
    ),
//    MinecraftAccountData::class.java to SchemaOverride(
//        description = "Information about Minecraft account linked to user",
//        fields = mapOf(
//            "nickname" to FieldOverride(
//                instance = "kleverdi",
//                description = "Minecraft nickname"
//            ),
//            "accountStatus" to FieldOverride(
//                instance = MinecraftAccountType.PLAYER,
//                description = "Account status/type in Minecraft"
//            )
//        )
//    ),
    AccountType::class.java to SchemaOverride(
        description = "User's role/status on the server",
        global = true,
    ),
//    MinecraftAccountType::class.java to SchemaOverride(
//        description = "Type of Minecraft account",
//        instance = MinecraftAccountType.PLAYER,
//        type = String::class.java,
//        global = true,
//        fields = mapOf(
//            "PLAYER" to FieldOverride(
//                instance = "PLAYER",
//                description = "Regular player account"
//            ),
//            "ADMIN" to FieldOverride(
//                instance = "ADMIN",
//                description = "Administrator account"
//            ),
//            "BOT" to FieldOverride(
//                instance = "BOT",
//                description = "Bot/System account"
//            )
//        )
//    ),
    FeatureData::class.java to SchemaOverride(
        description = "Base class for feature configuration data",
    ),
//    PlayersGroupFeatureData::class.java to SchemaOverride(
//        description = "Settings for players group feature",
//        fields = mapOf()
//    ),
//    NewProtectedData::class.java to SchemaOverride(
//        description = "Protected message data",
//        fields = mapOf(
//            "messageId" to FieldOverride(
//                instance = 733,
//                description = "ID of the protected message"
//            ),
//            "protectedType" to FieldOverride(
//                instance = NewProtectedData.ProtectedType.TEXT,
//                description = "Type of protected content"
//            ),
//            "senderBotId" to FieldOverride(
//                instance = 7630523429,
//                description = "ID of the bot that sent the message"
//            )
//        )
//    ),
//    NewProtectedData.ProtectedType::class.java to SchemaOverride(
//        description = "Type of protected message content",
//        instance = NewProtectedData.ProtectedType.TEXT,
//        type = String::class.java,
//        global = true,
//        fields = mapOf(
//            "TEXT" to FieldOverride(
//                instance = "TEXT",
//                description = "Text message"
//            ),
//            "MEDIA" to FieldOverride(
//                instance = "MEDIA",
//                description = "Media content (images, videos, etc)"
//            ),
//            "FILE" to FieldOverride(
//                instance = "FILE",
//                description = "File attachment"
//            )
//        )
//    ),
    RemoveUserData.Request::class.java to SchemaOverride(
        description = "Remove custom user data",
        fields = mapOf(
            "userId" to FieldOverride(
                instance = 1281684202,
                description = "Telegram user ID"
            ),
            "key" to FieldOverride(
                instance = ChatDataTypes.MINECRAFT_ACCOUNTS,
                description = "Data key to remove"
            )
        )
    ),
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
        global = false,
    ),
)) {
    private fun readResolve(): Any = Overrides
}