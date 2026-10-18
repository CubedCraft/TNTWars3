package com.addemann03.player_morphs.morphs

import com.addemann03.player_morphs.Morph
import com.addemann03.player_morphs.entities.MorphInfo
import me.libraryaddict.disguise.disguisetypes.watchers.CreeperWatcher
import me.libraryaddict.disguise.disguisetypes.watchers.WitherSkeletonWatcher
import org.bukkit.Material
import org.bukkit.entity.EntityType

class WitherSkelletonMorph : Morph<WitherSkeletonWatcher>(EntityType.WITHER_SKELETON) {
    companion object : MorphType<WitherSkelletonMorph>(WitherSkelletonMorph::class) {
        override val info = MorphInfo("Wither Skeleton", listOf("Spooky Scary Skeleton!"), Material.WITHER_SKELETON_SKULL)
    }

    override fun modifier(watcher: WitherSkeletonWatcher) {

    }
}