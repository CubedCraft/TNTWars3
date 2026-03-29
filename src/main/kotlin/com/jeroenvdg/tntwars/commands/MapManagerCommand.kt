package com.jeroenvdg.tntwars.commands

import com.jeroenvdg.minigame_utilities.*
import com.jeroenvdg.minigame_utilities.commands.*
import com.jeroenvdg.minigame_utilities.commands.builders.setCommand
import com.jeroenvdg.tntwars.game.Team
import com.jeroenvdg.tntwars.managers.mapManager.MapManager
import com.jeroenvdg.tntwars.managers.mapManager.MapRegion
import com.jeroenvdg.tntwars.managers.mapManager.RegionType
import com.mojang.brigadier.Command
import com.mojang.brigadier.arguments.BoolArgumentType
import com.mojang.brigadier.arguments.FloatArgumentType
import com.mojang.brigadier.arguments.IntegerArgumentType
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.builder.RequiredArgumentBuilder
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.exceptions.CommandSyntaxException
import com.sk89q.worldedit.WorldEdit
import com.sk89q.worldedit.bukkit.BukkitAdapter
import com.sk89q.worldedit.regions.CuboidRegion
import io.papermc.paper.command.brigadier.CommandSourceStack
import io.papermc.paper.command.brigadier.Commands
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.GameRules
import org.bukkit.Material
import org.bukkit.entity.Player
import java.util.*
import kotlin.math.round

class MapManagerCommand(val mapManager: MapManager) : BrigadierCommand {

    private fun loadMaps(ctx: CommandContext<CommandSourceStack>): Int {
        val player = CommandHelper.getPlayer(ctx) ?: return Command.SINGLE_SUCCESS
        try {
            mapManager.loadAll()
            player.sendMessage(TextHelper.deserialize("Loaded &p${mapManager.size}&r maps"))
        } catch (e: Exception) {
            Debug.error(e)
            player.sendMessage(TextHelper.deserialize("Failed to load maps"))
        }
        return Command.SINGLE_SUCCESS
    }

    private fun saveMaps(ctx: CommandContext<CommandSourceStack>): Int {
        val player = ctx.source.sender
        if(player !is Player) return Command.SINGLE_SUCCESS
        try {
            mapManager.saveAll()
            player.sendMessage(TextHelper.deserialize("Saved &p${mapManager.size}&r maps"))
        } catch (e: Exception) {
            Debug.error(e)
            player.sendMessage(TextHelper.deserialize("Failed to save maps"))
        }
        return Command.SINGLE_SUCCESS
    }

    private fun createMap(ctx: CommandContext<CommandSourceStack>): Int {
        val player = ctx.source.sender
        if(player !is Player) return Command.SINGLE_SUCCESS
        val name = try{StringArgumentType.getString(ctx, "name")}catch(e: IllegalArgumentException) {null}
        if(name == null) {
            player.sendMessage(TextHelper.format("&cUsage: /mapmanager create <name>"))
            return Command.SINGLE_SUCCESS
        }
        mapManager.create(name)
        player.sendMessage(TextHelper.format("Created &p${name}&r map"))
        return Command.SINGLE_SUCCESS
    }

    private fun listMaps(ctx: CommandContext<CommandSourceStack>): Int {
        val sender = ctx.source.sender
        val page = try{ IntegerArgumentType.getInteger(ctx, "page") }catch(e: IllegalArgumentException){null}
        if(page == null) {
            sender.sendMessage(TextHelper.deserialize(""))
            return Command.SINGLE_SUCCESS
        }

        val pageData = PageData(page, 7, mapManager.size)
        if (pageData.isInvalidPage) {
            sender.sendMessage(TextHelper.deserialize("Invalid page: &p$page"))
            return Command.SINGLE_SUCCESS
        }

        val header = TextHelper.deserialize("Available maps:")
        if (pageData.hasPages) header.append(Component.text("  ").append(pageData.pageTextComponent("/mapmanager list _index_")))

        sender.sendMessage(paginate(header, pageData) {
            val map = mapManager[it]
            val color = if (map.enabled) 'a' else if (map.isReady()) '6' else 'c'
            return@paginate TextHelper.deserialize("&$color${map.name} &8(&7${map.id}&8)").setCommand("/mapmanager info ${map.id}")
        })
        return Command.SINGLE_SUCCESS
    }

