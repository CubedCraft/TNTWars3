package com.addemann03.player_morphs.morphs

import com.addemann03.player_morphs.Morph
import com.addemann03.player_morphs.entities.MorphInfo
import me.libraryaddict.disguise.disguisetypes.watchers.ZombieWatcher
import org.bukkit.Material
import org.bukkit.entity.EntityType

class ZombieMorph : Morph<ZombieWatcher>(EntityType.ZOMBIE) {
    companion object : MorphType<ZombieMorph>(ZombieMorph::class) {
        override val info = MorphInfo("Zombie", listOf(("&7Morph into the classic zombie type.")), Material.ZOMBIE_HEAD)
    }

    override fun modifier(watcher: ZombieWatcher) {
        watcher.isBaby = false
        watcher.isAddEntityAnimations = true

    }
}