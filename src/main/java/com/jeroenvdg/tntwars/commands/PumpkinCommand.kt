package com.jeroenvdg.tntwars.commands

import com.jeroenvdg.minigame_utilities.commands.CommandHandler
import com.jeroenvdg.minigame_utilities.commands.CommandError
import com.jeroenvdg.minigame_utilities.commands.builders.SingleCommandBuilder
import com.jeroenvdg.tntwars.TNTWars
import org.bukkit.Bukkit

class PumpkinCommand : CommandHandler() {
    init {
        builder(SingleCommandBuilder("pumpkin") {
            execute { data, player ->
                val stats = TNTWars.instance.pumpkinManager.stats
                    ?: throw CommandError("The pumpkin event is unavailable because MySQL is disabled.")
                val rank = stats.getRank(player.uniqueId)
                player.sendMessage(data.format("&pPumpkins destroyed: &s${stats.getTotal(player.uniqueId)} &r• Rank: &s${if (rank == 0) "Unranked" else "#$rank"}"))
                player.sendMessage(data.format("&pPumpkin leaderboard"))
                val leaders = stats.getLeaderboard().take(10)
                if (leaders.isEmpty()) player.sendMessage(data.format("No pumpkins have been scored yet."))
                for (score in leaders) {
                    player.sendMessage(data.format("&s#${stats.getRank(score.playerId)} &p${Bukkit.getOfflinePlayer(score.playerId).name}&r: &s${score.total}"))
                }
            }
        })
    }
}