    private fun infoMap(ctx: CommandContext<CommandSourceStack>): Int {
        val sender = ctx.source.sender
        if(sender !is Player) return Command.SINGLE_SUCCESS
        val mapName = try{ StringArgumentType.getString(ctx, "map") }catch(e: IllegalArgumentException){null}
        if(mapName == null) {
            sender.sendMessage(TextHelper.deserialize(""))
            return Command.SINGLE_SUCCESS
        }

        val map = mapManager.find(mapName)
        if(map == null) {
            sender.sendMessage(TextHelper.prefixComp.append(Component.text("$mapName does not exist").color(
                NamedTextColor.RED)))
            return Command.SINGLE_SUCCESS
        }
        val builder = Component.text()

        map.managedWorld.load()
        if (map.managedWorld.world != sender.world) {
            sender.teleport(map.managedWorld.world!!.spawnLocation)
        }

        builder.append(TextHelper.line).appendNewline()
        builder.append(TextHelper.deserialize("Info for map &p${map.name} &8(&7${map.id}&8)  ")).append(TextHelper.deserialize("&7&lREFRESH").setCommand("/mapmanager")).appendNewline()
        builder.append(TextHelper.deserialize("creator: &f${map.creator}")).appendNewline()
        builder.append(TextHelper.deserialize("material: &3${map.itemMaterial}")).appendNewline()
        builder.append(TextHelper.deserialize("gamemode: &3${map.gamemodeName}")).appendNewline()

        builder.append(TextHelper.deserialize("enabled: ${if (map.enabled) "&aYes" else if (map.isReady()) "&6No" else "&cNot Ready"}")).append(Component.text("  "))
        if (map.isReady()) {
            builder.append(TextHelper.deserialize(if (map.enabled) "&c&lDISABLE" else "&a&lENABLE").setCommand("/mapmanager setenabled ${!map.enabled}"))
        }
        builder.appendNewline()

        builder.append(TextHelper.deserialize("experimental: ${if (map.isExperimental) "&c&lYes" else "&a&lNo"}")).appendNewline()
        builder.append(TextHelper.deserialize("void height: &d${map.voidHeight}")).appendNewline()
        builder.append(TextHelper.deserialize("tnt strength: &c${map.tntStrength}")).appendNewline()
        builder.append(TextHelper.deserialize("tnt count: &c${map.tntCount}")).appendNewline()
        builder.append(TextHelper.deserialize("fuse ticks: &c${map.fuseTicks}")).appendNewline()
        builder.append(TextHelper.deserialize("grace period: &f${map.gracePeriodTicks / 20f / 60f}&r Minutes (&s${map.gracePeriodTicks}&r Ticks)")).appendNewline()

        builder.append(TextHelper.deserialize("spawns:  "))
        for (team in map.teams) {
            builder.append(TextHelper.deserialize("&${team.key.primaryColor}${team.key.name}:&6${team.value.spawnLikeList.size}  "))
        }
        builder.append(TextHelper.deserialize("&7&lVIEW").setCommand("/mapmanager spawn list")).appendNewline()

        builder.append(TextHelper.deserialize("blue region: ${if (map.teams[Team.Blue]!!.teamRegion == null) "&cUnset" else "&aSet"}")).appendNewline()
        builder.append(TextHelper.deserialize("red region: ${if (map.teams[Team.Red]!!.teamRegion == null) "&cUnset" else "&aSet"}")).appendNewline()

        builder.append(TextHelper.deserialize("regions: &f${map.regions.size}&r  ").append(TextHelper.deserialize("&7&lView").setCommand("/mapmanager regions list"))).appendNewline()

        builder.append(TextHelper.line)
        sender.sendMessage(builder)
        return Command.SINGLE_SUCCESS
    }

    private fun setMaterial(ctx: CommandContext<CommandSourceStack>): Int {
        val player = CommandHelper.getPlayer(ctx) ?: return Command.SINGLE_SUCCESS
        val map = CommandHelper.getMap(ctx, mapManager) ?: return Command.SINGLE_SUCCESS
        val material = Material.matchMaterial(StringArgumentType.getString(ctx, "material"))
        if(material == null) {
            return Command.SINGLE_SUCCESS
        }

        map.itemMaterial = material
        player.sendMessage(TextHelper.format("Changed material to &p$material"))
        map.saveToConfig()
        return Command.SINGLE_SUCCESS
    }

    private fun setCreator(ctx: CommandContext<CommandSourceStack>): Int {
        val player = CommandHelper.getPlayer(ctx) ?: return Command.SINGLE_SUCCESS
        val map = CommandHelper.getMap(ctx, mapManager) ?: return Command.SINGLE_SUCCESS
        val newCreator = StringArgumentType.getString(ctx, "creator")

        map.creator = newCreator
        player.sendMessage(TextHelper.format("Changed creator to &p$newCreator"))
        map.saveToConfig()
        return Command.SINGLE_SUCCESS
    }

    private fun setGameModeName(ctx: CommandContext<CommandSourceStack>): Int {
        val player = CommandHelper.getPlayer(ctx) ?: return Command.SINGLE_SUCCESS
        val map = CommandHelper.getMap(ctx, mapManager) ?: return Command.SINGLE_SUCCESS
        val newName = StringArgumentType.getString(ctx, "name")

        map.gamemodeName = newName
        player.sendMessage(TextHelper.format("Changed gamemode name to &p$newName"))
        map.saveToConfig()
        return Command.SINGLE_SUCCESS
    }

