package com.jeroenvdg.tntwars.commands

import com.jeroenvdg.minigame_utilities.*
import com.jeroenvdg.minigame_utilities.commands.*
import com.jeroenvdg.minigame_utilities.commands.builders.CommandBuilder
import com.jeroenvdg.minigame_utilities.commands.builders.SingleCommandBuilder
import com.jeroenvdg.minigame_utilities.commands.builders.params.CommandValidator
import com.jeroenvdg.minigame_utilities.commands.builders.setCommand
import com.jeroenvdg.tntwars.commands.parameters.mapParam
import com.jeroenvdg.tntwars.commands.parameters.teamParam
import com.jeroenvdg.tntwars.game.Team
import com.jeroenvdg.tntwars.managers.mapManager.MapManager
import com.jeroenvdg.tntwars.managers.mapManager.MapRegion
import com.jeroenvdg.tntwars.managers.mapManager.RegionType
import com.jeroenvdg.tntwars.managers.mapManager.TNTWarsMap
import com.sk89q.worldedit.WorldEdit
import com.sk89q.worldedit.bukkit.BukkitAdapter
import com.sk89q.worldedit.regions.CuboidRegion
import net.kyori.adventure.text.Component
import org.bukkit.GameRule
import org.bukkit.GameRules
import org.bukkit.Material
import org.bukkit.entity.Player
import java.util.*
import kotlin.math.round

class MapManagerCommand(val mapManager: MapManager) : CommandHandler() {

    init {
        val mapProvider = object : CommandValidator(UUID.randomUUID().toString().replace("-"," "), true, { data, sender ->
            data.setParam("Map", mapManager.find { it.managedWorld.worldName == sender.world.name })
        }) {}

        builder(CommandBuilder("mapmanager") {
            validator(mapProvider)
            helpCommand()
            subCommand("load") { execute(::loadMaps) }
            subCommand("save") { execute(::saveMaps) }

            subCommand("create") {
                stringParam("Name", true, Regex("[a-zA-Z ]{3,24}"), "Please enter a name between 3 and 24 characters")
                execute(::createMap)
            }

            subCommand("list") {
                intParam("Page", false, min = 1)
                validator { data, sender -> if (!data.hasParam<Int>("Page")) data.setParam("Page", 1)}
                execute(::listMaps)
            }

            subCommand("info") {
                mapParam("Map", false)
                hasMapValidator()
                execute(::infoMap)
            }

            subCommand("setMaterial") {
                hasMapValidator()
                enumParam<Material>("Material", true)
                execute(::setMaterial)
            }

            subCommand("setCreator") {
                hasMapValidator()
                stringParam("Creator", true)
                execute(::setCreator)
            }

            subCommand("setGameModeName") {
                hasMapValidator()
                stringParam("Name", true)
                execute(::setGameModeName)
            }

            subCommand("setEnabled") {
                hasMapValidator()
                boolParam("Enabled", true)
                execute(::enableMap)
            }

            subCommand("setExperimental") {
                hasMapValidator()
                boolParam("Experimental", true)
                execute(::setMapExperimental)
            }

            subCommand("tp") {
                mapParam("Map", true)
                execute(::tpMap)
            }

            subCommand("setTeamRegion") {
                hasMapValidator()
                teamParam("Team", true, includeSpectatorTeam = true)
                execute(::setMapTeamRegion)
            }

            subCommand("setVoidHeight") {
                hasMapValidator()
                intParam("Height", true)
                execute(::setVoidHeight)
            }

            subCommand("setTntStrength") {
                hasMapValidator()
                floatParam("Strength", true)
                execute(::setTNTStrength)
            }

            subCommand("setTntCount") {
                hasMapValidator()
                intParam("Count", true)
                execute(::setTNTCount)
            }

            subCommand("setFuseTicks") {
                hasMapValidator()
                intParam("Ticks", true)
                execute(::setFuseTicks)
            }

            subCommand("gracePeriod") {
                hasMapValidator()
                floatParam("Minutes", true)
                execute(::setGracePeriod)
            }

            subCommand("applyGamerules") {
                hasMapValidator()
                execute(::applyDefaultGameRules)
            }
            
            commandGroup("regions") {
                hasMapValidator()
                helpCommand()
                subCommand("list") {
                    execute(::listMapRegions)
                }
                subCommand("add") {
                    enumParam<RegionType>("Type", true)
                    stringParam("Name", true, Regex("[a-zA-Z_0-9]{3,24}"), "Please enter a name between 3 and 24 characters")
                    validator { data, sender ->
                        val map = data.getParam<TNTWarsMap>("Map")
                        val name = data.getParam<String>("Name").lowercase()
                        if (map.regions.any { it.name == name }) {
                            throw CommandError("Region with name &p$name&r already exists")
                        }
                    }
                    execute(::addMapRegion)
                }
                subCommand("remove") {
                    optionsParam("Name", true) { data -> data.getParam<TNTWarsMap>("Map").regions.map { it.name }}
                    execute(::removeMapRegion)
                }
            }

            commandGroup("spawn") {
                hasMapValidator()
                helpCommand()
                subCommand("list") {
                    teamParam("Team", false)
                    execute(::listSpawns)
                }

                subCommand("tp") {
                    spawnParam("Spawn", true)
                    execute(::tpSpawn)
                }

                subCommand("add") {
                    teamParam("Team", true)
                    execute(::addSpawn)
                }

                subCommand("remove") {
                    spawnParam("Spawn", true)
                    execute(::removeSpawn)
                }
            }
        })
    }

