package com.addemann03.player_morphs.morphs

import com.addemann03.player_morphs.Morph
import com.addemann03.player_morphs.entities.MorphInfo
import me.libraryaddict.disguise.disguisetypes.watchers.BoggedWatcher
import me.libraryaddict.disguise.disguisetypes.watchers.CreeperWatcher
import org.bukkit.Material
import org.bukkit.entity.EntityType

class BoggedMorph : Morph<BoggedWatcher>(EntityType.BOGGED) {
    companion object : MorphType<BoggedMorph>(BoggedMorph::class) {
        override val info = MorphInfo("Bogged", listOf("The shrek-like monster."), Material.MOSSY_COBBLESTONE)
    }

    override fun modifier(watcher: BoggedWatcher) {

    }
}