    private fun enableMap(ctx: CommandContext<CommandSourceStack>): Int {
        val player = CommandHelper.getPlayer(ctx) ?: return Command.SINGLE_SUCCESS
        val map = CommandHelper.getMap(ctx, mapManager) ?: return Command.SINGLE_SUCCESS
        val newValue = BoolArgumentType.getBool(ctx,"enabled")

        if (!map.isReady()) {
            map.enabled =false
            player.sendMessage(TextHelper.format("Map &p${map.name}&r is not ready!"))
        } else if (map.enabled == newValue) {
            player.sendMessage(TextHelper.format("Map &p${map.name}&r is already &s${if (map.enabled) "enabled" else "disabled" }"))
        } else {
            map.enabled = newValue
            player.sendMessage(TextHelper.format("&s${if (map.enabled) "Enabled" else "Disabled"}&r map &p${map.name}"))
        }
        map.saveToConfig()
        return Command.SINGLE_SUCCESS
    }

    private fun setMapExperimental(ctx: CommandContext<CommandSourceStack>): Int {
        val player = CommandHelper.getPlayer(ctx) ?: return Command.SINGLE_SUCCESS
        val map = CommandHelper.getMap(ctx, mapManager) ?: return Command.SINGLE_SUCCESS
        val isExperimental = BoolArgumentType.getBool(ctx,"experimental")

        map.isExperimental = isExperimental
        player.sendMessage(TextHelper.format("Changed experimental status to &p$isExperimental"))
        map.saveToConfig()
        return Command.SINGLE_SUCCESS
    }

    private fun tpMap(ctx: CommandContext<CommandSourceStack>): Int {
        val player = CommandHelper.getPlayer(ctx) ?: return Command.SINGLE_SUCCESS
        val map = CommandHelper.getMap(ctx, mapManager) ?: return Command.SINGLE_SUCCESS
        map.managedWorld.load()
        player.teleport(map.managedWorld.world!!.spawnLocation)
        return Command.SINGLE_SUCCESS
    }

    private fun listSpawns(ctx: CommandContext<CommandSourceStack>): Int {
        val player = CommandHelper.getPlayer(ctx) ?: return Command.SINGLE_SUCCESS
        val map = CommandHelper.getMap(ctx, mapManager) ?: return Command.SINGLE_SUCCESS

        val builder = Component.text()
        builder.append(TextHelper.line).appendNewline()
        builder.append(TextHelper.format("Spawns for &p${map.name}").setCommand("/mapmanager info ${map.id}")).append(Component.text("  ")).append(TextHelper.deserialize("&7&lREFRESH").setCommand("/mapmanager spawn list")) .appendNewline()

        for (team in map.teams) {
            for (spawn in team.value.spawnLikeList.withIndex()) {
                val comp = Component.text()
                comp.append(TextHelper.format("&7- &${team.key.primaryColor}${team.key}&8:&s${spawn.index}  &a&lTP").setCommand("/mapmanager spawn tp ${team.key}:${spawn.index}"))
                comp.append(Component.text("  "))
                comp.append(TextHelper.deserialize("&c&lDEL")).setCommand("/mapmanager spawn remove ${team.key}:${spawn.index}")
                builder.append(comp).appendNewline()
            }
        }

        builder.append(TextHelper.format("Add for team"))
        for (team in map.teams) {
            builder.append(Component.text("  "))
            builder.append(TextHelper.deserialize("&${team.key.primaryColor}&l${team.key.name.uppercase().take(4)}").setCommand("/mapmanager spawn add ${team.key}"))
        }
        builder.appendNewline()
        builder.append(TextHelper.line)
        player.sendMessage(builder)
        return Command.SINGLE_SUCCESS
    }

    private fun tpSpawn(ctx: CommandContext<CommandSourceStack>): Int {
        val player = CommandHelper.getPlayer(ctx) ?: return Command.SINGLE_SUCCESS
        val map = CommandHelper.getMap(ctx, mapManager) ?: return Command.SINGLE_SUCCESS
        val spawn = StringArgumentType.getString(ctx, "spawn").split(":")
        val team = parseEnum<Team>(spawn[0])
        val index = spawn[1].toInt()

        val teamSpawns = map.teams[team]!!.spawnLikeList
        val location = teamSpawns[index].toLocation(map.managedWorld.world!!)

        player.teleport(location)
        player.sendMessage(TextHelper.format("You have been teleported to spawn &p${spawn.joinToString(":")}"))
        return Command.SINGLE_SUCCESS
    }

    private fun addSpawn(ctx: CommandContext<CommandSourceStack>): Int {
        val player = CommandHelper.getPlayer(ctx) ?: return Command.SINGLE_SUCCESS
        val map = CommandHelper.getMap(ctx, mapManager) ?: return Command.SINGLE_SUCCESS
        val team = CommandHelper.getTeam(ctx)
        val location = player.location.round()

        if (location.world != map.managedWorld.world) {
            throw CommandError("You must be in the same world as the map")
        }

        val teamSpawns = map.teams[team]!!.spawnLikeList
        teamSpawns.add(LocationLike(location))
        map.saveToConfig()
        player.sendMessage(TextHelper.format("Added new spawn for team &s${team.name}"))
        return Command.SINGLE_SUCCESS
    }

