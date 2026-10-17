package com.addemann03.player_morphs.morphs

import com.addemann03.player_morphs.Morph
import com.addemann03.player_morphs.entities.MorphInfo
import me.libraryaddict.disguise.disguisetypes.watchers.BoggedWatcher
import me.libraryaddict.disguise.disguisetypes.watchers.CreeperWatcher
import me.libraryaddict.disguise.disguisetypes.watchers.WardenWatcher
import org.bukkit.Material
import org.bukkit.entity.EntityType

class WardenMorph : Morph<WardenWatcher>(EntityType.WARDEN) {
    companion object : MorphType<WardenMorph>(WardenMorph::class) {
        override val info = MorphInfo("Warden", listOf(), Material.ECHO_SHARD)
    }

    override fun modifier(watcher: WardenWatcher) {

    }
}