package com.addemann03.player_morphs.morphs

import com.addemann03.player_morphs.Morph
import com.addemann03.player_morphs.entities.MorphInfo
import me.libraryaddict.disguise.disguisetypes.watchers.SkeletonWatcher
import me.libraryaddict.disguise.disguisetypes.watchers.ZombieWatcher
import org.bukkit.Material
import org.bukkit.entity.EntityType

class SkeletonMorph : Morph<SkeletonWatcher>(EntityType.SKELETON) {
    companion object : MorphType<SkeletonMorph>(SkeletonMorph::class) {
        override val info = MorphInfo("Skeleton", listOf(), Material.SKELETON_SKULL)
    }

    override fun modifier(watcher: SkeletonWatcher) {

    }
}