    private fun removeSpawn(ctx: CommandContext<CommandSourceStack>): Int {
        val player = CommandHelper.getPlayer(ctx) ?: return Command.SINGLE_SUCCESS
        val map = CommandHelper.getMap(ctx, mapManager) ?: return Command.SINGLE_SUCCESS
        val spawn = StringArgumentType.getString(ctx,"spawn").split(":")
        val team = parseEnum<Team>(spawn[0])
        val index = spawn[1].toInt()

        val teamSpawns = map.teams[team]!!.spawnLikeList
        teamSpawns.removeAt(index)
        player.sendMessage(TextHelper.format("Removed spawn &p${spawn.joinToString(":")}"))
        return Command.SINGLE_SUCCESS
    }

    private fun setMapTeamRegion(ctx: CommandContext<CommandSourceStack>): Int {
        val player = CommandHelper.getPlayer(ctx) ?: return Command.SINGLE_SUCCESS
        val map = mapManager.find(StringArgumentType.getString(ctx, "map"))
        if(map == null) {
            return Command.SINGLE_SUCCESS
        }
        val team = Team.valueOf(StringArgumentType.getString(ctx,"Team"))

        val wePlayer = BukkitAdapter.adapt(player)
        val region = (WorldEdit.getInstance().sessionManager.get(wePlayer).getRegionSelector(wePlayer.world).region as CuboidRegion).clone()

        map.teams[team]!!.teamRegion = region
        map.saveToConfig()
        player.sendMessage(TextHelper.format("Set team region for team &${team.primaryColor}${team.name}"))
        return Command.SINGLE_SUCCESS

    }

    private fun setVoidHeight(ctx: CommandContext<CommandSourceStack>): Int {
        val player = CommandHelper.getPlayer(ctx) ?: return Command.SINGLE_SUCCESS
        val mapName = StringArgumentType.getString(ctx,"map")
        val map = mapManager.find(mapName)
        if(map == null) {
            return Command.SINGLE_SUCCESS
        }
        val newHeight = IntegerArgumentType.getInteger(ctx,"height")

        map.voidHeight = newHeight
        map.saveToConfig()
        player.sendMessage(TextHelper.format("Set void height to &p$newHeight"))
        return Command.SINGLE_SUCCESS
    }

    private fun setTNTStrength(ctx: CommandContext<CommandSourceStack>): Int {
        val player = CommandHelper.getPlayer(ctx) ?: return Command.SINGLE_SUCCESS
        val mapName = StringArgumentType.getString(ctx,"map")
        val newStrength = FloatArgumentType.getFloat(ctx,"strength")

        val map = mapManager.find(mapName)
        if(map == null) {
            return Command.SINGLE_SUCCESS
        }

        map.tntStrength = newStrength
        map.saveToConfig()
        player.sendMessage(TextHelper.format("Set strength to &p$newStrength"))
        return Command.SINGLE_SUCCESS
    }

    private fun setTNTCount(ctx: CommandContext<CommandSourceStack>): Int {
        val player = CommandHelper.getPlayer(ctx) ?: return Command.SINGLE_SUCCESS
        val mapName = StringArgumentType.getString(ctx,"map")
        val map = mapManager.find(mapName)
        if(map == null) {
            return Command.SINGLE_SUCCESS
        }
        val newCount = IntegerArgumentType.getInteger(ctx,"Count")

        map.tntCount = newCount
        map.saveToConfig()
        player.sendMessage(TextHelper.format("Set count to &p$newCount"))
        return Command.SINGLE_SUCCESS
    }

    private fun setFuseTicks(ctx: CommandContext<CommandSourceStack>): Int {
        val player = CommandHelper.getPlayer(ctx) ?: return Command.SINGLE_SUCCESS
        val mapName = StringArgumentType.getString(ctx,"map")
        val map = mapManager.find(mapName)
        if(map == null) {
            return Command.SINGLE_SUCCESS
        }
        val newTicks = IntegerArgumentType.getInteger(ctx,"ticks")

        map.fuseTicks = newTicks
        map.saveToConfig()
        player.sendMessage(TextHelper.format("Set fuse ticks to &p$newTicks"))
        return Command.SINGLE_SUCCESS
    }

    private fun setGracePeriod(ctx: CommandContext<CommandSourceStack>): Int {
        val player = CommandHelper.getPlayer(ctx) ?: return Command.SINGLE_SUCCESS
        val mapName = StringArgumentType.getString(ctx,"map")
        val map = CommandHelper.getMap(ctx, mapManager) ?: return Command.SINGLE_SUCCESS
        val minutes = FloatArgumentType.getFloat(ctx,"minutes")
        val ticks = round(minutes * (60.0 * 20.0)).toInt()

        map.gracePeriodTicks = ticks
        map.saveToConfig()
        player.sendMessage(TextHelper.format("Set grace period to &p$minutes&r minutes (&s$ticks&r Ticks)"))
        return Command.SINGLE_SUCCESS
    }

