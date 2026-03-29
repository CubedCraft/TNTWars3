package com.jeroenvdg.tntwars.player.states

import com.jeroenvdg.tntwars.EventBus
import com.jeroenvdg.tntwars.game.GameManager
import com.jeroenvdg.tntwars.game.Team
import com.jeroenvdg.tntwars.game.TeamSelectMode
import com.jeroenvdg.tntwars.interfaces.MapSelector
import com.jeroenvdg.tntwars.interfaces.ProfileInterface
import com.jeroenvdg.tntwars.interfaces.SettingsInterface
import com.jeroenvdg.tntwars.interfaces.TeamSelector
import com.jeroenvdg.tntwars.misc.PlayerDeathContext
import com.jeroenvdg.tntwars.player.TNTWarsPlayer
import com.jeroenvdg.minigame_utilities.Scheduler
import com.jeroenvdg.minigame_utilities.Textial
import com.jeroenvdg.tntwars.TNTWars
import org.bukkit.GameMode
import org.bukkit.event.player.PlayerInteractEvent

class PlayerSpectatorState(tntWarsPlayer: TNTWarsPlayer) : BasePlayerState(tntWarsPlayer) {

    companion object {
        val config get() = TNTWars.instance.config;
    }

    override val flyEnabled = true
    private var joinMode = GameManager.instance.teamSelectMode

    override fun onActivate() {
        user.isGodMode = true
        player.gameMode = GameMode.ADVENTURE
        super.onActivate() // run after changing gamemode!

        user.onTeamChanged += ::handleTeamChanged
        user.onInteract += ::handleInteract
        user.resetInventory()

        startCoroutine {
            val mode = joinMode
            Scheduler.delay(20)
            if (mode == joinMode) modeChangedMsg(joinMode)
        }
        EventBus.onTeamSelectorModeChanged += ::handleTeamSelectorModeChanged
    }

    override fun onDeactivate() {
        super.onDeactivate()
        user.isGodMode = false
        user.onTeamChanged -= ::handleTeamChanged
        user.onInteract -= ::handleInteract
        EventBus.onTeamSelectorModeChanged -= ::handleTeamSelectorModeChanged
    }

    override fun onDeath(deathContext: PlayerDeathContext) {
        player.teleport(GameManager.instance.activeMap.spawns[Team.Spectator]!!.random())
    }

    override fun onInventoryReset() {
        if (user.team == Team.Spectator && joinMode.isJoinable) {
            player.inventory.setItem(0, TeamSelector.teamSelectorItem)
        }

        if(!config.gameConfig.tournamentMode.enabled) {
            player.inventory.setItem(4, ProfileInterface.makeProfileItem(player))
            player.inventory.setItem(8, MapSelector.mapSelectorItem)
        }

        player.inventory.setItem(7, SettingsInterface.settingsItem)
    }

    private fun handleTeamChanged(old: Team, new: Team) {
        user.resetInventory()
    }

    private fun handleInteract(event: PlayerInteractEvent) {
        event.isCancelled = true
    }

    private fun handleTeamSelectorModeChanged(mode: TeamSelectMode) {
        modeChangedMsg(mode)
        user.resetInventory()
    }

    private fun modeChangedMsg(newMode: TeamSelectMode) {
        if (newMode == TeamSelectMode.TempDisable) {
            player.sendMessage(Textial.bc.format("&wJoining has been temporarily disabled, please wait &p1 minute&w for it to enable again"))
        } else if (joinMode == TeamSelectMode.TempDisable && newMode.isJoinable) {
            player.sendMessage(Textial.bc.format("Joining has been enabled again"))
        }
        joinMode = newMode
    }

}

