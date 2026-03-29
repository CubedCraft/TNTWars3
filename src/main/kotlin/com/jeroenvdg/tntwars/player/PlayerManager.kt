package com.jeroenvdg.tntwars.player

import com.jeroenvdg.minigame_utilities.TextHelper
import com.jeroenvdg.minigame_utilities.Textial
import com.jeroenvdg.minigame_utilities.await
import com.jeroenvdg.minigame_utilities.launchCoroutine
import com.jeroenvdg.tntwars.EventBus
import com.jeroenvdg.tntwars.TNTWars
import com.jeroenvdg.tntwars.game.GameManager
import com.jeroenvdg.tntwars.game.Team
import com.jeroenvdg.tntwars.services.userIdentifier.UserIdentifier
import kotlinx.coroutines.Job
import net.kyori.adventure.audience.Audience
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import java.util.*

class PlayerManager : Listener, HashMap<UUID, TNTWarsPlayer>() {

    val players get() = values

    companion object {
        val instance get() = TNTWars.instance.playerManager
    }

    init {
        EventBus.onUserVanishChanged += ::handleVanishChanged
    }

    fun repair() {
        for (user in this.players) {
            removePlayer(user.bukkitPlayer, false)
        }

        for (player in Bukkit.getOnlinePlayers()) {
            addPlayer(player, false)
        }
    }

    fun addPlayer(player: Player, sendJoinMessage: Boolean = true) : Job {
        val user = TNTWarsPlayer(player)
        return launchCoroutine {
            user.init().await()
            updatePlayerVisibility(user)

            if (sendJoinMessage) {
                joinMessage(user, user.isVanishMode)
            }

            this[player.uniqueId] = user
            player.sendMessage(GameManager.instance.activeMap.mapMessage)
            EventBus.onPlayerJoined.invoke(user)
        }
    }

    fun removePlayer(player: Player, sendLeaveMessage: Boolean = true): Job? {
        val user = get(player.uniqueId) ?: return null
        this.remove(player.uniqueId)
        val job = user.dispose()

        if (sendLeaveMessage) {
            leaveMessage(user, user.isVanishMode)
        }

        EventBus.onPlayerLeft.invoke(user)
        return job
    }

    fun updatePlayerVisibility(user: TNTWarsPlayer) {
        val plugin = TNTWars.instance

        for (other in players) {
            if (other == user) continue

            if (canSeePlayer(user, other)) user.bukkitPlayer.showPlayer(plugin, other.bukkitPlayer)
            else user.bukkitPlayer.hidePlayer(plugin, other.bukkitPlayer)

            if (canSeePlayer(other, user)) other.bukkitPlayer.showPlayer(plugin, user.bukkitPlayer)
            else other.bukkitPlayer.hidePlayer(plugin, user.bukkitPlayer)
        }
    }

    fun updatePlayerVisibility() {
        val plugin = TNTWars.instance
        val players = this.players

        for (playerA in players) {
            for (playerB in players) {
                if (playerA == playerB) continue
                if (canSeePlayer(playerA, playerB)) playerA.bukkitPlayer.showPlayer(plugin, playerB.bukkitPlayer)
                else playerA.bukkitPlayer.hidePlayer(plugin, playerB.bukkitPlayer)
            }
        }
    }

    fun findUsersInTeam(team: Team): List<TNTWarsPlayer> {
        return values.filter { it.team == team }
    }

    fun get(player: Player?): TNTWarsPlayer? {
        return this[player?.uniqueId ?: return null]
    }

    fun get(userIdentifier: UserIdentifier?): TNTWarsPlayer? {
        return this[userIdentifier?.uuid ?: return null]
    }

    private fun canSeePlayer(user: TNTWarsPlayer, other: TNTWarsPlayer): Boolean {
        if (other.isVanishMode) return user.canSeeVanish
        if (user.team.isSpectatorTeam) return true
        return !other.team.isSpectatorTeam
    }

    private fun joinMessage(user: TNTWarsPlayer, inVanish: Boolean) {
        if (inVanish) {
            Audience.audience(Bukkit.getOnlinePlayers().filter { it.hasPermission("cubedcraft.staff") })
                .sendMessage(TextHelper.deserialize("${TextHelper.infoPrefix} &c&l${TextHelper.toSmallText("vanish")} &7${user.bukkitPlayer.name} ${TextHelper.mainColor}joined"))
        } else {
            Bukkit.broadcast(TextHelper.deserialize("${TextHelper.infoPrefix} &7${user.bukkitPlayer.name} ${TextHelper.mainColor}joined"))
        }
    }

    private fun leaveMessage(user: TNTWarsPlayer, inVanish: Boolean) {
        if (inVanish) {
            Audience.audience(Bukkit.getOnlinePlayers().filter { it.hasPermission("cubedcraft.staff") })
                .sendMessage(TextHelper.deserialize("${TextHelper.infoPrefix} &c&l${TextHelper.toSmallText("vanish")} &7${user.bukkitPlayer.name} ${TextHelper.mainColor}left"))
        } else {
            Bukkit.broadcast(TextHelper.deserialize("${TextHelper.infoPrefix} &7${user.bukkitPlayer.name} ${TextHelper.mainColor}left"))
        }
    }

    @EventHandler
    private fun onPlayerJoined(event: PlayerJoinEvent) {
        event.joinMessage(null)
        addPlayer(event.player)
    }

    @EventHandler
    private fun onPlayerLeft(event: PlayerQuitEvent) {
        event.quitMessage(null)
        removePlayer(event.player)
    }

    private fun handleVanishChanged(user: TNTWarsPlayer) {
        if (user.isVanishMode) {
            leaveMessage(user, false)
        } else {
            joinMessage(user, false)
        }

        updatePlayerVisibility(user)
    }
}
