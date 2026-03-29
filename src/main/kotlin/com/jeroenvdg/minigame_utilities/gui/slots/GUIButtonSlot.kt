package com.jeroenvdg.minigame_utilities.gui.slots

import com.jeroenvdg.minigame_utilities.SoundHelper
import com.jeroenvdg.minigame_utilities.gui.MenuContainer
import com.jeroenvdg.minigame_utilities.gui.player
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import org.bukkit.event.inventory.InventoryClickEvent

fun MenuContainer.addButton(slot: Int, action: GUIButtonSlot.() -> Unit) {
    val button = GUIButtonSlot(slot, this)
    action(button)
    add(button)
}

open class GUIButtonSlot(slot: Int, container: MenuContainer) : GUISlot(slot, container) {

    protected var clickHandler: (InventoryClickEvent) -> Unit = {}

    var allowLeftClick = true
    var allowRightClick = false
    var cancelDoubleClick = true
    var allowHotBarSwitch = false
    var allowDrop = false
    var allowOffhandSwap = false
    var allowShiftClickIntoButton = false

    var playSound = true
    var clickSound = SoundHelper.Sounds.UIClick

    fun onClick(action: (event: InventoryClickEvent) -> Unit) {
        clickHandler = action
    }

    override fun handleSlotClicked(event: InventoryClickEvent) {
        event.isCancelled = true
        if (!isValidClick(event)) return
        tryPlaySound(event.player)
        clickHandler(event)
    }

    protected fun tryPlaySound(player: Player) {
        if (playSound) SoundHelper.play(player, clickSound)
    }

    protected fun isValidClick(event: InventoryClickEvent) : Boolean {
        val clickType = event.click
        if (!allowLeftClick && event.isLeftClick) return false
        if (!allowRightClick && event.isRightClick) return false
        if (cancelDoubleClick && (clickType == ClickType.DOUBLE_CLICK)) return false
        if (!allowHotBarSwitch && clickType == ClickType.NUMBER_KEY) return false
        if (!allowDrop && (clickType == ClickType.DROP || clickType == ClickType.CONTROL_DROP)) return false
        if (!allowOffhandSwap && clickType == ClickType.SWAP_OFFHAND) return false
        if (!allowShiftClickIntoButton && (clickType == ClickType.SHIFT_LEFT || clickType == ClickType.SHIFT_RIGHT)) return false
        return true
    }
}