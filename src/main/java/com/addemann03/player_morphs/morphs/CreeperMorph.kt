package com.addemann03.player_morphs.morphs

import com.addemann03.player_morphs.Morph
import com.addemann03.player_morphs.entities.MorphInfo
import me.libraryaddict.disguise.disguisetypes.watchers.PiglinWatcher
import me.libraryaddict.disguise.disguisetypes.watchers.ZombieWatcher
import org.bukkit.Material
import org.bukkit.entity.EntityType

class CreeperMorph : Morph<PiglinWatcher>(EntityType.PIGLIN) {
    companion object : MorphType<CreeperMorph>(CreeperMorph::class) {
        override val info = MorphInfo("Piglin", listOf(), Material.PIGLIN_HEAD)
    }

    override fun modifier(watcher: PiglinWatcher) {

    }
}