    private fun SingleCommandBuilder.spawnParam(name: String, required: Boolean) {
        optionsParam(name, required) { data ->
            val map = data.getParam<TNTWarsMap>("Map")
            return@optionsParam map.teams.flatMap { it.value.spawnLikeList.withIndex().map { spawn -> "${it.key}:${spawn.index}" } }
        }
    }

    private fun SingleCommandBuilder.hasMapValidator() {
        validator { data, _ ->
            if (!data.hasParam<TNTWarsMap>("Map")) {
                throw CommandError("You are not standing in an editable map")
            }
        }
    }

    private fun CommandBuilder.hasMapValidator() {
        validator { data, _ ->
            if (!data.hasParam<TNTWarsMap>("Map")) {
                throw CommandError("You are not standing in an editable map")
            }
        }
    }

    private fun loadMaps(data: CommandData, sender: Player) {
        try {
            mapManager.loadAll()
            sender.sendMessage(data.format("Loaded &p${mapManager.size}&r maps"))
        } catch (e: Exception) {
            Debug.error(e)
            sender.sendMessage(data.format("Failed to load maps"))
        }
    }

    private fun saveMaps(data: CommandData, sender: Player) {
        try {
            mapManager.saveAll()
            sender.sendMessage(data.format("Saved &p${mapManager.size}&r maps"))
        } catch (e: Exception) {
            Debug.error(e)
            sender.sendMessage(data.format("Failed to save maps"))
        }
    }

    private fun createMap(data: CommandData, sender: Player) {
        val name = data.getParam<String>("Name")
        mapManager.create(name)
        sender.sendMessage(data.format("Created &p${name}&r map"))
    }

    private fun listMaps(data: CommandData, sender: Player) {
        val page = data.getParam<Int>("Page")

        val pageData = PageData(page, 7, mapManager.size)
        if (pageData.isInvalidPage) return sender.sendMessage(data.format("Invalid page: &p$page"))

        val header = parse("Available maps:").toBuilder()
        if (pageData.hasPages) header.append(Component.text("  ").append(pageData.pageTextComponent("/$name list _index_")))

        sender.sendMessage(paginate(header.build(), data.textial, pageData) {
            val map = mapManager[it]
            val color = if (map.enabled) 'a' else if (map.isReady()) '6' else 'c'
            return@paginate parse("&$color${map.name} &8(&7${map.id}&8)").setCommand("/$name info ${map.id}")
        })
    }

