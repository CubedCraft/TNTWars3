package com.addemann03.player_morphs.morphs

import com.addemann03.player_morphs.Morph
import com.addemann03.player_morphs.entities.MorphInfo
import me.libraryaddict.disguise.disguisetypes.watchers.CreakingWatcher
import org.bukkit.Material
import org.bukkit.entity.EntityType

class CreakingMorph : Morph<CreakingWatcher>(EntityType.CREAKING) {
    companion object : MorphType<CreakingMorph>(CreakingMorph::class) {
        override val info = MorphInfo("Creaking", listOf("The lord of the creaking sounds."), Material.CREAKING_HEART)
    }

    override fun modifier(watcher: CreakingWatcher) {

    }
}