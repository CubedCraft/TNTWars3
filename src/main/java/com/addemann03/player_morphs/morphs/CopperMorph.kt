package com.addemann03.player_morphs.morphs

import com.addemann03.player_morphs.Morph
import com.addemann03.player_morphs.entities.MorphInfo
import me.libraryaddict.disguise.disguisetypes.watchers.BoggedWatcher
import me.libraryaddict.disguise.disguisetypes.watchers.CopperGolemWatcher
import me.libraryaddict.disguise.disguisetypes.watchers.CreeperWatcher
import org.bukkit.Material
import org.bukkit.entity.EntityType

class CopperMorph : Morph<CopperGolemWatcher>(EntityType.COPPER_GOLEM) {
    companion object : MorphType<CopperMorph>(CopperMorph::class) {
        override val info = MorphInfo("Copper Golem", listOf("Become a busy cannon worker.","§cThis morph will make the game harder."), Material.COPPER_GOLEM_STATUE)
    }

    override fun modifier(watcher: CopperGolemWatcher) {

    }
}