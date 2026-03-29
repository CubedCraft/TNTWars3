package com.jeroenvdg.tntwars

import com.jeroenvdg.minigame_utilities.Debug
import com.jeroenvdg.minigame_utilities.Textial
import org.bukkit.Material
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.file.FileConfiguration

class Config private constructor(configuration: FileConfiguration) {
    companion object {
        fun from(configuration: FileConfiguration): Config {
            return Config(configuration)
        }
    }

    val serverId = configuration.getInt("serverId", 6)
    val mySQLConfig = MySQLConfig(configuration.getConfigurationSection("mySQL")!!)
    val discordConfig = DiscordConfig(configuration.getConfigurationSection("discord")!!)
    val gameConfig = GameConfig(configuration.getConfigurationSection("game")!!)
    val rewardConfig = RewardsConfig(configuration.getConfigurationSection("rewards")!!)
    val message = MessageConfig(configuration.getConfigurationSection("messages")!!)
    val itemSelectorConfig = ItemSelectorConfig(configuration.getConfigurationSection("itemSelector")!!)
}

class MySQLConfig(section: ConfigurationSection) {
    val enabled = section.getBoolean("enabled", false)
    val host = section.getString("host", "localhost")!!
    val port = section.getInt("port", 3306)
    val database = section.getString("database", "database")!!
    val username = section.getString("username", "username")!!
    val password = section.getString("password", "")!!
}

class DiscordConfig(section: ConfigurationSection) {
    val enabled = section.getBoolean("enabled", false)
    val tntwarsChannelId = section.getString("tntwars-channel", "")!!
    val generalChannelId = section.getString("general-channel", "")!!
    val tntwarsWebhookURL = section.getString("tntwars-webhook", "")!!
}

class GameConfig(section: ConfigurationSection) {
    val matchTime = section.getInt("matchTime", 1800)
    val countdownTime = section.getInt("countdownTime", 30)
    val tntInDispenser = section.getInt("tntInDispenser", 288)
    val tournamentMode = GameTournamentModeConfig(section.getConfigurationSection("tournamentMode")!!);
}

class GameTournamentModeConfig(section: ConfigurationSection) {
    val enabled = section.getBoolean("enabled", false)
    val lives = section.getInt("lives");
}

class RewardsConfig(section: ConfigurationSection) {
    val winRewards = RewardConfig(section.getConfigurationSection("win")!!)
    val killRewards = RewardConfig(section.getConfigurationSection("kill")!!)
    val mvpRewards = RewardConfig(section.getConfigurationSection("mvp")!!)
}

class RewardConfig(section: ConfigurationSection) {
    val coins = section.getInt("coins", 0)
    val score = section.getInt("score", 0)
    val points = section.getInt("points", 0)
}

class MessageConfig(section: ConfigurationSection) {
    val matchStartMessage = Textial.msg.parse(section.getStringList("matchStartMessage"))
    val matchStartTitle = Textial.msg.parse(section.getString("matchStartTitle", "Plz don't hack :(")!!)
    val matchStartSubtitle = Textial.msg.parse(section.getString("matchStartSubtitle", "Plz don't hack :(")!!)
}

class ItemSelectorConfig(config: ConfigurationSection) {
    val selectorItem: Material
    val items: Array<Material>

    init {
        val selectorItemResult = getMaterial(config.getString("selectorItem")!!)
        if (selectorItemResult.isFailure) {
            Debug.error(selectorItemResult.exceptionOrNull()!!.message!!)
            selectorItem = Material.STONE
        } else {
            selectorItem = selectorItemResult.getOrThrow()
        }

        val items = mutableListOf<Material>()

        for (value in config.getStringList("items")) {
            val materialResult = getMaterial(value)
            if (materialResult.isFailure) {
                Debug.error(materialResult.exceptionOrNull()!!.message!!)
            } else {
                items.add(materialResult.getOrThrow())
            }
        }

        this.items = items.toTypedArray()
    }

    private fun getMaterial(name: String): Result<Material> {
        val material = Material.getMaterial(name)
        if (material != null) return Result.success(material)
        return Result.success(Material.getMaterial(name, true) ?: return Result.failure(IllegalArgumentException("Material $name not found")))
    }
}