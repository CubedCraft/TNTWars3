package com.jeroenvdg.tntwars.listeners

import com.jeroenvdg.minigame_utilities.TextHelper
import com.jeroenvdg.minigame_utilities.fromLegacyCode
import com.jeroenvdg.tntwars.TNTWars
import com.jeroenvdg.tntwars.listeners.BlockOwnershipManager.Companion.getOwner
import com.jeroenvdg.tntwars.listeners.BlockOwnershipManager.Companion.getTeam
import com.jeroenvdg.tntwars.player.PlayerManager
import com.jeroenvdg.tntwars.player.TNTWarsPlayer
import io.papermc.paper.chat.ChatRenderer
import io.papermc.paper.event.player.AsyncChatEvent
import net.kyori.adventure.identity.Identity
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.entity.Player
import org.bukkit.entity.TNTPrimed
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.FoodLevelChangeEvent
import org.bukkit.event.player.PlayerAttemptPickupItemEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.event.player.PlayerSwapHandItemsEvent
import kotlin.jvm.optionals.getOrNull

class PlayerEventListener : Listener {

    @EventHandler
    private fun onFoodDecrease(event: FoodLevelChangeEvent) {
        event.isCancelled = true
        event.entity.foodLevel = 20
    }

    @EventHandler
    private fun onPlayerDamaged(event: EntityDamageEvent) {
        if (event.entity !is Player) return
        val user = TNTWars.instance.playerManager.get(event.entity as Player) ?: return
        if (event is EntityDamageByEntityEvent && handleDamageByTNT(user, event)) return
        user.onDamaged.invoke(event)
    }

    @EventHandler
    private fun onPlayerMoved(event: PlayerMoveEvent) {
        val user = TNTWars.instance.playerManager.get(event.player) ?: return
        user.onPlayerMoved.invoke(event)
    }

    @EventHandler
    private fun onInteract(event: PlayerInteractEvent) {
        val user = TNTWars.instance.playerManager.get(event.player) ?: return
        user.onInteract.invoke(event)
    }

    @EventHandler
    private fun onItemPickup(event: PlayerAttemptPickupItemEvent) {
        event.isCancelled = true
    }

    @EventHandler
    private fun onItemSwap(event: PlayerSwapHandItemsEvent) {
        val user = PlayerManager.instance.get(event.player) ?: return
        user.onHandItemSwap.invoke(event)
    }

    @EventHandler
    private fun onChatMessage(event: AsyncChatEvent) {
        val user = PlayerManager.instance.get(event.player)
        val color = when (user) {
            null -> NamedTextColor.YELLOW
            else -> fromLegacyCode(user.team.primaryColor)
        }

        if (user != null && user.teamChatEnabled && !user.team.isSpectatorTeam) {
            try {
                val playerManager = PlayerManager.instance
                event.viewers().removeIf { audience ->
                    val uuid = audience.get(Identity.UUID).getOrNull() ?: return@removeIf false
                    val receiver = playerManager[uuid] ?: return@removeIf false
                    return@removeIf receiver.team != user.team
                }

                event.renderer(ChatRenderer { player, _, _, _ ->
                    return@ChatRenderer TextHelper.deserialize("&${user.team.primaryColor}&lTEAMCHAT &${user.team.primaryColor}${player.name} ")
                        .append(event.message().color(NamedTextColor.WHITE))
                })
            }
            catch (e: Exception) {
                event.isCancelled = true
                event.player.sendMessage(TextHelper.deserialize("Unable to send the message to all members, please disable &p/teamchat&r for now"))
            }
        }

        val previous = event.renderer()
        event.renderer(ChatRenderer { player, sourceDisplay, sourceMessage, audience ->
            val currentFormat = previous.render(player, sourceDisplay, sourceMessage, audience)
            return@ChatRenderer Component.text()
                .append(Component.text("${user?.getRank() ?: "[???]"} ").color(color))
                .append(currentFormat)
                .build()
        })
    }

    private fun handleDamageByTNT(user: TNTWarsPlayer, event: EntityDamageByEntityEvent): Boolean {
        if (event.damager !is TNTPrimed) return false
        val entityTeam = event.damager.getTeam() ?: return false
        if (user.team.isSpectatorTeam) return false
        if (user.team != entityTeam) return false
        val owner = event.damager.getOwner() ?: return false
        if (owner == user.bukkitPlayer.uniqueId.toString()) return false
        event.isCancelled = true
        return true
    }

}