package com.jeroenvdg.tntwars.game

import com.jeroenvdg.minigame_utilities.Textial
import com.jeroenvdg.minigame_utilities.Textial.Companion.deserialize
import net.kyori.adventure.text.Component

private val tieComponent = Component.text("The game ended in a tie").color(Textial.Gold.color)

enum class MatchEndReason(val teamThatWon: Team?, val message: Component, val title: Component, val subtitle: Component) {
    TimeLimitReached(
        teamThatWon = null,
        message = tieComponent.append(Component.text(" - The time limit has been reached").color(Textial.Gray.color)),
        title = tieComponent,
        subtitle = Component.text("The time limit has been reached").color(Textial.Gray.color)
    ),
    StaffInterference(
        teamThatWon = null,
        message = tieComponent.append(Component.text(" - A staff member ended the match").color(Textial.Gray.color)),
        title = tieComponent,
        subtitle = Component.text("A staff member ended the match").color(Textial.Gray.color)
    ),
    NotEnoughPlayers(
        teamThatWon = null,
        message = tieComponent.append(Component.text(" - Not enough players to continue the match").color(Textial.Gray.color)),
        title = tieComponent,
        subtitle = Component.text("Not enough players").color(Textial.Red.color)
    ),
    RedTeamWon(
        teamThatWon = Team.Red,
        message = Textial.deserialize("&cRed Team &ahas won!"),
        title = deserialize("&6\uD83E\uDDE8 &c&lRed Team Won &6\uD83E\uDDE8"),
        subtitle = deserialize("&7Better luck next time, blue team")
    ),
    BlueTeamWon(
        teamThatWon = Team.Blue,
        message = Textial.deserialize("&9Blue Team &ahas won!"),
        title = deserialize("&6\uD83E\uDDE8 &9&lBlue Team Won &6\uD83E\uDDE8"),
        subtitle = deserialize("&7Better luck next time, red team")
    ),
}