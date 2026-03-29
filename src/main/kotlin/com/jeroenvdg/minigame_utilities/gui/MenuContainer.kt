package com.jeroenvdg.minigame_utilities.gui

import com.jeroenvdg.minigame_utilities.gui.slots.GUISlot
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.inventory.Inventory

open class MenuContainer(private val inventory: Inventory) {

    private val slots = arrayOfNulls<GUISlot>(inventory.size)
    private var closeHandler: (InventoryCloseEvent) -> Unit = {}

    fun add(guiSlot: GUISlot) {
        if (guiSlot.index < 0 || guiSlot.index > inventory.size) {
            throw Exception("Index ${guiSlot.index} is outside of inventory! min: 0, max: ${inventory.size}")
        }

        slots[guiSlot.index] = guiSlot
        updateSlot(guiSlot)
    }

    fun isActive(slot: GUISlot): Boolean {
        if (slot.index < 0 || slot.index > inventory.size) return false
        return slots[slot.index] == slot
    }

    fun canDragSlot(slot: Int): Boolean {
        if (slot < 0 || slot > inventory.size) return true
        val guiSlot = slots[slot]
        return guiSlot != null && slots[slot]!!.allowDrag
    }

    fun updateSlot(slot: GUISlot) {
        if (!isActive(slot)) return
        inventory.setItem(slot.index, slot.displayItem)
    }

    fun clear() {
        for (guiSlot in slots.reversed()) {

        }
    }

    fun onInventoryClose(action: (InventoryCloseEvent) -> Unit) {
        closeHandler = action
    }

    fun handleInventoryClick(event: InventoryClickEvent) {
        if (event.slot >= slots.size) {
            event.isCancelled = true
            return
        }

        val slot = slots[event.slot]
        if (slot == null) {
            event.isCancelled = true
        } else {
            slot.handleSlotClicked(event)
        }
    }

    fun handleInventoryClose(event: InventoryCloseEvent) {
        closeHandler(event)
    }
}