package com.jeroenvdg.tntwars.listeners

import com.jeroenvdg.minigame_utilities.gui.player
import com.jeroenvdg.tntwars.TNTWars
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.enchantments.Enchantment
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.block.BlockDropItemEvent
import org.bukkit.event.inventory.InventoryAction
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.player.PlayerDropItemEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerSwapHandItemsEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import kotlin.math.max

class GenericItemListener : Listener{

    companion object {
        val guiKey = NamespacedKey("com.jeroenvdg.missilewars.itemlistener", "gui")
        val clickableKey = NamespacedKey("com.jeroenvdg.missilewars.itemlistener", "clickable")
        val movableKey = NamespacedKey("com.jeroenvdg.missilewars.itemlistener", "movable")
        val droppableKey = NamespacedKey("com.jeroenvdg.missilewars.itemlistener", "droppable")
    }

    @EventHandler
    private fun handleKnockback(event: PlayerInteractEvent) {
        if (event.action != Action.RIGHT_CLICK_AIR && event.action != Action.RIGHT_CLICK_BLOCK) return

        val item = event.item ?: return
        if (item.type != Material.WOODEN_SPEAR) return

        if (event.player.isSneaking) {
            item.editMeta { meta ->
                val currentLevel = meta.getEnchantLevel(Enchantment.KNOCKBACK)

                // Om den är 10 eller mer, gå tillbaka till 1. Annars öka med 1.
                val nextLevel = if (currentLevel >= 5) 1 else currentLevel + 1

                // Vi sätter 'ignoreLevelRestriction' till true eftersom Minecrafts vanliga max är 2
                meta.removeEnchant(Enchantment.KNOCKBACK)
                meta.addEnchant(Enchantment.KNOCKBACK, nextLevel, true)
            }
        }
    }


    @EventHandler
    private fun handleClick(event: PlayerInteractEvent) {
        if (event.action != Action.RIGHT_CLICK_AIR && event.action != Action.RIGHT_CLICK_BLOCK) return

        val item = event.item ?: return
        val guiName = item.itemMeta.persistentDataContainer.get(guiKey, PersistentDataType.STRING) ?: return
        val gui = TNTWars.instance.guiManager.find(guiName) ?: return
        gui.open(event.player)
        event.isCancelled = true
    }

    @EventHandler
    fun handleBlockDrop(event: BlockDropItemEvent) {
        event.isCancelled = true
    }


    @EventHandler
    fun handleDrop(event: PlayerDropItemEvent) {
        val item = event.itemDrop.itemStack
        val isDroppable = item.itemMeta.persistentDataContainer.get(droppableKey, PersistentDataType.BOOLEAN) ?: true
        if (isDroppable) {
            event.itemDrop.remove()
            return
        } else{
            event.isCancelled = true
            return
        }
    }


    @EventHandler
    private fun handleInventoryClick(event: InventoryClickEvent) {
        fun checkItem(item: ItemStack?): Boolean {
            if (item == null) return false
            if (!item.hasItemMeta()) return false
            if (item.itemMeta.persistentDataContainer.get(movableKey, PersistentDataType.BOOLEAN) != false) return false
            event.isCancelled = true
            return true
        }

        if (event.slot < 0) return // Not really sure how this happens, would this break anything?

        if (event.action == InventoryAction.HOTBAR_SWAP) {
            val item = if (event.hotbarButton == -1) event.player.inventory.itemInOffHand else (event.clickedInventory ?: event.inventory).getItem(event.hotbarButton)
            if (checkItem(item)) return
        }

        val item = (event.clickedInventory ?: event.inventory).getItem(event.slot)
        if (checkItem(item)) return
        if (checkItem(event.currentItem)) return
        if (checkItem(event.cursor)) return
    }

    @EventHandler
    private fun handleItemSwap(event: PlayerSwapHandItemsEvent) {
        val itemA = event.mainHandItem
        val itemB = event.offHandItem

        if (itemA.hasItemMeta() && itemA.itemMeta.persistentDataContainer.get(movableKey, PersistentDataType.BOOLEAN) == false) {
            event.isCancelled = true
            return
        }
        if (itemB.hasItemMeta() && itemB.itemMeta.persistentDataContainer.get(movableKey, PersistentDataType.BOOLEAN) == false) {
            event.isCancelled = true
            return
        }
    }
}

