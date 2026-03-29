package com.jeroenvdg.tntwars.managers.mapManager

import com.jeroenvdg.minigame_utilities.*
import com.jeroenvdg.minigame_utilities.manager.Manageable
import com.jeroenvdg.tntwars.game.Team
import com.jeroenvdg.tntwars.managers.ManagedWorld
import com.sk89q.worldedit.math.BlockVector3
import com.sk89q.worldedit.regions.CuboidRegion
import org.bukkit.Material
import org.bukkit.configuration.ConfigurationSection

class TNTWarsMap(val managedWorld: ManagedWorld, name: String) : Manageable(managedWorld.name.lowercase().replace(' ', '_'), name) {

    var voidHeight = 0
    var tntStrength = -1f
    var tntCount = -1
    var fuseTicks = -1
    var gracePeriodTicks = -1
    var isExperimental = false
    var gamemodeName = "Classic"
    val teams = mutableMapOf<Team, MapTeamData>()
    val regions = mutableListOf<MapRegion>()
    var itemMaterial = Material.AIR
    var creator = "CubedCraft"

    init {
        loadFromConfig()
    }

    fun loadFromConfig() {
        teams.clear()
        regions.clear()
        val section = managedWorld.getConfigSection("map")

        for (team in arrayOf(Team.Spectator, Team.Red, Team.Blue)) {
            val teamSection = section.getConfigurationSection(team.name) ?: section.createSection(team.name)

            teams[team] = MapTeamData()
            teams[team]!!.loadFromConfig(teamSection)
        }

        val regionsSection = section.getConfigurationSection("regions") ?: section.createSection("regions")
        for (key in regionsSection.getKeys(false)) {
            val regionSection = regionsSection.getConfigurationSection(key)!!
            val region = MapRegion(key)
            region.loadFromConfig(regionSection)
            regions.add(region)
        }

        isExperimental = section.getBoolean("isExperimental")
        gamemodeName = section.getString("gamemodeName", "Classic")!!
        voidHeight = section.getInt("voidHeight", 0)
        tntStrength = section.getDouble("tntStrength", -1.0).toFloat()
        tntCount = section.getInt("tntCount", -1)
        fuseTicks = section.getInt("fuseLength", -1)
        gracePeriodTicks = section.getInt("gracePeriodTicks", -1)
        itemMaterial = Material.getMaterial(section.getString("material", "AIR")!!) ?: Material.AIR
        creator = section.getString("creator", "CubedCraft")!!

        // Must be last step!!
        enabled = section.getBoolean("enabled")
    }

    fun saveToConfig() {
        val section = managedWorld.getConfigSection("map")
        section.set("enabled", enabled)
        section.set("isExperimental", isExperimental)
        section.set("gamemodeName", gamemodeName)
        section.set("voidHeight", voidHeight)
        section.set("tntStrength", tntStrength)
        section.set("tntCount", tntCount)
        section.set("fuseLength", fuseTicks)
        section.set("gracePeriodTicks", gracePeriodTicks)
        section.set("material", itemMaterial.toString())
        section.set("creator", creator)

        for (kvPair in teams) {
            val teamSection = section.getConfigurationSection(kvPair.key.name) ?: section.createSection(kvPair.key.name)
            kvPair.value.saveToConfig(teamSection)
        }

        val regionSection = section.getConfigurationSection("regions") ?: section.createSection("regions")
        regionSection.clear()
        for (region in regions) {
            region.saveToConfig(regionSection.getConfigurationSection(region.name) ?: regionSection.createSection(region.name))
        }

        if (managedWorld.world != null && (teams[Team.Spectator]?.spawnLikeList?.size ?: 0) > 0) {
            managedWorld.world!!.spawnLocation = teams[Team.Spectator]!!.spawnLikeList.first().toLocation(managedWorld.world!!)
        }

        managedWorld.saveConfig()
    }

    override fun isReady(): Boolean {
        if (itemMaterial == Material.AIR) return false

        for (pair in teams) {
            val team = pair.key
            val data = pair.value
            if (data.spawnLikeList.isEmpty()) return false
            if (team.isSpectatorTeam) continue
            if (data.teamRegion == null) return false
        }

        return true
    }
}

class MapTeamData {

    val spawnLikeList = mutableListOf<LocationLike>()
    var teamRegion: CuboidRegion? = null
        set(value) {
            if (value == field) return
            if (value == null) {
                field = null
                return
            }

            val minPos = value.minimumPoint
            val maxPoint = value.maximumPoint

            val adjustedRegion = CuboidRegion(BlockVector3.at(minPos.x, 0, minPos.z), maxPoint)
            field = adjustedRegion
        }

    fun loadFromConfig(section: ConfigurationSection) {
        spawnLikeList.clear()
        teamRegion = section.getCuboidRegion("teamRegion")

        val spawnSection = section.getConfigurationSection("spawns") ?: section.createSection("spawns")
        for (key in spawnSection.getKeys(false)) {
            spawnLikeList.add(spawnSection.getLocationLike(key)!!)
        }
    }

    fun saveToConfig(section: ConfigurationSection) {
        section.setCuboidRegion("teamRegion", teamRegion)

        val spawns = section.getConfigurationSection("spawns") ?: section.createSection("spawns")
        for (key in spawns.getKeys(false)) {
            spawns.set(key, null)
        }
        for (i in spawnLikeList.indices) {
            spawns.setLocationLike("$i", spawnLikeList[i])
        }
    }
}

class MapRegion(var name: String) {

    var type = RegionType.Protected
    var region: CuboidRegion? = null

    fun loadFromConfig(section: ConfigurationSection) {
        type = parseEnum<RegionType>(section.getString("type") ?: "Protected") ?: RegionType.Protected
        region = section.getCuboidRegion("region")
    }

    fun saveToConfig(section: ConfigurationSection) {
        section.set("type", type.name)
        section.setCuboidRegion("region", region)
    }

}

enum class RegionType {
    Protected,
    Wall
}