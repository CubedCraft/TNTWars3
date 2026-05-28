package xyz.pondwader.replay_engine.replay

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.InventoryHolder
import org.bukkit.inventory.ItemStack

internal class ReplayPlayerMenu(
    private val teleportToReplayPlayer: (ReplayPlayerTarget) -> Unit,
    targets: List<ReplayPlayerTarget>,
) : InventoryHolder {
    private val targetsBySlot = HashMap<Int, ReplayPlayerTarget>()
    private val menuInventory: Inventory = Bukkit.createInventory(this, inventorySize(targets.size), Component.text("Replay Players"))

    init {
        for ((index, target) in targets.withIndex()) {
            targetsBySlot[index] = target
            menuInventory.setItem(index, playerHead(target))
        }
    }

    override fun getInventory(): Inventory {
        return menuInventory
    }

    fun handleClick(event: InventoryClickEvent) {
        val target = targetsBySlot[event.rawSlot] ?: return
        teleportToReplayPlayer(target)
        event.whoClicked.closeInventory()
    }

    private fun playerHead(target: ReplayPlayerTarget): ItemStack {
        val item = ItemStack(Material.PLAYER_HEAD)
        item.editMeta { meta ->
            meta.displayName(Component.text(target.name, NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false))
            meta.lore(listOf(Component.text("Click to teleport", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)))
        }
        return item
    }

    private companion object {
        fun inventorySize(itemCount: Int): Int {
            return ((itemCount.coerceAtLeast(1) + 8) / 9).coerceAtMost(6) * 9
        }
    }
}