    private fun applyDefaultGameRules(ctx: CommandContext<CommandSourceStack>): Int {
        val player = CommandHelper.getPlayer(ctx) ?: return Command.SINGLE_SUCCESS
        val mapName = StringArgumentType.getString(ctx, "map")
        val map = mapManager.find(mapName)
        if(map == null) {
            player.sendMessage(TextHelper.deserialize(TextHelper.prefix + "&6$mapName &cdoes not exist!"))
            return Command.SINGLE_SUCCESS
        }
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

        player.sendMessage(TextHelper.format("&7Applied default gamerules"))
        return Command.SINGLE_SUCCESS
    }

    private fun listMapRegions(ctx: CommandContext<CommandSourceStack>): Int {
        val player = CommandHelper.getPlayer(ctx) ?: return Command.SINGLE_SUCCESS
        val mapName = StringArgumentType.getString(ctx,"map")
        val map = mapManager.find(mapName)
        if(map == null) {
            return Command.SINGLE_SUCCESS
        }
        val builder = Component.text()
        builder.append(TextHelper.line).appendNewline()
        builder.append(TextHelper.format("Regions for &p${map.name}  ")).append(TextHelper.deserialize("&7&lREFRESH").setCommand("/mapmanager regions list $mapName")).appendNewline()
        for (region in map.regions) {
            builder.append(TextHelper.format("&7- &s${region.name}")).append(Component.text("  ")).append(TextHelper.deserialize("&c&lDEL").setCommand("/mapmanager regions remove ${region.name}")).appendNewline()
        }
        builder.append(TextHelper.line).appendNewline()
        player.sendMessage(builder)
        return Command.SINGLE_SUCCESS
    }

    private fun addMapRegion(ctx: CommandContext<CommandSourceStack>): Int {
        val player = CommandHelper.getPlayer(ctx) ?: return Command.SINGLE_SUCCESS
        val map = CommandHelper.getMap(ctx, mapManager) ?: return Command.SINGLE_SUCCESS
        val type = CommandHelper.getStringOrNull(ctx,"type")?.let{
            RegionType.entries.find{entry -> entry.name == it}
        }
        val name = CommandHelper.getStringOrNull(ctx,"name")

        if(type == null) {
            player.sendMessage(TextHelper.deserialize(TextHelper.prefix + "&cUnkown region type!"))
            return Command.SINGLE_SUCCESS
        }

        if(name == null) {
            player.sendMessage(TextHelper.deserialize(TextHelper.prefix + "&cPlease specify a name!"))
            return Command.SINGLE_SUCCESS
        }

        val region = MapRegion(name)
        val wePlayer = BukkitAdapter.adapt(player)
        val selection = WorldEdit.getInstance().sessionManager.get(wePlayer).getRegionSelector(wePlayer.world).region as CuboidRegion

        region.type = type
        region.region = CuboidRegion.makeCuboid(selection)
        map.regions.add(region)
        map.saveToConfig()
        player.sendMessage(TextHelper.deserialize("Added region &p${region.name}"))
        return Command.SINGLE_SUCCESS
    }

    private fun removeMapRegion(ctx: CommandContext<CommandSourceStack>): Int {
        val player = CommandHelper.getPlayer(ctx) ?: return Command.SINGLE_SUCCESS
        val mapName = CommandHelper.getStringOrNull(ctx, "map")
        if(mapName == null) {
            player.sendMessage(TextHelper.prefixComp.append(Component.text("Unknown map region.").color(NamedTextColor.RED)))
            return Command.SINGLE_SUCCESS
        }
        val map = mapManager.find(mapName)
        if(map == null) {
            player.sendMessage(TextHelper.prefixComp.append(Component.text("Unknown map region.").color(NamedTextColor.RED)))
            return Command.SINGLE_SUCCESS
        }
        val name = CommandHelper.getStringOrNull(ctx, "name")
        if(name == null) {
            player.sendMessage(TextHelper.prefixComp.append(Component.text("Unknown map region.").color(NamedTextColor.RED)))
            return Command.SINGLE_SUCCESS
        }
        map.saveToConfig()
        if (map.regions.removeIf { it.name == name }) {
            player.sendMessage(TextHelper.deserialize("Removed region &6$name"))
        } else {
            player.sendMessage(TextHelper.deserialize("Region &6$name&r not found"))
        }
        return Command.SINGLE_SUCCESS
    }

