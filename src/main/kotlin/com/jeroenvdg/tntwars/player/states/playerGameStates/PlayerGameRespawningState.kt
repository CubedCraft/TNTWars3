package com.jeroenvdg.tntwars.player.states.playerGameStates

import com.jeroenvdg.tntwars.misc.PlayerDeathContext
import com.jeroenvdg.tntwars.player.TNTWarsPlayer
import com.jeroenvdg.minigame_utilities.Scheduler
import com.jeroenvdg.minigame_utilities.Textial
import net.kyori.adventure.audience.Audience
import net.kyori.adventure.text.Component
import net.kyori.adventure.title.Title
import org.bukkit.GameMode
import java.time.Duration

class PlayerGameRespawningState(user: TNTWarsPlayer) : BasePlayerGameState(user) {
    override fun onActivate() {
        super.onActivate()
        player.gameMode = GameMode.SPECTATOR
        startCoroutine { respawnRoutine() }
    }

    override fun onDeactivate() {
        super.onDeactivate()
    }

    override fun onDeath(deathContext: PlayerDeathContext) {
    }

    private suspend fun respawnRoutine() {
        Scheduler.delay(1)
        for (i in 3 downTo 1) {
            val title = Title.title(
                Textial.msg.parse("Respawning in &p$i&r seconds"),
                Component.text(""),
                Title.Times.times(Duration.ofMillis(if (i == 3) 250 else 0), Duration.ofMillis(1500), Duration.ofMillis(250))
            )

            Audience.audience(player).showTitle(title)
            Scheduler.delay(25)
        }
        gameStateMachine.playerContext.onRespawn()
    }
}