package com.jeroenvdg.tntwars.commands

import com.jeroenvdg.minigame_utilities.Debug
import com.jeroenvdg.minigame_utilities.commands.CommandData
import com.jeroenvdg.minigame_utilities.commands.CommandError
import com.jeroenvdg.minigame_utilities.commands.CommandHandler
import com.jeroenvdg.minigame_utilities.commands.builders.CommandBuilder
import com.jeroenvdg.tntwars.EventBus
import com.jeroenvdg.tntwars.InfluenceType
import com.jeroenvdg.tntwars.TNTWars
import com.jeroenvdg.tntwars.commands.parameters.mapParam
import com.jeroenvdg.tntwars.commands.parameters.teamParam
import com.jeroenvdg.tntwars.commands.parameters.userParam
import com.jeroenvdg.tntwars.game.GameManager
import com.jeroenvdg.tntwars.game.MatchEndReason
import com.jeroenvdg.tntwars.game.Team
import com.jeroenvdg.tntwars.listeners.BlockOwnershipManager
import com.jeroenvdg.tntwars.managers.mapManager.TNTWarsMap
import com.jeroenvdg.tntwars.player.TNTWarsPlayer
import com.jeroenvdg.tntwars.player.PlayerManager
import org.bukkit.GameMode
import org.bukkit.entity.Player

class GameCommand : CommandHandler() {

    init {
        builder(CommandBuilder("game") {
            helpCommand()
            subCommand("start") {
                permissions("tntwars.game.admin")
                execute(::start)
            }

            subCommand("stop") {
                permissions("tntwars.game.admin")
                execute(::stop)
            }

            subCommand("leave") {
                permissions("tntwars.game.admin")
                execute(::leaveSystem)
            }

            subCommand("join") {
                permissions("tntwars.game.admin")
                execute(::joinSystem)
            }

            subCommand("setTeam") {
                permissions("tntwars.game.admin")
                userParam("User", true, allowAll = true)
                teamParam("Team", true)
                execute(::setUserTeam)
            }

            subCommand("loadMap") {
                permissions("tntwars.game.admin")
                mapParam("Map", false, mustBeEnabled = true)
                execute(::loadMap)
            }

            subCommand("loadRandomMap") {
                permissions("tntwars.game.admin")
                execute(::loadRandomMap)
            }

            subCommand("setTNTStrength") {
                permissions("tntwars.game.admin")
                floatParam("Strength", true)
                execute { data, sender ->
                    val strength = data.getParam<Float>("strength")
                    if (strength < 0) {
                        GameManager.instance.activeMap.tntStrength = -1f
                    } else {
                        GameManager.instance.activeMap.tntStrength = strength
                    }
                    sender.sendMessage(data.format("The tnt explosion strength has been overridden to &p${GameManager.instance.activeMap.tntStrength}"))
                }
            }

            subCommand("setFuseTicks") {
                permissions("tntwars.game.admin")
                intParam("Ticks", true)
                execute { data, sender ->
                    val ticks = data.getParam<Int>("Ticks")
                    if (ticks < 0) {
                        GameManager.instance.activeMap.fuseTicks = -1
                    } else {
                        GameManager.instance.activeMap.fuseTicks = ticks
                    }
                    sender.sendMessage(data.format("The tnt fuse ticks has been overridden to &p${GameManager.instance.activeMap.fuseTicks}"))
                }
            }

            subCommand("setTntCount") {
                permissions("tntwars.game.admin")
                intParam("Ticks", true)
                execute { data, sender ->
                    val count = data.getParam<Int>("Count")
                    if (count < 0) {
                        GameManager.instance.activeMap.tntCount = -1
                    } else {
                        GameManager.instance.activeMap.tntCount = count
                    }
                    sender.sendMessage(data.format("The tnt count has been overridden to &p${GameManager.instance.activeMap.tntCount}"))
                }
            }

            subCommand("giveCoins") {
                permissions("tntwars.game.admin")
                intParam("Amount", true, min = 1)
                playerParam("Player", false, online = true)
                execute { data, sender ->
                    val target = data.getParam("Player", sender)
                    val count = data.getParam<Int>("Amount")

                    val targetUser = PlayerManager.instance.get(target) ?: throw CommandError("User must be online")
                    targetUser.stats.coins += count

                    sender.sendMessage(data.format("You have given &p${target.name} &s${count}&r coins"))
                    target.sendMessage(data.format("You have been given &p${count}&r coins"))
                }
            }

            subCommand("getInspector") {
                execute(::getInspector)
            }

            subCommand("toggleBorder") {
                execute(::toggleBorder)
            }

            subCommand("refreshGuis") {
                permissions("tntwars.game.admin")
                execute(::refreshGUI)
            }

            for (type in InfluenceType.entries) {
                subCommand(type.name) {
                    for (param in type.params) {
                        add(param)
                    }

                    execute { data, sender ->
                        val args = type.params.map { data.getParam<Any>(it.name) }
                        EventBus.onAdminGameInfluence.invoke(type, args)
                        sender.sendMessage(data.format("Game action executed, please note that it may not be implemented"))
                    }
                }
            }
        })
    }

