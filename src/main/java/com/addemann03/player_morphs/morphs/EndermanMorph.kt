package com.addemann03.player_morphs.morphs

import com.addemann03.player_morphs.Morph
import com.addemann03.player_morphs.entities.MorphInfo
import me.libraryaddict.disguise.disguisetypes.watchers.BoggedWatcher
import me.libraryaddict.disguise.disguisetypes.watchers.CreeperWatcher
import me.libraryaddict.disguise.disguisetypes.watchers.EndermanWatcher
import org.bukkit.Material
import org.bukkit.entity.EntityType

class EndermanMorph : Morph<EndermanWatcher>(EntityType.ENDERMAN) {
    companion object : MorphType<EndermanMorph>(EndermanMorph::class) {
        override val info = MorphInfo("Enderman", listOf("Become the Construction Manager."), Material.ENDER_PEARL)
    }

    override fun modifier(watcher: EndermanWatcher) {

    }
}