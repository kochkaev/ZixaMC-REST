package ru.kochkaev.zixamc.rest

import com.google.common.reflect.TypeToken
import ru.kochkaev.zixamc.api.ZixaMC
import ru.kochkaev.zixamc.api.config.GsonManager.gson
import ru.kochkaev.zixamc.api.sql.MySQL
import java.sql.SQLException
import java.util.UUID

open class TokenizedSQLArray<T>(
    val sql: MySQL,
    val column: String,
    val token: UUID,
    val tokenColumn: String,
    val serializer: (List<T>) -> String = { gson.toJson(it) },
    val deserializer: (String) -> List<T> = { gson.fromJson(it, object: TypeToken<List<T>>(){}.type) },
    val valSerializer: (T) -> String = { gson.toJson(it) },
) {
    open fun get() =
        try {
            MySQL.reConnect()
            val preparedStatement =
                MySQL.MySQLConnection!!.prepareStatement("SELECT $column FROM ${sql.tableName} WHERE $tokenColumn = ?;")
            preparedStatement.setString(1, token.toString())
            val query = preparedStatement.executeQuery()
            query.next()
            deserializer(query.getString(1))
        } catch (e: SQLException) {
            ZixaMC.logger.error("Get SQLArray \"$column\" in table \"${sql.tableName}\" error", e)
            null
        }
    open fun set(array: List<T>) {
        try {
            MySQL.reConnect()
            val preparedStatement =
                MySQL.MySQLConnection!!.prepareStatement("UPDATE ${sql.tableName} SET $column = ? WHERE $tokenColumn = ?;")
            preparedStatement.setString(1, serializer(array))
            preparedStatement.setString(2, token.toString())
            preparedStatement.executeUpdate()
        } catch (e: SQLException) {
            ZixaMC.logger.error("Set SQLArray \"$column\" in table \"${sql.tableName}\" error", e)
        }
    }

    open fun contains(value: T): Boolean =
        try {
            MySQL.reConnect()
            val preparedStatement =
                MySQL.MySQLConnection!!.prepareStatement("SELECT JSON_CONTAINS($column, JSON_QUOTE(?), '$') FROM ${sql.tableName} WHERE $tokenColumn = ?;")
            preparedStatement.setString(1, valSerializer(value))
            preparedStatement.setString(2, token.toString())
            val query = preparedStatement.executeQuery()
            query.next()
            query.getBoolean(1)
        } catch (e: SQLException) {
            ZixaMC.logger.error("Check contains in SQLArray \"$column\" in table \"${sql.tableName}\" error", e)
            false
        }
    open fun add(value: T) = try {
        if (contains(value)) false
        else {
            MySQL.reConnect()
            val preparedStatement =
                MySQL.MySQLConnection!!.prepareStatement("UPDATE ${sql.tableName} SET $column = JSON_ARRAY_APPEND($column, '$', ?) WHERE $tokenColumn = ?;")
            preparedStatement.setString(1, valSerializer(value))
            preparedStatement.setString(2, token.toString())
            preparedStatement.executeUpdate()
            true
        }
    } catch (e: SQLException) {
        ZixaMC.logger.error("Add value to SQLArray \"$column\" in table \"${sql.tableName}\" error", e)
        false
    }
    open fun remove(value: T) = try {
        if (!contains(value)) false
        else {
            MySQL.reConnect()
            val statement = "UPDATE ${sql.tableName} " +
                    "SET $column = JSON_REMOVE($column, JSON_UNQUOTE(JSON_SEARCH($column, 'one', ?, NULL, '$'))) " +
                    "WHERE $tokenColumn = ?;"
            val preparedStatement =
                MySQL.MySQLConnection!!.prepareStatement(statement)
            preparedStatement.setString(1, valSerializer(value))
            preparedStatement.setString(2, token.toString())
            preparedStatement.executeUpdate()
            true
        }
    } catch (e: SQLException) {
        ZixaMC.logger.error("Remove value from SQLArray \"$column\" in table \"${sql.tableName}\" error", e)
        false
    }
}