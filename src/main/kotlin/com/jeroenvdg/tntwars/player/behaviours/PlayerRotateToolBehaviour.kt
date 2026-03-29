package com.jeroenvdg.tntwars.player.behaviours

import com.jeroenvdg.tntwars.player.TNTWarsPlayer
import org.bukkit.Material

class PlayerRotateToolBehaviour(user: TNTWarsPlayer) : BasePlayerRotateItemBehaviour(user) {
    override val isRightClick: Boolean = true
    override val isSneaking: Boolean = true
    override val materials = listOf(Material.DIAMOND_PICKAXE, Material.DIAMOND_AXE, Material.DIAMOND_SHOVEL, Material.SHEARS)
}