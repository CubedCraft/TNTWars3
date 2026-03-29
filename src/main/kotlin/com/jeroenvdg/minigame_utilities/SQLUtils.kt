package com.jeroenvdg.minigame_utilities

import java.sql.DriverManager

object SQLUtils {
    fun createDatabaseIfMissing(host: String, port: Int, username: String, password: String) {
        val url = "jdbc:mysql://$host:$port/"

        DriverManager.getConnection(url, username, password).use { connection ->
            connection.createStatement().use { statement ->
                statement.executeUpdate("CREATE DATABASE IF NOT EXISTS tntwars_test")
            }
        }
    }
}