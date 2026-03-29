package com.jeroenvdg.minigame_utilities.gui.guibuilders

import com.jeroenvdg.minigame_utilities.TextHelper
import com.jeroenvdg.minigame_utilities.gui.MenuContainer
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.inventory.Inventory

class ChestMenu : IMenu {

    override val menu: Inventory
    override val container: MenuContainer

    constructor(title: String, rows: Int, builderAction: MenuContainer.() -> Unit) : this(TextHelper.deserialize("&0${title.replace("&r", "&0")}"), rows, builderAction)

    constructor(title: Component, rows: Int, builderAction: MenuContainer.() -> Unit) {
        this.menu = Bukkit.createInventory(this, rows * 9, title)
        this.container = MenuContainer(menu)
        builderAction(container)
    }
}