    private fun infoMap(data: CommandData, sender: Player) {
        val map = data.getParam<TNTWarsMap>("Map")
        val builder = Component.text()

        map.managedWorld.load()
        if (map.managedWorld.world != sender.world) {
            sender.teleport(map.managedWorld.world!!.spawnLocation)
        }

        builder.append(Textial.line).appendNewline()
        builder.append(data.format("Info for map &p${map.name} &8(&7${map.id}&8)  ")).append(data.parse("&7&lREFRESH").setCommand("/${data.originalCommand}")).appendNewline()
        builder.append(data.format("creator: &f${map.creator}")).appendNewline()
        builder.append(data.format("material: &3${map.itemMaterial}")).appendNewline()
        builder.append(data.format("gamemode: &3${map.gamemodeName}")).appendNewline()

        builder.append(data.format("enabled: ${if (map.enabled) "&aYes" else if (map.isReady()) "&6No" else "&cNot Ready"}")).append(Component.text("  "))
        if (map.isReady()) {
            builder.append(data.parse(if (map.enabled) "&c&lDISABLE" else "&a&lENABLE").setCommand("/$name setenabled ${!map.enabled}"))
        }
        builder.appendNewline()

        builder.append(data.format("experimental: ${if (map.isExperimental) "&c&lYes" else "&a&lNo"}")).appendNewline()
        builder.append(data.format("void height: &d${map.voidHeight}")).appendNewline()
        builder.append(data.format("tnt strength: &c${map.tntStrength}")).appendNewline()
        builder.append(data.format("tnt count: &c${map.tntCount}")).appendNewline()
        builder.append(data.format("fuse ticks: &c${map.fuseTicks}")).appendNewline()
        builder.append(data.format("grace period: &f${map.gracePeriodTicks / 20f / 60f}&r Minutes (&s${map.gracePeriodTicks}&r Ticks)")).appendNewline()

        builder.append(data.format("spawns:  "))
        for (team in map.teams) {
            builder.append(data.parse("&${team.key.primaryColor.char}${team.key.name}:&6${team.value.spawnLikeList.size}  "))
        }
        builder.append(data.parse("&7&lVIEW").setCommand("/$name spawn list")).appendNewline()

        builder.append(data.format("blue region: ${if (map.teams[Team.Blue]!!.teamRegion == null) "&cUnset" else "&aSet"}")).appendNewline()
        builder.append(data.format("red region: ${if (map.teams[Team.Red]!!.teamRegion == null) "&cUnset" else "&aSet"}")).appendNewline()

        builder.append(data.format("regions: &f${map.regions.size}&r  ").append(data.parse("&7&lView").setCommand("/$name regions list"))).appendNewline()

        builder.append(Textial.line)
        sender.sendMessage(builder)
    }

    private fun setMaterial(data: CommandData, sender: Player) {
        val map = data.getParam<TNTWarsMap>("Map")
        val material = data.getParam<Material>("material")

        map.itemMaterial = material
        sender.sendMessage(data.format("Changed material to &p$material"))
        map.saveToConfig()
    }

    private fun setCreator(data: CommandData, sender: Player) {
        val map = data.getParam<TNTWarsMap>("Map")
        val newCreator = data.getParam<String>("Creator")

        map.creator = newCreator
        sender.sendMessage(data.format("Changed creator to &p$newCreator"))
        map.saveToConfig()
    }

    private fun setGameModeName(data: CommandData, sender: Player) {
        val map = data.getParam<TNTWarsMap>("Map")
        val newName = data.getParam<String>("Name")

        map.gamemodeName = newName
        sender.sendMessage(data.format("Changed gamemode name to &p$newName"))
        map.saveToConfig()
    }

    private fun enableMap(data: CommandData, sender: Player) {
        val map = data.getParam<TNTWarsMap>("Map")
        val newValue = data.getParam<Boolean>("Enabled")

        if (!map.isReady()) {
            map.enabled =false
            sender.sendMessage(data.format("Map &p${map.name}&r is not ready!"))
        } else if (map.enabled == newValue) {
            sender.sendMessage(data.format("Map &p${map.name}&r is already &s${if (map.enabled) "enabled" else "disabled" }"))
        } else {
            map.enabled = newValue
            sender.sendMessage(data.format("&s${if (map.enabled) "Enabled" else "Disabled"}&r map &p${map.name}"))
        }
        map.saveToConfig()
    }

    private fun setMapExperimental(data: CommandData, sender: Player) {
        val map = data.getParam<TNTWarsMap>("Map")
        val isExperimental = data.getParam<Boolean>("Experimental")

        map.isExperimental = isExperimental
        sender.sendMessage(data.format("Changed experimental status to &p$isExperimental"))
        map.saveToConfig()
    }

    private fun tpMap(data: CommandData, sender: Player) {
        val map = data.getParam<TNTWarsMap>("Map")
        map.managedWorld.load()
        sender.teleport(map.managedWorld.world!!.spawnLocation)
    }

