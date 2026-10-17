package com.addemann03.player_morphs.morphs

import com.addemann03.player_morphs.Morph
import com.addemann03.player_morphs.entities.MorphInfo
import me.libraryaddict.disguise.disguisetypes.watchers.PiglinWatcher
import me.libraryaddict.disguise.disguisetypes.watchers.ZombieWatcher
import org.bukkit.Material
import org.bukkit.entity.EntityType

class PiglinMorph : Morph<PiglinWatcher>(EntityType.PIGLIN) {
    companion object : MorphType<PiglinMorph>(PiglinMorph::class) {
        override val info = MorphInfo("Piglin", listOf("&7Huh? Ooh gold!? Oink oink."), Material.PIGLIN_HEAD)
    }

    override fun modifier(watcher: PiglinWatcher) {
        watcher.isBaby = false
        watcher.isAddEntityAnimations = true

    }
}