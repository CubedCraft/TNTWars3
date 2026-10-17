package com.addemann03.player_morphs.morphs

import com.addemann03.player_morphs.Morph
import com.addemann03.player_morphs.entities.MorphInfo
import me.libraryaddict.disguise.disguisetypes.watchers.CreeperWatcher
import me.libraryaddict.disguise.disguisetypes.watchers.VindicatorWatcher
import org.bukkit.Material
import org.bukkit.entity.EntityType

class VindicatorMorph : Morph<VindicatorWatcher>(EntityType.VINDICATOR) {
    companion object : MorphType<VindicatorMorph>(VindicatorMorph::class) {
        override val info = MorphInfo("Vindicator", listOf("Very Aggressive."), Material.IRON_AXE)
    }

    override fun modifier(watcher: VindicatorWatcher) {

    }
}