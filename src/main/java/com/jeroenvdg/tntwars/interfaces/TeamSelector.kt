package com.jeroenvdg.tntwars.interfaces

import com.jeroenvdg.minigame_utilities.Soundial
import com.jeroenvdg.minigame_utilities.Textial
import com.jeroenvdg.minigame_utilities.gui.guibuilders.HopperMenu
import com.jeroenvdg.minigame_utilities.gui.guibuilders.IMenu
import com.jeroenvdg.minigame_utilities.gui.player
import com.jeroenvdg.minigame_utilities.gui.slots.addButton
import com.jeroenvdg.minigame_utilities.makeItem
import com.jeroenvdg.tntwars.TNTWars
import com.jeroenvdg.tntwars.game.GameManager
import com.jeroenvdg.tntwars.game.Team
import com.jeroenvdg.tntwars.game.TeamSelectMode
import com.jeroenvdg.tntwars.listeners.GenericItemListener
import com.jeroenvdg.tntwars.player.PlayerManager
import com.jeroenvdg.tntwars.services.userIdentifier.UserIdentifier
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import kotlin.math.abs

class TeamSelector : IPlayerGUI {

    companion object : GUISingleton<TeamSelector>("TeamSelector") {
        val config get() = TNTWars.instance.config;

        val teamSelectorItem = makeItem(Material.RECOVERY_COMPASS) {
            named("&aTeams")
            setLore("Click to open the team selector")
            withPersistentData(GenericItemListener.guiKey, guiName)
            withPersistentData(GenericItemListener.movableKey, PersistentDataType.BOOLEAN, false)
            withPersistentData(GenericItemListener.droppableKey, PersistentDataType.BOOLEAN, false)
        }
    }

    override val name get() = guiName

    val teamSelectPreferences = HashMap<UserIdentifier, Team>()

    private lateinit var menu: IMenu

    private val redItem = makeItem(Material.RED_BANNER) {
        named("&cRed Team")
        setLore("Click to select this team")
    }

    private val blueItem = makeItem(Material.BLUE_BANNER) {
        named("&9Blue Team")
        setLore("Click to select this team")
    }

    private val autoItem = makeItem(Material.CLOCK) {
        named("&aAuto Team")
        setLore("Click to select a random team")
    }

    private fun makeDisabledItem(nameProvider: () -> String): ItemStack {
        return makeItem(Material.BARRIER) {
            named(nameProvider())
            setLore("&7Disabled in tournament mode")
        }
    }


    override fun open(player: Player) {
        menu.open(player)
        Soundial.play(player, Soundial.UIOpen)
    }

    override fun create() {
        teamSelectPreferences.clear()
        menu = HopperMenu("Pick a team") {
            if(config.gameConfig.tournamentMode.enabled) {
                addButton(0) {
                    displayItem = makeDisabledItem { "&9Blue Team" }
                }
            } else {
                addButton(0) {
                    displayItem = blueItem
                    onClick { event -> teamButtonClickedHandler(event, Team.Blue) }
                }
            }

            addButton(2) {
                displayItem = autoItem
                onClick { event ->
                    val player = event.player
                    val user = TNTWars.instance.playerManager.get(player) ?: return@onClick

                    when (GameManager.instance.teamSelectMode) {
                        TeamSelectMode.Queue -> {
                            user.team = Team.Queue
                            player.sendMessage(Textial.info("&eYou have joined the queue"))
                            Soundial.play(player, Soundial.Success)
                        }
                        TeamSelectMode.Join -> {
                            if (user.team.isSpectatorTeam) {
                                GameManager.instance.addPlayerToRandomTeam(user)
                                player.sendMessage(Textial.info("&eYou have joined the &${user.team.primaryColor.char}${user.team.name}&e team"))
                                Soundial.play(player, Soundial.Success)
                            } else {
                                val usersInRed = Team.Red.usersInTeam().size
                                val usersInBlue = Team.Blue.usersInTeam().size
                                if (abs(usersInRed - usersInBlue) <= 1) {
                                    player.sendMessage(Textial.info.parse("&cYou cannot switch to this team right now"))
                                    Soundial.play(player, Soundial.UIFail)
                                    return@onClick
                                }

                                val otherTeam = if (user.team == Team.Blue) Team.Red else Team.Blue
                                user.team = otherTeam
                                player.sendMessage(Textial.info.parse("&eYou have swapped to the &${user.team.primaryColor.char}${user.team.name}&e team"))
                            }
                        }
                        TeamSelectMode.None, TeamSelectMode.TempDisable -> {
                            player.sendMessage(Textial.info.parse("&aHow did you even open this menu at this stage?"))
                            Soundial.play(player, Soundial.UIFail)
                        }
                    }

                    player.closeInventory()
                }
            }

            if(config.gameConfig.tournamentMode.enabled) {
                addButton(4) {
                    displayItem = makeDisabledItem { "&cRed Team" }
                }
            } else {
                addButton(4) {
                    displayItem = redItem
                    onClick { event -> teamButtonClickedHandler(event, Team.Red) }
                }
            }
        }
    }

    private fun teamButtonClickedHandler(event: InventoryClickEvent, team: Team) {
        val player = event.player
        if (!player.hasPermission("tntwars.teamselect")) {
            player.sendMessage(Textial.msg.parse("&cA donator rank is required for team selection"))
            Soundial.play(player, Soundial.UIFail)
            return
        }

        val user = PlayerManager.instance.get(player) ?: return
        when (GameManager.instance.teamSelectMode) {
            TeamSelectMode.Queue -> {
                user.team = Team.Queue
                teamSelectPreferences[user.identifier] = team
                player.sendMessage(Textial.info("&eYou have joined the queue with preference for team &${team.primaryColor.char}${team.name}"))
                Soundial.play(player, Soundial.Success)
            }
            TeamSelectMode.Join -> {
                if (user.team == team) {
                    player.sendMessage(Textial.msg.parse("&cYou are already in this team"))
                } else if (user.team.isGameTeam) {
                    val usersInTeam = user.team.usersInTeam()
                    val usersInTargetTeam = team.usersInTeam()
                    if (usersInTeam.size - 1 <= usersInTargetTeam.size) {
                        player.sendMessage(Textial.msg.parse("&cYou cannot switch to this team right now"))
                        Soundial.play(player, Soundial.UIFail)
                        return
                    }
                    user.team = team
                    player.sendMessage(Textial.info("&eYou have switched to the &${team.primaryColor.char}${team.name}&e team"))
                    Soundial.play(player, Soundial.Success)
                } else {
                    val usersInTargetTeam = team.usersInTeam()
                    val usersInOtherTeam = (if (team == Team.Blue) Team.Red else Team.Blue).usersInTeam()
                    if (usersInTargetTeam.size > usersInOtherTeam.size) {
                        player.sendMessage(Textial.msg.parse("&cYou cannot switch to this team right now"))
                        Soundial.play(player, Soundial.UIFail)
                        return
                    }
                    user.team = team
                    player.sendMessage(Textial.info("&eYou have joined the &${team.primaryColor.char}${team.name}&e team"))
                    Soundial.play(player, Soundial.Success)
                }
            }
            TeamSelectMode.None, TeamSelectMode.TempDisable -> {
                player.sendMessage(Textial.msg.parse("&aHow did you even open this menu at this stage?"))
                Soundial.play(player, Soundial.UIFail)
            }
        }
    }
}