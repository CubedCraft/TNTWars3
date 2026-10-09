package com.jeroenvdg.tntwars.services.vanishService

import com.cubedcraft.cubedcore.Core
import com.cubedcraft.cubedcore.events.VanishChangeEvent
import com.jeroenvdg.tntwars.EventBus
import com.jeroenvdg.tntwars.player.TNTWarsPlayer
import com.jeroenvdg.tntwars.player.PlayerManager
import com.jeroenvdg.minigame_utilities.Textial
import org.bukkit.event.EventHandler
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.bukkit.plugin.Plugin

class CubedCraftPlayerVanishService(val plugin: Plugin) : IPlayerVanishService, Listener {
    override fun init() {
        addEventListener(this)
    }

    override fun dispose() {
        removeEventListener(this)
    }

    override fun isPlayerVanish(user: TNTWarsPlayer): Boolean {
        return Core.instance.playerManager.getPlayer(user.identifier.uuid)?.vanished ?: false
    }

    fun addEventListener(listener : Listener) = plugin.server.pluginManager.registerEvents(listener, plugin)
    fun removeEventListener(listener: Listener) = HandlerList.unregisterAll(listener)

    @EventHandler
    private fun onVanishChange(event: VanishChangeEvent) {
        val player = event.player
        val user = PlayerManager.instance.get(player.player) ?: return player.sendMessage(Textial.msg.format("You could not be send to the vanish state"))
        EventBus.onUserVanishChanged.invoke(user)
    }
}