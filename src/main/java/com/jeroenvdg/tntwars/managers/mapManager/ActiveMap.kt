package com.jeroenvdg.tntwars.managers.mapManager

import com.jeroenvdg.tntwars.game.Team
import com.jeroenvdg.tntwars.managers.ManagedWorld
import com.jeroenvdg.minigame_utilities.Textial
import com.sk89q.worldedit.regions.CuboidRegion
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TextComponent
import org.bukkit.Location

class ActiveMap(private val mapData: TNTWarsMap, val managedWorld: ManagedWorld) {

    val name = mapData.name

    val spawns = HashMap<Team, List<Location>>()
    val teamRegions = HashMap<Team, CuboidRegion>()
    val protectedRegions = mapData.regions.filter { it.type == RegionType.Protected }.toMutableList()
    var tntStrength = mapData.tntStrength
    var tntCount = mapData.tntCount
    var fuseTicks = mapData.fuseTicks
    var gracePeriodTicks = mapData.gracePeriodTicks
    var gracePeriodActive = false
    val voidHeight = mapData.voidHeight
    var startedTime: Long = 0

    val mapMessage: TextComponent

    init {
        for (team in mapData.teams) {
            val list = mutableListOf<Location>()
            for (spawn in team.value.spawnLikeList) {
                list.add(spawn.toLocation(managedWorld.world!!))
            }
            spawns[team.key] = list
            teamRegions[team.key] = team.value.teamRegion ?: continue
        }

        var mapMessage = Textial.summary.parse(listOf(
            "&7&m                       &r",
            " &fMap: &p${mapData.name}",
            " &fMade By: &p${mapData.creator}",
            " &fGamemode: &p${mapData.gamemodeName}",
            "&7&m                       &r",
        ))

        if (mapData.isExperimental) {
            mapMessage = mapMessage.append(Component.text()
                .appendNewline()
                .appendNewline().append(Textial.bc.format("&wThis is an experimental map!"))
                .appendNewline().append(Textial.bc.format("&wThis is an experimental map!"))
                .appendNewline().append(Textial.bc.format("&wThis is an experimental map!"))
                .appendNewline())
        }

        this.mapMessage =mapMessage
    }

    fun getMapData(): TNTWarsMap {
        return mapData
    }

    fun dispose() {
        managedWorld.delete()
    }
}