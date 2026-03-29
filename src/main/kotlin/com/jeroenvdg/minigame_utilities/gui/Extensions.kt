package com.jeroenvdg.minigame_utilities.gui

import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryClickEvent

val InventoryClickEvent.player get() = this.view.player as Player