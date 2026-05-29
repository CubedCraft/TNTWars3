package com.jeroenvdg.tntwars.player.behaviours

import com.jeroenvdg.tntwars.game.GameManager
import com.jeroenvdg.tntwars.player.PlayerBehaviour
import com.jeroenvdg.tntwars.player.TNTWarsPlayer
import com.jeroenvdg.minigame_utilities.Textial
import com.sk89q.worldedit.bukkit.BukkitAdapter
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.player.PlayerMoveEvent

class PlayerTeamBorderBehaviour(user: TNTWarsPlayer) : PlayerBehaviour(user) {

    override fun onActivate() {
        user.onPlayerMoved += ::handlePlayerMoved
        user.onBlockPlaced += ::handleBlockPlaced
    }

    override fun onDeactivate() {
        user.onPlayerMoved -= ::handlePlayerMoved
        user.onBlockPlaced -= ::handleBlockPlaced
    }

    private fun handlePlayerMoved(e: PlayerMoveEvent) {
        if (!GameManager.instance.isGameWorld(player.world)) return

        val team = user.team
        if (user.ignoreTeamBounds) return
        if (team.isSpectatorTeam) return

        val region = GameManager.instance.activeMap.teamRegions[team] ?: return
        val location = player.location

        var changed = false
        if (location.x > region.maximumX + 1) {
            location.x = region.maximumX.toDouble() + 1
            changed = true
        }
        if (location.x < region.minimumX) {
            location.x = region.minimumX.toDouble()
            changed = true
        }
        if (location.z > region.maximumZ + 1) {
            location.z = region.maximumZ.toDouble() + 1
            changed = true
        }
        if (location.z < region.minimumZ) {
            location.z = region.minimumZ.toDouble()
            changed = true
        }
        if (location.y > region.maximumY) {
            location.y = region.maximumY.toDouble()
            player.sendMessage(Textial.msg.format("&cYou have reached the height limit"))
            changed = true
        }

        if (changed) {
            player.teleport(location)
        }
    }

    private fun handleBlockPlaced(e: BlockPlaceEvent) {
        if (!GameManager.instance.isGameWorld(e.block.world)) return

        val team = user.team
        if (team.isSpectatorTeam) return

        val region = GameManager.instance.activeMap.teamRegions[team] ?: return
        if (region.contains(BukkitAdapter.adapt(e.block.location).toBlockPoint())) return
        e.isCancelled = true
    }

}
