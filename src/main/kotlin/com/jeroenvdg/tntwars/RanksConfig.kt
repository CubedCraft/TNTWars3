package com.jeroenvdg.tntwars

import org.bukkit.configuration.file.FileConfiguration
import java.lang.Integer.parseInt

class RanksConfig private constructor(fileConfiguration: FileConfiguration){
    companion object {
        fun from(configuration: FileConfiguration): RanksConfig {
            return RanksConfig(configuration)
        }
    }

    val ranks: Array<Rank>

    init {
        val parsedRanks = ArrayList<Rank>()
        val map = fileConfiguration.getConfigurationSection("ranks") ?: throw Exception("No ranks found!")
        for (score in map.getKeys(false)) {
            parsedRanks.add(Rank(parseInt(score), map.getString(score) ?: throw IllegalArgumentException("Illegal argument for '$score'")))
        }
        ranks = parsedRanks.toTypedArray()
    }
}

data class Rank(val score: Int, val rank: String)