package com.jeroenvdg.tntwars.player.behaviours

import com.jeroenvdg.tntwars.player.TNTWarsPlayer
import org.bukkit.Material

class PlayerRotateFenceBehaviour(user: TNTWarsPlayer) : BasePlayerRotateItemBehaviour(user) {
    override val isRightClick: Boolean = false
    override val isSneaking: Boolean = true
    override val materials = listOf(Material.OAK_FENCE, Material.STONE_PRESSURE_PLATE)
}