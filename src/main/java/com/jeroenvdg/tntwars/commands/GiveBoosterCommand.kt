package com.jeroenvdg.tntwars.commands

import com.jeroenvdg.minigame_utilities.Textial.Companion.deserialize
import com.jeroenvdg.minigame_utilities.commands.CommandData
import com.jeroenvdg.minigame_utilities.commands.CommandHandler
import com.jeroenvdg.minigame_utilities.commands.builders.CommandBuilder
import com.jeroenvdg.minigame_utilities.commands.builders.SingleCommandBuilder
import com.jeroenvdg.tntwars.interfaces.BoosterInterface
import com.jeroenvdg.tntwars.managers.BoosterManager
import com.jeroenvdg.tntwars.player.PlayerManager
import com.jeroenvdg.tntwars.player.TNTWarsPlayer
import org.bukkit.Bukkit
import org.bukkit.entity.Player

class GiveBoosterCommand : CommandHandler() {

    init {
        builder(SingleCommandBuilder("givebooster") {
            permissions("bcore.*")
            playerParam("player", true, online = false, allowAll = false)
            execute(::giveBooster)
        })
    }

    private fun giveBooster(data: CommandData, player: Player) {
        val player = data.getParam<Player>("player")
        BoosterManager.instance.giveBooster(PlayerManager.instance.get(player)!!)
        Bukkit.broadcast(deserialize("&a&l${player.name} got a booster!"))
    }

}
