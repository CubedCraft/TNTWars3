package com.jeroenvdg.tntwars.player.behaviours

import com.jeroenvdg.tntwars.player.PlayerBehaviour
import com.jeroenvdg.tntwars.player.TNTWarsPlayer
import org.bukkit.Material
import org.bukkit.event.player.PlayerInteractEvent

abstract class BasePlayerRotateItemBehaviour(user: TNTWarsPlayer) : PlayerBehaviour(user) {

    protected abstract val isRightClick: Boolean
    protected abstract val isSneaking: Boolean
    protected abstract val materials: List<Material>

    override fun onActivate() {
        user.onInteract += ::handleInteract
    }

    override fun onDeactivate() {
        user.onInteract -= ::handleInteract
    }

    private fun handleInteract(event: PlayerInteractEvent) {
        if (event.action.isRightClick && !isRightClick) return
        if (event.action.isLeftClick && isRightClick) return
        if (!event.player.isSneaking && isSneaking) return

        val item = event.item ?: return

        val index = materials.indexOf(item.type)
        if (index < 0) return

        event.isCancelled = true
        item.type = materials[(index + 1) % materials.size]
    }
}
