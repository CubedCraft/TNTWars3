package com.jeroenvdg.tntwars

import com.jeroenvdg.minigame_utilities.CommandHelper
import com.jeroenvdg.minigame_utilities.Event0
import com.jeroenvdg.minigame_utilities.Event1
import com.jeroenvdg.minigame_utilities.Event2
import com.jeroenvdg.tntwars.commands.BoosterCommand.team
import com.jeroenvdg.tntwars.game.MatchEndReason
import com.jeroenvdg.tntwars.game.Team
import com.jeroenvdg.tntwars.game.TeamSelectMode
import com.jeroenvdg.tntwars.managers.mapManager.ActiveMap
import com.jeroenvdg.tntwars.misc.PlayerDeathContext
import com.jeroenvdg.tntwars.player.TNTWarsPlayer
import com.jeroenvdg.tntwars.player.gameContexts.IPlayerGameContext
import com.mojang.brigadier.Command
import com.mojang.brigadier.arguments.IntegerArgumentType
import com.mojang.brigadier.builder.ArgumentBuilder
import com.mojang.brigadier.builder.RequiredArgumentBuilder
import io.papermc.paper.command.brigadier.CommandSourceStack
import org.bukkit.event.Cancellable

enum class InfluenceType(val params: ArgumentBuilder<CommandSourceStack, *>) {
    setLives(team().then(
        RequiredArgumentBuilder.argument<CommandSourceStack, Int>("lives", IntegerArgumentType.integer(1, 8))
            .executes{
            ctx ->
            val team = CommandHelper.getTeam(ctx)
            val lives = IntegerArgumentType.getInteger(ctx, "lives")
            EventBus.onAdminGameInfluence.invoke(setLives, listOf(team, lives))
            return@executes Command.SINGLE_SUCCESS
        })),
    setTimer(RequiredArgumentBuilder.argument<CommandSourceStack, Int>("minutes", IntegerArgumentType.integer())
        .executes{
                ctx ->
            val minutes = IntegerArgumentType.getInteger(ctx, "minutes")
            EventBus.onAdminGameInfluence.invoke(setLives, listOf(minutes))
            return@executes Command.SINGLE_SUCCESS
        })
}

class EventBus {
    companion object {

        val onPlayerJoined = Event1<TNTWarsPlayer>()
        val onPlayerLeft = Event1<TNTWarsPlayer>()
        val onPlayerTeamChanged = Event2<TNTWarsPlayer, Team>()
        val onPlayerDeath = Event1<PlayerDeathContext>()
        val onTeamSelectorModeChanged = Event1<TeamSelectMode>()
        val onTNTSpawnEvent = Event1<TNTSpawnEvent>()
        val onMapChanged = Event1<ActiveMap>()

        val onMatchStarted = Event0()
        val onPlayerGameContextProviderChanged = Event1<IPlayerGameContext.IProvider>()
        val onMatchEnded = Event1<MatchEndReason>()
        val onFlawlessMatchEnded = Event1<Team>()
        val onUserVanishChanged = Event1<TNTWarsPlayer>()

        val onAdminGameInfluence = Event2<InfluenceType, List<Any>>()

        fun reset() {
            onPlayerJoined.clear()
            onPlayerLeft.clear()
            onPlayerTeamChanged.clear()
            onPlayerDeath.clear()
            onTeamSelectorModeChanged.clear()
            onUserVanishChanged.clear()
            onMapChanged.clear()

            onMatchStarted.clear()
            onMatchEnded.clear()
            onPlayerGameContextProviderChanged.clear()

            onAdminGameInfluence.clear()
        }
    }
}

data class TNTSpawnEvent(val team: Team?, val ownerId: String?) : Cancellable {

    private var cancelled = false

    override fun isCancelled() = cancelled
    override fun setCancelled(cancelled: Boolean) {
        this.cancelled = cancelled
    }
}