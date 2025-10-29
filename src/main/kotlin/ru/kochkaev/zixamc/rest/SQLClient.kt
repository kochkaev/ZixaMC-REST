package ru.kochkaev.zixamc.rest

import com.google.gson.annotations.JsonAdapter
import com.google.gson.reflect.TypeToken
import ru.kochkaev.zixamc.api.ZixaMC
import ru.kochkaev.zixamc.api.config.GsonManager.gson
import ru.kochkaev.zixamc.api.config.serialize.SimpleAdapter
import ru.kochkaev.zixamc.api.sql.MySQL
import java.sql.SQLException
import java.util.UUID

@JsonAdapter(SQLClientAdapter::class)
class SQLClient private constructor(val token: UUID) {

    private val userIdField = TokenizedSQLField<Long>(
        sql = SQLClient,
        column = "user_id",
        token = token,
        tokenColumn = "token",
        type = Long::class.java,
        setter = { ps, it -> ps.setLong(1, it) },
        getter = { rs -> rs.getLong(1) },
    )
    var userId: Long
        get() = userIdField.get()!!
        set(value) { userIdField.set(value) }
    private val markField = TokenizedSQLField<String?>(
        sql = SQLClient,
        column = "mark",
        token = token,
        tokenColumn = "token",
        type = Long::class.java,
        setter = { ps, it -> ps.setString(1, it) },
        getter = { rs -> rs.getString(1) },
    )
    var mark: String?
        get() = markField.get()!!
        set(value) { markField.set(value) }
    val permissions = TokenizedSQLArray<String>(
        sql = SQLClient,
        column = "permissions",
        token = token,
        tokenColumn = "token",
        deserializer = { gson.fromJson(it, object: TypeToken<List<String>>(){}.type) },
        valSerializer = { it }
    )

    companion object: MySQL() {
        override val tableName: String = Config.config.sqlClientsTableName
        override fun getModel(): String =
            String.format(
                """
                CREATE TABLE `%s`.`%s` (
                    `token` CHAR(36) NOT NULL,
                    `user` BIGINT NOT NULL,
                    `mark` VARCHAR(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci,
                    `permissions` JSON NOT NULL DEFAULT "[]",
                    UNIQUE (`token`)
                ) ENGINE = InnoDB;
                """.trimIndent(),
                config.database,
                Config.config.sqlClientsTableName
            )

        fun get(token: UUID) =
            if (exists(token)) SQLClient(token)
            else null
        @Deprecated("Not safe", replaceWith = ReplaceWith("get(token)"))
        fun getWithoutCheck(token: UUID) = SQLClient(token)
        fun getAllFromUser(userId: Long) = try {
            reConnect()
            val preparedStatement =
                MySQLConnection!!.prepareStatement("SELECT token FROM $tableName WHERE user_id = ?;")
            preparedStatement.setLong(1, userId)
            val query = preparedStatement.executeQuery()
            val clients = arrayListOf<SQLClient>()
            while (query.next())
                clients.add(getWithoutCheck(UUID.fromString(query.getString(1))))
            clients
        } catch (e: SQLException) {
            ZixaMC.logger.error("getRestClientData error ", e)
            listOf()
        }
        fun getAllWithMark(mark: String) = try {
            reConnect()
            val preparedStatement =
                MySQLConnection!!.prepareStatement("SELECT token FROM $tableName WHERE mark = ?;")
            preparedStatement.setString(1, mark)
            val query = preparedStatement.executeQuery()
            val clients = arrayListOf<SQLClient>()
            while (query.next())
                clients.add(getWithoutCheck(UUID.fromString(query.getString(1))))
            clients
        } catch (e: SQLException) {
            ZixaMC.logger.error("getRestClientData error ", e)
            listOf()
        }
        fun getAllWithPermission(permission: String) = try {
            reConnect()
            val preparedStatement =
                MySQLConnection!!.prepareStatement("SELECT token FROM $tableName WHERE JSON_CONTAINS(permissions, JSON_QUOTE(?), '$');")
            preparedStatement.setString(1, permission)
            val query = preparedStatement.executeQuery()
            val clients = arrayListOf<SQLClient>()
            while (query.next())
                clients.add(getWithoutCheck(UUID.fromString(query.getString(1))))
            clients
        } catch (e: SQLException) {
            ZixaMC.logger.error("getRestClientData error", e)
            listOf()
        }
        fun create(token: UUID, userId: Long, mark: String?, permissions: List<String> = listOf()): Boolean {
            try {
                reConnect()
                if (!exists(token)) {
                    val preparedStatement =
                        MySQLConnection!!.prepareStatement("INSERT INTO $tableName (token, user_id, mark, permissions) VALUES (?, ?, ?, ?);")
                    preparedStatement.setString(1, token.toString())
                    preparedStatement.setLong(2, userId)
                    preparedStatement.setString(3, mark)
                    preparedStatement.setString(4, gson.toJson(permissions?:listOf<String>()))
                    preparedStatement.executeUpdate()
                    return true
                }
            } catch (e: SQLException) {
                ZixaMC.logger.error("Rest client create error ", e)
            }
            return false
        }

        fun exists(token: UUID) =
            try {
                reConnect()
                val preparedStatement =
                    MySQLConnection!!.prepareStatement("SELECT * FROM $tableName WHERE token = ?;")
                preparedStatement.setString(1, token.toString())
                preparedStatement.executeQuery().next()
            } catch (e: SQLException) {
                ZixaMC.logger.error("isRestClientRegistered error", e)
                false
            }
    }
}
class SQLClientAdapter: SimpleAdapter<SQLClient>(
    reader = { SQLClient.getWithoutCheck(UUID.fromString(it.nextString())) },
    writer = { out, it -> out.value(it.token.toString()) }
)