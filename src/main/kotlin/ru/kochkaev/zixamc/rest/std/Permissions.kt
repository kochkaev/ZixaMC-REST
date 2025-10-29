package ru.kochkaev.zixamc.rest.std

object Permissions {
    const val DOWNLOAD_FILES = "std.download_files"
    const val UPLOAD_FILES = "std.upload_files"

    const val READ_USER = "std.read_user"
    const val READ_ALL_USERS = "std.read_all_users"
    const val WRITE_USER_OVERRIDE = "std.write_user.override"
    const val WRITE_USER_NICKNAMES = "std.write_user.nicknames"
    const val WRITE_USER_ACCOUNT_TYPE = "std.write_user.account_type"
    const val WRITE_USER_TEMP_ARRAY = "std.write_user.account_type"
    const val WRITE_USER_AGREED_WITH_RULES = "std.write_user.agreed_with_rules"
    const val WRITE_USER_IS_RESTRICTED = "std.write_user.is_restricted"
    const val WRITE_USER_DATA = "std.write_user.data"
    const val CREATE_USER = "std.create_user"
    const val DELETE_USER = "std.delete_user"

    const val READ_GROUP = "std.read_group"
    const val READ_ALL_GROUPS = "std.read_all_groups"
    const val WRITE_GROUP_OVERRIDE = "std.write_group.override"
    const val WRITE_GROUP_NAMES = "std.write_group.names"
    const val WRITE_GROUP_MEMBERS = "std.write_group.members"
    const val WRITE_GROUP_AGREED_WITH_RULES = "std.write_group.agreed_with_rules"
    const val WRITE_GROUP_IS_RESTRICTED = "std.write_group.is_restricted"
    const val WRITE_GROUP_FEATURES = "std.write_group.features"
    const val WRITE_GROUP_DATA = "std.write_group.data"
    const val CREATE_GROUP = "std.create_group"
    const val DELETE_GROUP = "std.delete_group"
}