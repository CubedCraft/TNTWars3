package com.jeroenvdg.tntwars.commands

import com.jeroenvdg.minigame_utilities.commands.CommandData
import com.jeroenvdg.minigame_utilities.commands.CommandHandler
import com.jeroenvdg.minigame_utilities.commands.builders.CommandBuilder
import com.jeroenvdg.minigame_utilities.commands.builders.SingleCommandBuilder
import com.jeroenvdg.tntwars.interfaces.BoosterInterface
import org.bukkit.entity.Player

class BoosterCommand : CommandHandler() {

    init {
        builder(SingleCommandBuilder("booster") {
            execute(::openSettings)
        })
    }

    private fun openSettings(data: CommandData, player: Player) {
        BoosterInterface.open(player)
    }

}
