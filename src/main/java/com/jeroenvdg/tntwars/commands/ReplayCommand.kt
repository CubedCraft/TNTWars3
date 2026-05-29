package com.jeroenvdg.tntwars.commands

import com.jeroenvdg.minigame_utilities.commands.CommandError
import com.jeroenvdg.minigame_utilities.commands.CommandHandler
import com.jeroenvdg.minigame_utilities.commands.builders.SingleCommandBuilder
import com.jeroenvdg.tntwars.game.Team
import com.jeroenvdg.tntwars.interfaces.ReplayInterface
import com.jeroenvdg.tntwars.player.PlayerManager

class ReplayCommand : CommandHandler() {
    init {
        builder(SingleCommandBuilder("replay") {
            execute { _, player ->
                val user =
                    PlayerManager.instance.get(player) ?: throw CommandError("You must be in the game to view replays")
                if (user.team != Team.Spectator) throw CommandError("You can only view replays while spectating")

                ReplayInterface.open(player)
            }
        })
    }
}