    private fun listSpawns(data: CommandData, sender: Player) {
        val map = data.getParam<TNTWarsMap>("Map")

        val builder = Component.text()
        builder.append(Textial.line).appendNewline()
        builder.append(data.format("Spawns for &p${map.name}").setCommand("/$name info ${map.id}")).append(Component.text("  ")).append(data.parse("&7&lREFRESH").setCommand("/$name spawn list")) .appendNewline()

        for (team in map.teams) {
            for (spawn in team.value.spawnLikeList.withIndex()) {
                val comp = Component.text()
                comp.append(data.format("&7- &${team.key.primaryColor.char}${team.key}&8:&s${spawn.index}  &a&lTP").setCommand("/$name spawn tp ${team.key}:${spawn.index}"))
                comp.append(Component.text("  "))
                comp.append(data.parse("&c&lDEL")).setCommand("/$name spawn remove ${team.key}:${spawn.index}")
                builder.append(comp).appendNewline()
            }
        }

        builder.append(data.format("Add for team"))
        for (team in map.teams) {
            builder.append(Component.text("  "))
            builder.append(data.parse("&${team.key.primaryColor.char}&l${team.key.name.uppercase().take(4)}").setCommand("/$name spawn add ${team.key}"))
        }
        builder.appendNewline()
        builder.append(Textial.line)
        sender.sendMessage(builder)
    }

    private fun tpSpawn(data: CommandData, sender: Player) {
        val map = data.getParam<TNTWarsMap>("Map")
        val spawn = data.getParam<String>("Spawn").split(":")
        val team = parseEnum<Team>(spawn[0])
        val index = spawn[1].toInt()

        val teamSpawns = map.teams[team]!!.spawnLikeList
        val location = teamSpawns[index].toLocation(map.managedWorld.world!!)

        sender.teleport(location)
        sender.sendMessage(data.format("You have been teleported to spawn &p${spawn.joinToString(":")}"))
    }

    private fun addSpawn(data: CommandData, sender: Player) {
        val map = data.getParam<TNTWarsMap>("Map")
        val team = data.getParam<Team>("Team")
        val location = sender.location.round()

        if (location.world != map.managedWorld.world) {
            throw CommandError("You must be in the same world as the map")
        }

        val teamSpawns = map.teams[team]!!.spawnLikeList
        teamSpawns.add(LocationLike(location))
        map.saveToConfig()
        sender.sendMessage(data.format("Added new spawn for team &s${team.name}"))
    }

    private fun removeSpawn(data: CommandData, sender: Player) {
        val map = data.getParam<TNTWarsMap>("Map")
        val spawn = data.getParam<String>("Spawn").split(":")
        val team = parseEnum<Team>(spawn[0])
        val index = spawn[1].toInt()

        val teamSpawns = map.teams[team]!!.spawnLikeList
        teamSpawns.removeAt(index)
        sender.sendMessage(data.format("Removed spawn &p${spawn.joinToString(":")}"))
    }

    private fun setMapTeamRegion(data: CommandData, sender: Player) {
        val map = data.getParam<TNTWarsMap>("Map")
        val team = data.getParam<Team>("Team")

        val wePlayer = BukkitAdapter.adapt(sender)
        val region = (WorldEdit.getInstance().sessionManager.get(wePlayer).getRegionSelector(wePlayer.world).region as CuboidRegion).clone()

        map.teams[team]!!.teamRegion = region
        map.saveToConfig()
        sender.sendMessage(data.format("Set team region for team &${team.primaryColor.char}${team.name}"))

    }

    private fun setVoidHeight(data: CommandData, sender: Player) {
        val map = data.getParam<TNTWarsMap>("Map")
        val newHeight = data.getParam<Int>("Height")

        map.voidHeight = newHeight
        map.saveToConfig()
        sender.sendMessage(data.format("Set void height to &p$newHeight"))
    }

    private fun setTNTStrength(data: CommandData, sender: Player) {
        val map = data.getParam<TNTWarsMap>("Map")
        val newStrength = data.getParam<Float>("Strength")

        map.tntStrength = newStrength
        map.saveToConfig()
        sender.sendMessage(data.format("Set strength to &p$newStrength"))
    }

    private fun setTNTCount(data: CommandData, sender: Player) {
        val map = data.getParam<TNTWarsMap>("Map")
        val newCount = data.getParam<Int>("Count")

        map.tntCount = newCount
        map.saveToConfig()
        sender.sendMessage(data.format("Set count to &p$newCount"))
    }

