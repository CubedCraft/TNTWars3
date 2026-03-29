package com.jeroenvdg.minigame_utilities.gui.slots

import com.jeroenvdg.minigame_utilities.gui.MenuContainer
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.ItemStack

fun MenuContainer.addItem(slot: Int, itemStack: ItemStack) {
    add(GUISlot(slot, itemStack, this))
}

open class GUISlot {

    val index: Int
    val container: MenuContainer
    var allowDrag = false

    var displayItem: ItemStack? = null
        set(value) {
            field = value
            container.updateSlot(this)
        }

    val isActive get() = container.isActive(this)

    protected constructor(index: Int, container: MenuContainer) {
        this.index = index
        this.container = container
    }

    constructor(index: Int, itemStack: ItemStack, container: MenuContainer) {
        this.index = index
        this.container = container
        this.displayItem = itemStack
    }

    open fun handleSlotClicked(event: InventoryClickEvent) {
        event.isCancelled = true
    }

}