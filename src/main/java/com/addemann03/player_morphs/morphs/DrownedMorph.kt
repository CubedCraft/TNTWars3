package com.addemann03.player_morphs.morphs

import com.addemann03.player_morphs.Morph
import com.addemann03.player_morphs.entities.MorphInfo
import me.libraryaddict.disguise.disguisetypes.watchers.ZombieWatcher
import org.bukkit.Material
import org.bukkit.entity.EntityType

class DrownedMorph : Morph<ZombieWatcher>(EntityType.DROWNED) {
    companion object : MorphType<DrownedMorph>(DrownedMorph::class) {
        override val info = MorphInfo("Drowned", listOf(), Material.TRIDENT)
    }

    override fun modifier(watcher: ZombieWatcher) {

    }
}