    private fun setFuseTicks(data: CommandData, sender: Player) {
        val map = data.getParam<TNTWarsMap>("Map")
        val newTicks = data.getParam<Int>("Ticks")

        map.fuseTicks = newTicks
        map.saveToConfig()
        sender.sendMessage(data.format("Set fuse ticks to &p$newTicks"))
    }

    private fun setGracePeriod(data: CommandData, sender: Player) {
        val map = data.getParam<TNTWarsMap>("Map")
        val minutes = data.getParam<Float>("Minutes")
        val ticks = round(minutes * (60.0 * 20.0)).toInt()

        map.gracePeriodTicks = ticks
        map.saveToConfig()
        sender.sendMessage(data.format("Set grace period to &p$minutes&r minutes (&s$ticks&r Ticks)"))
    }

    private fun applyDefaultGameRules(data: CommandData, sender: Player) {
        val map = data.getParam<TNTWarsMap>("Map")
        val world = map.managedWorld.world ?: throw CommandError("World is not loaded!")

        world.setGameRule(GameRules.COMMAND_BLOCK_OUTPUT, false)
        world.setGameRule(GameRules.ADVANCE_TIME, false)
        world.setGameRule(GameRules.ADVANCE_WEATHER, false)
        world.setGameRule(GameRules.SHOW_ADVANCEMENT_MESSAGES, false)
        world.setGameRule(GameRules.RAIDS, false)
        world.setGameRule(GameRules.ENTITY_DROPS, false)
        world.setGameRule(GameRules.IMMEDIATE_RESPAWN, true)
        world.setGameRule(GameRules.SPAWN_MOBS, false)
        world.setGameRule(GameRules.SPAWN_PATROLS, false)
        world.setGameRule(GameRules.SPAWN_WANDERING_TRADERS, false)
        world.setGameRule(GameRules.SPAWN_WARDENS, false)
        world.setGameRule(GameRules.SPREAD_VINES, false)
        world.setGameRule(GameRules.RESPAWN_RADIUS, 0)
        world.setGameRule(GameRules.BLOCK_DROPS, false)
        world.setGameRule(GameRules.KEEP_INVENTORY, true)
        world.setGameRule(GameRules.SHOW_DEATH_MESSAGES, false)
        world.setGameRule(GameRules.SPECTATORS_GENERATE_CHUNKS, false)
        world.worldBorder.size = 500.0

        sender.sendMessage(data.parse("Applied default gamerules"))
    }

    private fun listMapRegions(data: CommandData, sender: Player) {
        val map = data.getParam<TNTWarsMap>("Map")
        val builder = Component.text()
        builder.append(Textial.line).appendNewline()
        builder.append(data.format("Regions for &p${map.name}  ")).append(data.parse("&7&lREFRESH").setCommand("/${data.originalCommand}")).appendNewline()
        for (region in map.regions) {
            builder.append(data.format("&7- &s${region.name}")).append(Component.text("  ")).append(data.parse("&c&lDEL").setCommand("/$name regions remove ${region.name}")).appendNewline()
        }
        builder.append(Textial.line).appendNewline()
        sender.sendMessage(builder)
    }

    private fun addMapRegion(data: CommandData, sender: Player) {
        val map = data.getParam<TNTWarsMap>("Map")
        val type = data.getParam<RegionType>("Type")
        val name = data.getParam<String>("Name")

        val region = MapRegion(name)
        val wePlayer = BukkitAdapter.adapt(sender)
        val selection = WorldEdit.getInstance().sessionManager.get(wePlayer).getRegionSelector(wePlayer.world).region as CuboidRegion

        region.type = type
        region.region = CuboidRegion.makeCuboid(selection)
        map.regions.add(region)
        map.saveToConfig()
        sender.sendMessage(data.format("Added region &p${region.name}"))
    }

    private fun removeMapRegion(data: CommandData, sender: Player) {
        val map = data.getParam<TNTWarsMap>("Map")
        val name = data.getParam<String>("Name")
        map.saveToConfig()
        if (map.regions.removeIf { it.name == name }) {
            sender.sendMessage(data.format("Removed region &p$name"))
        } else {
            sender.sendMessage(data.format("Region &p$name&r not found"))
        }
    }
}