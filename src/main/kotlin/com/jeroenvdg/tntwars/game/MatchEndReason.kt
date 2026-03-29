package com.jeroenvdg.tntwars.game

import com.jeroenvdg.minigame_utilities.Textial
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
        message = tieComponent.append(Component.text(" - There aren't enough players to continue the match").color(Textial.Gray.color)),
        title = tieComponent,
        subtitle = Component.text("there aren't enough players to continue the match").color(Textial.Gray.color)
    ),
    RedTeamWon(
        teamThatWon = Team.Red,
        message = Component.text("The red team has won the match").color(Textial.Red.color),
        title = Component.text("The red team won!").color(Textial.Red.color),
        subtitle = Component.empty()
    ),
    BlueTeamWon(
        teamThatWon = Team.Blue,
        message = Component.text("The blue team has won the match").color(Textial.Blue.color),
        title = Component.text("The blue team won!").color(Textial.Blue.color),
        subtitle = Component.empty()
    ),
}