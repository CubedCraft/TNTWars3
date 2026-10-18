package com.addemann03.player_morphs.morphs

import com.addemann03.player_morphs.Morph
import com.addemann03.player_morphs.entities.MorphInfo
import me.libraryaddict.disguise.disguisetypes.watchers.CreeperWatcher
import org.bukkit.Material
import org.bukkit.entity.EntityType

class CreeperMorph : Morph<CreeperWatcher>(EntityType.CREEPER) {
    companion object : MorphType<CreeperMorph>(CreeperMorph::class) {
        override val info = MorphInfo("Creeper", listOf("Make TNTWars even more explosive!"), Material.CREEPER_HEAD)
    }

    override fun modifier(watcher: CreeperWatcher) {

    }
}