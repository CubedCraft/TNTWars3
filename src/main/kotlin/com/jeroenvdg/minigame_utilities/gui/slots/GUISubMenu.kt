package com.jeroenvdg.minigame_utilities.gui.slots

import com.jeroenvdg.minigame_utilities.gui.MenuContainer
import com.jeroenvdg.minigame_utilities.gui.guibuilders.IMenu
import com.jeroenvdg.minigame_utilities.gui.player
import org.bukkit.inventory.ItemStack

fun MenuContainer.addSubMenu(slot: Int, item: ItemStack, menu: IMenu) {
    addButton(slot) {
        displayItem = item
        onClick {
            menu.open(it.player)
        }
    }
}