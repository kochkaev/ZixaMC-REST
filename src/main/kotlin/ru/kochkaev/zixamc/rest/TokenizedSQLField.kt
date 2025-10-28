package ru.kochkaev.zixamc.rest

import com.google.common.reflect.TypeToken
import ru.kochkaev.zixamc.api.ZixaMC
import ru.kochkaev.zixamc.api.config.GsonManager.gson
import ru.kochkaev.zixamc.api.sql.MySQL
import java.lang.reflect.Type
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.SQLException
import java.util.UUID

open class TokenizedSQLField<T>(
    val sql: MySQL,
    val column: String,
    val token: UUID,
    val tokenColumn: String,
    val type: Type? = object: TypeToken<T>(){}.type,
    val setter: (PreparedStatement, T) -> Unit = { ps, it -> ps.setString(1, gson.toJson(it)) },
    val getter: (ResultSet) -> T = { rs -> gson.fromJson(rs.getString(1), type) },
) {
    protected open fun exists(): Boolean = try {
        MySQL.reConnect()
        val preparedStatement =
            MySQL.MySQLConnection!!.prepareStatement("SELECT * FROM ${sql.tableName} WHERE $tokenColumn = ?;")
        preparedStatement.setString(1, token.toString())
        val query = preparedStatement.executeQuery()
        query.next()
    } catch (e: SQLException) {
        ZixaMC.logger.error("Is exists \"$tokenColumn = $token\" in table \"${sql.tableName}\" error due operation with \"$column\" column", e)
        false
    }
    open fun get(): T? = try {
        MySQL.reConnect()
        val preparedStatement =
            MySQL.MySQLConnection!!.prepareStatement("SELECT $column FROM ${sql.tableName} WHERE $tokenColumn = ?;")
        preparedStatement.setString(1, token.toString())
        val query = preparedStatement.executeQuery()
        if (query.next())
            getter(query)
        else null
    } catch (e: SQLException) {
        ZixaMC.logger.error("Get column \"$column\" in table \"${sql.tableName}\" error", e)
        null
    }
    open fun set(value: T): Boolean = try {
        if (!exists()) false
        else {
            MySQL.reConnect()
            val preparedStatement =
                MySQL.MySQLConnection!!.prepareStatement("UPDATE ${sql.tableName} SET $column = ? WHERE $tokenColumn = ?;")
            setter(preparedStatement, value)
            preparedStatement.setString(2, token.toString())
            preparedStatement.executeUpdate()
            true
        }
    } catch (e: SQLException) {
        ZixaMC.logger.error("Set column \"$column\" in table \"${sql.tableName}\" error", e)
        false
    }
}