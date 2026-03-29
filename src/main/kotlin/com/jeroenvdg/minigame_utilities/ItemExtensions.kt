package com.jeroenvdg.minigame_utilities

import net.kyori.adventure.text.TextComponent
import org.bukkit.inventory.ItemStack

fun ItemStack.setDisplayName(component: TextComponent) : ItemStack {
    val m = itemMeta
    m.displayName(component)
    itemMeta = m
    return this
}


fun ItemStack.setDisplayName(string: String): ItemStack {
    return setDisplayName(TextHelper.deserialize(string))
}