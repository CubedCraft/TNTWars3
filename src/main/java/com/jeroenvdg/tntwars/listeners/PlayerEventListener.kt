package com.jeroenvdg.tntwars.listeners

import com.destroystokyo.paper.event.entity.EntityKnockbackByEntityEvent
import com.destroystokyo.paper.event.player.PlayerLaunchProjectileEvent
import com.jeroenvdg.tntwars.TNTWars
import com.jeroenvdg.tntwars.listeners.BlockOwnershipManager.Companion.getOwner
import com.jeroenvdg.tntwars.listeners.BlockOwnershipManager.Companion.getTeam
import com.jeroenvdg.tntwars.player.TNTWarsPlayer
import com.jeroenvdg.tntwars.player.PlayerManager
import com.jeroenvdg.minigame_utilities.Textial
import com.jeroenvdg.minigame_utilities.Textial.Companion.deserialize
import com.jeroenvdg.tntwars.listeners.BlockOwnershipManager.Companion.setOwner
import io.papermc.paper.chat.ChatRenderer
import io.papermc.paper.event.entity.EntityKnockbackEvent
import io.papermc.paper.event.entity.EntityPushedByEntityAttackEvent
import io.papermc.paper.event.player.AsyncChatEvent
import net.kyori.adventure.identity.Identity
import net.kyori.adventure.text.Component
import org.bukkit.entity.Player
import org.bukkit.entity.TNTPrimed
import org.bukkit.entity.WindCharge
import org.bukkit.entity.minecart.ExplosiveMinecart
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.FoodLevelChangeEvent
import org.bukkit.event.player.PlayerAttemptPickupItemEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.event.player.PlayerSwapHandItemsEvent
import java.util.UUID
import kotlin.collections.get
import kotlin.jvm.optionals.getOrNull

class PlayerEventListener : Listener {

    @EventHandler
    private fun onFoodDecrease(event: FoodLevelChangeEvent) {
        event.isCancelled = true
        event.entity.foodLevel = 20
    }

    @EventHandler
    fun onWindChargePush(event: EntityKnockbackEvent) {

        println(event.toString())

        when(event) {
            is EntityKnockbackByEntityEvent -> {
                val hitEntity = event.entity
                val hitBy = event.hitBy
                if(hitBy is Player && hitEntity is Player) {
                    val hitPlayer = hitEntity.let{
                        PlayerManager.instance.get(it)!!
                    }
                    val hitByPlayer = hitBy.let{
                        PlayerManager.instance.get(it)!!
                    }
                    val result = handlePlayerKnockback(hitPlayer, hitByPlayer, event)
                }
            }
            is EntityPushedByEntityAttackEvent -> {
                val hitEntity = event.entity
                val hitBy = event.pushedBy
                if(hitBy is Player && hitEntity is Player) {
                    val hitPlayer = hitEntity.let{
                        PlayerManager.instance.get(it)!!
                    }
                    val hitByPlayer = hitBy.let{
                        PlayerManager.instance.get(it)!!
                    }
                    val result = handlePlayerPushed(hitPlayer, hitByPlayer, event)
                }
            }
        }
    }

    private fun handlePlayerKnockback(
        hitEntity: TNTWarsPlayer,
        hitBy: TNTWarsPlayer,
        event: EntityKnockbackByEntityEvent
    ): Boolean {
        val result = hitEntity.team == hitBy.team && (hitEntity.identifier.uuid != hitBy.identifier.uuid)
        if(result) {
            hitBy.bukkitPlayer.sendMessage(deserialize("&cYou cannot throw projectiles at your teammates"))
            event.isCancelled = true
            return true
        }
        return false
    }

    private fun handlePlayerPushed(
        hitEntity: TNTWarsPlayer,
        hitBy: TNTWarsPlayer,
        event: EntityPushedByEntityAttackEvent
    ): Boolean {
        val result = hitEntity.team == hitBy.team && (hitEntity.identifier.uuid != hitBy.identifier.uuid)
        if(result) {
            hitBy.bukkitPlayer.sendMessage(deserialize("&cYou cannot throw projectiles at your teammates"))
            event.isCancelled = true
            return true
        }
        return false
    }

    @EventHandler
    fun onWindChargeSpawn(event: PlayerLaunchProjectileEvent) {
        val player = event.player
        val entity = event.projectile
        if(entity !is WindCharge) return
        val owner = entity.getOwner()
        if(owner == null) entity.setOwner(player)
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
            null -> Textial.Yellow.color
            else -> user.team.primaryColor.color
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
                    return@ChatRenderer Textial.msg.parse("&${user.team.primaryColor.char}&lTEAMCHAT &${user.team.primaryColor.char}${player.name} &6${Textial.doubleArrowSymbol} ")
                        .append(event.message().color(Textial.White.color))
                })
            }
            catch (e: Exception) {
                event.isCancelled = true
                event.player.sendMessage(Textial.cmd.format("Unable to send the message to all members, please disable &p/teamchat&r for now"))
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
        val damager = event.damager

        val isAllowedExplosive = damager is TNTPrimed ||
                damager is ExplosiveMinecart ||
                damager is WindCharge
        if (!isAllowedExplosive) return false

        if (user.team.isSpectatorTeam) {
            event.isCancelled = true
            return true
        }

        val ownerId = damager.getOwner()
        val entityTeam = damager.getTeam()

        if (ownerId != null && ownerId == user.bukkitPlayer.uniqueId.toString()) {
            return false
        }

        if (entityTeam != null && user.team == entityTeam) {
            event.isCancelled = true
            //println("${damager.type} friendly-fire protected for ${user.bukkitPlayer.name}!")
            return true
        }

        return false
    }

}