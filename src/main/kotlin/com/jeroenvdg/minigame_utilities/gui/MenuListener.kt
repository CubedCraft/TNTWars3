package com.jeroenvdg.minigame_utilities.gui

import com.jeroenvdg.minigame_utilities.gui.guibuilders.IMenu
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.event.inventory.InventoryDragEvent

class MenuListener : Listener {

    @EventHandler
    fun onInventoryClick(event: InventoryClickEvent) {
        var inventory = event.clickedInventory?.holder
        if (inventory !is IMenu) {
            if (!event.isShiftClick) return
            if (event.inventory.holder !is IMenu) return
            inventory = event.inventory.holder as IMenu
        }

        inventory.handleInventoryClick(event)
    }

    @EventHandler
    fun onItemDrag(event: InventoryDragEvent) {
        if (event.isCancelled) return
        val inventory = event.inventory.holder
        if (inventory !is IMenu) return
        if (event.rawSlots.any { !inventory.container.canDragSlot(it) }) {
            event.isCancelled = true
        }
    }

    @EventHandler
    fun onInventoryClose(event: InventoryCloseEvent) {
        val holder = event.inventory
        if (holder !is IMenu) return
        holder.handleInventoryClose(event)
    }
}