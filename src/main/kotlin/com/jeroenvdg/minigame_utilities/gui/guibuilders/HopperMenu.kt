package com.jeroenvdg.minigame_utilities.gui.guibuilders

import com.jeroenvdg.minigame_utilities.TextHelper
import com.jeroenvdg.minigame_utilities.Textial
import com.jeroenvdg.minigame_utilities.gui.MenuContainer
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.event.inventory.InventoryType
import org.bukkit.inventory.Inventory

class HopperMenu : IMenu {
    override val menu: Inventory
    override val container: MenuContainer

    constructor(title: String, builderAction: MenuContainer.() -> Unit) : this(TextHelper.deserialize("&0${title.replace("&r", "&0")}"), builderAction)

    constructor(title: Component, builderAction: MenuContainer.() -> Unit) {
        this.menu = Bukkit.createInventory(this, InventoryType.HOPPER, title)
        this.container = MenuContainer(menu)
        builderAction(container)
    }
}