    override fun create(registrar: Commands) {
        val node = LiteralArgumentBuilder.literal<CommandSourceStack>("mapmanager")
            .requires{
                    source ->
                source.sender.hasPermission("tntwars.mapmanager")
            }
            .executes(::help)
            .then(
                LiteralArgumentBuilder.literal<CommandSourceStack>("load")
                    .executes(::loadMaps)
            )
            .then(
                LiteralArgumentBuilder.literal<CommandSourceStack>("save")
                    .executes(::saveMaps)
            )
            .then(
                LiteralArgumentBuilder.literal<CommandSourceStack>("create")
                    .executes(::createMap)
                    .then(
                        RequiredArgumentBuilder.argument<CommandSourceStack, String>("name", StringArgumentType.word())
                            .executes(::createMap)
                    )
            )
            .then(
        LiteralArgumentBuilder.literal<CommandSourceStack>("list")
                .then(
                    RequiredArgumentBuilder.argument<CommandSourceStack, Int>("page", IntegerArgumentType.integer(1))
                        .executes(::listMaps)
                )
            )
            .then(
                LiteralArgumentBuilder.literal<CommandSourceStack>("info")
                    .executes{ ctx ->
                        ctx.source.sender.sendMessage(TextHelper.format("&cUsage: /mapmanager info <map>"))
                        return@executes Command.SINGLE_SUCCESS
                    }
                    .then(
                        RequiredArgumentBuilder.argument<CommandSourceStack, String>("map", StringArgumentType.word())
                            .suggests{
                                ctx, builder ->
                                mapManager.forEach { map -> builder.suggest(map.id) }
                                builder.buildFuture()
                            }
                            .executes(::infoMap)
                    )
            )
            .then(
                LiteralArgumentBuilder.literal<CommandSourceStack>("setMaterial")
                    .then(
                        RequiredArgumentBuilder.argument<CommandSourceStack, String>("map", StringArgumentType.word())
                            .executes{
                                    ctx ->
                                ctx.source.sender.sendMessage(TextHelper.format("&cUsage: /mapmanager setMaterial <map> <material>"))
                                return@executes Command.SINGLE_SUCCESS
                            }
                            .suggests { _, builder ->
                                mapManager.forEach { builder.suggest(it.id) }
                                builder.buildFuture()
                            }
                            .then(
                                RequiredArgumentBuilder.argument<CommandSourceStack, String>("material", StringArgumentType.word())
                                    .suggests{
                                            ctx, builder ->
                                        Material.entries.forEach{ entry -> builder.suggest(entry.name)}
                                        builder.buildFuture()
                                    }
                                    .executes(::setMaterial)
                            )
                    )
            )
            .then(
                LiteralArgumentBuilder.literal<CommandSourceStack>("setCreator")
                    .then(
                        RequiredArgumentBuilder.argument<CommandSourceStack, String>("map", StringArgumentType.word())
                            .suggests { _, builder ->
                                mapManager.forEach { builder.suggest(it.id) }
                                builder.buildFuture()
                            }
                            .executes{
                                ctx ->
                                ctx.source.sender.sendMessage(TextHelper.format("&cUsage: /mapmanager setCreator <map> <creator>"))
                                return@executes Command.SINGLE_SUCCESS
                            }
                        .then(
                            RequiredArgumentBuilder.argument<CommandSourceStack, String>("creator", StringArgumentType.word())
                            .executes(::setCreator))
                    )
            )
            .then(
                LiteralArgumentBuilder.literal<CommandSourceStack>("setGameModeName")
                    .then(
                        RequiredArgumentBuilder.argument<CommandSourceStack, String>("map", StringArgumentType.word())
                            .suggests { _, builder ->
                                mapManager.forEach { builder.suggest(it.id) }
                                builder.buildFuture()
                            }
                            .executes{
                                ctx ->
                                ctx.source.sender.sendMessage(TextHelper.format("&cUsage: /mapmanager setGameModeName <map> <name>"))
                                return@executes Command.SINGLE_SUCCESS
                            }
                            .then(
                                RequiredArgumentBuilder.argument<CommandSourceStack, String>("name", StringArgumentType.word())
                                    .executes(::setGameModeName)
                            )
                    )
            )
            .then(
                LiteralArgumentBuilder.literal<CommandSourceStack>("setEnabled")
                    .then(
                        RequiredArgumentBuilder.argument<CommandSourceStack, String>("map", StringArgumentType.word())
                            .executes{
                                ctx ->
                                ctx.source.sender.sendMessage(TextHelper.format("&cUsage: /mapmanager setEnabled <map> <enabled>"))
                                return@executes Command.SINGLE_SUCCESS
                            }
                            .suggests { _, builder ->
                                mapManager.forEach { builder.suggest(it.id) }
                                builder.buildFuture()
                            }
                        .then(RequiredArgumentBuilder.argument<CommandSourceStack, Boolean>("enabled", BoolArgumentType.bool())
                            .executes(::enableMap))
                    )
            )
            .then(
                LiteralArgumentBuilder.literal<CommandSourceStack>("setExperimental")
                    .then(
                        RequiredArgumentBuilder.argument<CommandSourceStack, String>("map", StringArgumentType.word())
                            .suggests { _, builder ->
                                mapManager.forEach { builder.suggest(it.id) }
                                builder.buildFuture()
                            }
                            .then(RequiredArgumentBuilder.argument<CommandSourceStack, Boolean>("experimental", BoolArgumentType.bool())
                                .executes(::setMapExperimental)
                            )
                    )
            )
            .then(
                LiteralArgumentBuilder.literal<CommandSourceStack>("tp")
                    .then(
                        RequiredArgumentBuilder.argument<CommandSourceStack, String>("map", StringArgumentType.word())
                            .suggests { _, builder ->
                                mapManager.forEach { builder.suggest(it.id) }
                                builder.buildFuture()
                            }
                            .executes(::tpMap)
                    )
            )
            .then(
                LiteralArgumentBuilder.literal<CommandSourceStack>("setTeamRegion")
                    .then(
                        RequiredArgumentBuilder.argument<CommandSourceStack, String>("map", StringArgumentType.word())
                            .suggests{
                                    ctx, builder ->
                                mapManager.forEach { map -> builder.suggest(map.name) }
                                builder.buildFuture()
                            }
                        .then(RequiredArgumentBuilder.argument<CommandSourceStack, String>("team", StringArgumentType.word())
                            .suggests { _, builder ->
                                Team.entries.forEach { builder.suggest(it.name) }
                                builder.buildFuture()
                            }
                            .executes(::setMapTeamRegion))
                    )
            )
            .then(
                LiteralArgumentBuilder.literal<CommandSourceStack>("setVoidHeight")

                    .then(
                        RequiredArgumentBuilder.argument<CommandSourceStack, String>("map", StringArgumentType.word())
                            .suggests{
                                    ctx, builder ->
                                mapManager.forEach { map -> builder.suggest(map.id) }
                                builder.buildFuture()
                            }
                            .then(
                        RequiredArgumentBuilder.argument<CommandSourceStack, Int>("height", IntegerArgumentType.integer())
                            .executes(::setVoidHeight)
                            )
                    )
            )
            .then(
                LiteralArgumentBuilder.literal<CommandSourceStack>("setTntStrength")
                    .then(
                        RequiredArgumentBuilder.argument<CommandSourceStack, String>("map", StringArgumentType.word())
                            .suggests{
                                    ctx, builder ->
                                mapManager.forEach { map -> builder.suggest(map.id) }
                                builder.buildFuture()
                            }
                            .then(
                        RequiredArgumentBuilder.argument<CommandSourceStack, Float>("strength", FloatArgumentType.floatArg())
                            .executes(::setTNTStrength)
                            )
                    )
            )
            .then(
                LiteralArgumentBuilder.literal<CommandSourceStack>("setTntCount")
                    .then(
                        RequiredArgumentBuilder.argument<CommandSourceStack, String>("map", StringArgumentType.word())
                            .suggests{
                                    ctx, builder ->
                                mapManager.forEach { map -> builder.suggest(map.id) }
                                builder.buildFuture()
                            }
                            .then(
                                RequiredArgumentBuilder.argument<CommandSourceStack, Int>("count", IntegerArgumentType.integer())
                                    .executes(::setTNTCount)
                            )
                    )
            )
            .then(
                LiteralArgumentBuilder.literal<CommandSourceStack>("setFuseTicks")
                    .then(
                        RequiredArgumentBuilder.argument<CommandSourceStack, String>("map", StringArgumentType.word())
                            .suggests{
                                    ctx, builder ->
                                mapManager.forEach { map -> builder.suggest(map.id) }
                                builder.buildFuture()
                            }
                        .then(RequiredArgumentBuilder.argument<CommandSourceStack, Int>("ticks", IntegerArgumentType.integer())
                            .executes(::setFuseTicks))
                    )
            )
            .then(
                LiteralArgumentBuilder.literal<CommandSourceStack>("gracePeriod")
                    .then(
                        RequiredArgumentBuilder.argument<CommandSourceStack, String>("map", StringArgumentType.word())
                            .suggests{
                                ctx, builder ->
                                mapManager.forEach { map -> builder.suggest(map.id) }
                                builder.buildFuture()
                            }
                        .then(RequiredArgumentBuilder.argument<CommandSourceStack, Float>("minutes", FloatArgumentType.floatArg())
                            .executes(::setGracePeriod))
                    )
            )
            .then(
                LiteralArgumentBuilder.literal<CommandSourceStack>("applyGamerules")
                    .then(
                        RequiredArgumentBuilder.argument<CommandSourceStack, String>("map", StringArgumentType.word())
                            .executes(::applyDefaultGameRules)
                            .suggests{
                                ctx, builder ->
                                mapManager.forEach{map -> builder.suggest(map.id)}
                                builder.buildFuture()
                            }
                    )
            )
            .then(
                LiteralArgumentBuilder.literal<CommandSourceStack>("regions")
                    .then(
                        RequiredArgumentBuilder.argument<CommandSourceStack, String>("map", StringArgumentType.word())
                            .suggests{
                                    ctx, builder ->
                                mapManager.forEach{map -> builder.suggest(map.id)}
                                builder.buildFuture()
                            }
                            .then(
                        LiteralArgumentBuilder.literal<CommandSourceStack>("list")
                            .executes(::listMapRegions)
                            )
                    )
                    .then(
                        LiteralArgumentBuilder.literal<CommandSourceStack>("add")
                            .then(
                                    RequiredArgumentBuilder.argument<CommandSourceStack, String>("type", StringArgumentType.word())
                                        .suggests { _, builder ->
                                            RegionType.entries.forEach { builder.suggest(it.name) }
                                            builder.buildFuture()
                                        }
                                        .then(
                                            RequiredArgumentBuilder.argument<CommandSourceStack, String>("name", StringArgumentType.word())
                                                .executes { ctx ->
                                                    val name = StringArgumentType.getString(ctx, "name").lowercase()
                                                    val map = CommandHelper.getMap(ctx, mapManager) ?: return@executes Command.SINGLE_SUCCESS

                                                    if (map.regions.any { it.name == name }) {
                                                        throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.dispatcherParseException().create("Region already exists")
                                                    }

                                                    addMapRegion(ctx)
                                                }
                                        )
                                    )
                            )
                    .then(
                        LiteralArgumentBuilder.literal<CommandSourceStack>("remove")
                            .then(
                                RequiredArgumentBuilder.argument<CommandSourceStack, String>("name", StringArgumentType.word())
                                        .suggests { ctx, builder ->
                                            val map = CommandHelper.getMap(ctx, mapManager)
                                                if(map == null) {
                                                    return@suggests builder.buildFuture()
                                                }
                                            map.regions.forEach { builder.suggest(it.name) }
                                            builder.buildFuture()
                                        }
                                        .executes(::removeMapRegion)
                            )
                    )
            )
            .then(
                LiteralArgumentBuilder.literal<CommandSourceStack>("spawn")
                    .then(
                        RequiredArgumentBuilder.argument<CommandSourceStack, String>("map", StringArgumentType.word())
                            .suggests{
                                    ctx, builder ->
                                mapManager.forEach{map -> builder.suggest(map.id)}
                                builder.buildFuture()
                            }
                            .then(
                                LiteralArgumentBuilder.literal<CommandSourceStack>("tp")
                                    .then(
                                        RequiredArgumentBuilder.argument<CommandSourceStack, String>("spawn", StringArgumentType.word())
                                            .suggests { ctx, builder ->
                                                val map = CommandHelper.getMap(ctx, mapManager) ?: return@suggests builder.buildFuture()
                                                val spawn = CommandHelper.getStringOrNull(ctx, "spawn")?.split(":") ?: return@suggests builder.buildFuture()
                                                val teamData = map.teams[Team.valueOf(spawn[0])] ?: return@suggests builder.buildFuture()
                                                teamData.spawnLikeList.forEach{ location -> builder.suggest("${spawn[0]}:${location.x},${location.y},${location.z},${location.pitch},${location.yaw}") }
                                                builder.buildFuture()
                                            }
                                            .executes(::tpSpawn)
                                    )
                            )
                            .then(
                        LiteralArgumentBuilder.literal<CommandSourceStack>("list")
                                .then(
                                RequiredArgumentBuilder.argument<CommandSourceStack, String>("team", StringArgumentType.word())
                                    .suggests { _, builder ->
                                        Team.entries.filter{it != Team.Queue}.forEach { builder.suggest(it.name) }
                                        builder.buildFuture()
                                    }
                                    .executes(::listSpawns)
                                )
                            )
                            .then(
                                LiteralArgumentBuilder.literal<CommandSourceStack>("add")
                                    .then(
                                            RequiredArgumentBuilder.argument<CommandSourceStack, String>("team", StringArgumentType.word())
                                                .suggests { _, builder ->
                                                    Team.entries.forEach { builder.suggest(it.name) }
                                                    builder.buildFuture()
                                                }
                                                .executes(::addSpawn)
                                        )
                            )
                            .then(
                                LiteralArgumentBuilder.literal<CommandSourceStack>("remove")
                                    .then(
                                        RequiredArgumentBuilder.argument<CommandSourceStack, String>("spawn", StringArgumentType.word())
                                            .suggests { ctx, builder ->
                                                val map = CommandHelper.getMap(ctx, mapManager) ?: return@suggests builder.buildFuture()
                                                val spawn = CommandHelper.getStringOrNull(ctx, "spawn")?.split(":") ?: return@suggests builder.buildFuture()
                                                val teamData = map.teams[Team.valueOf(spawn[0])] ?: return@suggests builder.buildFuture()
                                                teamData.spawnLikeList.forEach{ location -> builder.suggest("${spawn[0]}:${location.x},${location.y},${location.z},${location.pitch},${location.yaw}") }
                                                builder.buildFuture()
                                            }
                                            .executes(::removeSpawn)
                                    )
                            )
                    )
            )

            .build()

        registrar.register(node)
        val alias = LiteralArgumentBuilder.literal<CommandSourceStack>("mm").redirect(node)
        registrar.register(alias.build())
    }
}