    private fun start(data: CommandData, sender: Player) {
        TNTWars.instance.gameManager.startMatch()
        sender.sendMessage(data.format("Started game"))
    }

    private fun stop(data: CommandData, sender: Player) {
        TNTWars.instance.gameManager.endMatch(MatchEndReason.StaffInterference)
        sender.sendMessage(data.format("Stopped game"))
    }

    private fun leaveSystem(data: CommandData, sender: Player) {
        if (PlayerManager.instance.get(sender) == null) throw CommandError("You have not been released from the shackles of god")
        PlayerManager.instance.removePlayer(sender, false)
        sender.gameMode = GameMode.CREATIVE
        sender.inventory.clear()
        sender.sendMessage(data.format("You have been freed from the shackles of god"))
    }

    private fun joinSystem(data: CommandData, sender: Player) {
        if (PlayerManager.instance[sender.uniqueId] != null) throw CommandError("You are already shackled by god")
        PlayerManager.instance.addPlayer(sender, false)
        sender.sendMessage(data.format("The shackles of god have restrained you to the game"))
    }

    private fun setUserTeam(data: CommandData, sender: Player) {
        val team = data.getParam<Team>("Team")
        if (data.hasParam<TNTWarsPlayer>("User")) {
            val user = data.getParam<TNTWarsPlayer>("User")
            if (user.team == team) throw CommandError("&p${user.bukkitPlayer.name}&r is already in team &s${team.name}")
            user.team = team
            sender.sendMessage(data.format("Changed the team of &p${user.bukkitPlayer.name}&r to &s${team.name}"))
            return
        }

        val users = data.getParam<Collection<TNTWarsPlayer>>("User")
        for (user in users) {
            if (user.team == team) continue
            user.team = team
            sender.sendMessage(data.format("Changed the team of &p${user.bukkitPlayer.name}&r to &s${team.name}"))
        }
    }

    private fun loadMap(data: CommandData, sender: Player) {
        val map = data.getParam<TNTWarsMap>("Map")
        TNTWars.instance.gameManager.loadMap(map)
        sender.sendMessage(data.format("Loaded map &p${map.id}"))
    }

    private fun loadRandomMap(data: CommandData, sender: Player) {
        TNTWars.instance.gameManager.loadRandomMap()
        sender.sendMessage(data.format("Loaded a random map"))
    }

    private fun getInspector(data: CommandData, sender: Player) {
        sender.inventory.addItem(BlockOwnershipManager.tool)
        sender.sendMessage(data.format("Added ownership inspector tool to your inventory"))
    }

    private fun toggleBorder(data: CommandData, sender: Player) {
        val user = PlayerManager.instance.get(sender)
            ?: throw CommandError("You must obay god's will before exeuting this command")
        user.ignoreTeamBounds = !user.ignoreTeamBounds
        if (user.ignoreTeamBounds) {
            sender.sendMessage(data.format("You now ignore team bounds"))
        } else {
            sender.sendMessage(data.format("You no longer ignore team bounds"))
        }
    }

    private fun refreshGUI(data: CommandData, sender: Player) {
        try {
            TNTWars.instance.recreateGuis()
            sender.sendMessage(data.format("Refreshed guis"))
        } catch (e: Exception) {
            sender.sendMessage(data.format("Error refreshing guis, check console"))
            Debug.error(e)
        }
    }
}