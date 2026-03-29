package com.jeroenvdg.minigame_utilities.gui.guibuilders

import com.jeroenvdg.minigame_utilities.gui.MenuContainer
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.InventoryHolder

interface IMenu : InventoryHolder {

    // Needs a different name due to naming clash
    val menu: Inventory
    val container: MenuContainer

    override fun getInventory() = menu
    fun open(player: Player) = player.openInventory(menu)
    fun edit (action: MenuContainer.() -> Unit) = action(container)
    fun handleInventoryClick(event: InventoryClickEvent) = container.handleInventoryClick(event)
    fun handleInventoryClose(event: InventoryCloseEvent) = container.handleInventoryClose(event)
}