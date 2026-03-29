package com.jeroenvdg.minigame_utilities.gui.slots

import com.jeroenvdg.minigame_utilities.LoreBuilder
import com.jeroenvdg.minigame_utilities.TextHelper
import com.jeroenvdg.minigame_utilities.Textial
import com.jeroenvdg.minigame_utilities.gui.MenuContainer
import com.jeroenvdg.minigame_utilities.gui.player
import org.bukkit.Material
import org.bukkit.enchantments.Enchantment
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack

fun MenuContainer.addCarousel(slot: Int, action: GUICarouselSlot.() -> Unit) {
    val carousel = GUICarouselSlot(slot, this)
    action(carousel)
    carousel.updateItem()
    add(carousel)
}

class GUICarouselSlot(slot: Int, container: MenuContainer) : GUIButtonSlot(slot, container) {

    private val options = ArrayList<ItemStack>(2)

    var currentOption = 0
        set(value) {
            if (value == field) return
            if (value < 0) throw IndexOutOfBoundsException()
            if (value >= options.size) throw IndexOutOfBoundsException()
            field = value
            updateItem()
        }

    fun addOption(item: ItemStack) {
        options.add(item)
    }

    fun makeItem(material: Material, amount: Int = 1, action: CarouselItemBuilder.() -> Unit): ItemStack {
        val builder = CarouselItemBuilder(material, amount)
        action(builder)
        return builder.build()
    }

    fun updateItem() {
        displayItem = options[currentOption]
    }

    override fun handleSlotClicked(event: InventoryClickEvent) {
        event.isCancelled = true
        if (!isValidClick(event)) return
        tryPlaySound(event.player)
        currentOption = (currentOption + 1) % options.size
        clickHandler(event)
    }
}

class CarouselItemBuilder(material: Material, amount: Int) {
    private val itemStack = ItemStack(material, amount)
    val meta = itemStack.itemMeta

    fun named(name: String) {
        meta.displayName(TextHelper.deserialize(name))
    }

    fun enchanted() {
        meta.itemFlags.add(ItemFlag.HIDE_ENCHANTS)
        meta.addEnchant(Enchantment.LUCK_OF_THE_SEA, 1, true)
    }
    
    fun lore(action: LoreBuilder.() -> Unit) {
        val builder = LoreBuilder()
        builder.line("press &6LEFT CLICK &rfor the next option")
        builder.line("")
        action(builder)
        meta.lore(builder.build())
    }
    
    fun build(): ItemStack {
        itemStack.itemMeta = meta
        return itemStack
    }
}