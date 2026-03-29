package com.jeroenvdg.minigame_utilities

import org.bukkit.Bukkit
import org.bukkit.plugin.java.JavaPlugin
import java.util.logging.Level

class Debug private constructor() {
    companion object {
        private lateinit var plugin: JavaPlugin

        fun broadcast(msg: String) { Bukkit.broadcast(Textial.debug.format(msg)) }
        fun log(msg: String, sendMessageToOps: Boolean = false) { plugin.logger.log(Level.INFO, msg); if (sendMessageToOps) msgToOps(msg) }
        fun warn(msg: String, sendMessageToOps: Boolean = false) { plugin.logger.log(Level.WARNING, msg); if (sendMessageToOps) msgToOps(msg) }
        fun error(msg: String, sendMessageToOps: Boolean = false) { plugin.logger.log(Level.SEVERE, msg); if (sendMessageToOps) msgToOps(msg) }
        fun error(error: Exception) {
            var error: Throwable = error
            error("========================================================")
            error("An Exception has been thrown!")
            error("${error.javaClass.name}: ${error.message}")
            for (stackTraceElement in error.stackTrace) error("## $stackTraceElement")
            while (error.cause != null) {
                error = error.cause!!
                error("")
                error("Caused by:")
                error("${error.javaClass.name}: ${error.message}")
                for (stackTraceElement in error.stackTrace) error("## $stackTraceElement")
            }
            error("========================================================")
        }

        fun msgToOps(string: String) {
            for (player in Bukkit.getOnlinePlayers()) {
                if (!player.hasPermission("cubed.staff")) continue
                player.sendMessage(TextHelper.format(string))
            }
        }

        fun setup(plugin: JavaPlugin) {
            this.plugin = plugin
        }
    }
}