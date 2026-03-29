package com.jeroenvdg.minigame_utilities.gui.slots

import com.jeroenvdg.minigame_utilities.gui.MenuContainer
import com.jeroenvdg.minigame_utilities.gui.player
import org.bukkit.Material
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.ItemStack

fun MenuContainer.addToggle(slot: Int, action: GUIToggleSlot.() -> Unit) {
    val toggle = GUIToggleSlot(slot, this)
    action(toggle)
    toggle.refresh()
    add(toggle)
}

class GUIToggleSlot(slot: Int, container: MenuContainer) : GUIButtonSlot(slot, container) {

    private var enabledItem: ItemStack? = null
    private var disabledItem: ItemStack? = null

    private var enabledMaterial: Material? = null
    private var disabledMaterial: Material? = null

    private var name: String? = null
    private var description: String? = null
    private var disabledName: String? = null
    private var disabledDescription: String? = null

    var isEnabled: Boolean = false
        set(value) {
            if (value == field) return
            field = value

            displayItem = if (value) enabledItem else disabledItem
        }

    fun material(material: Material) {
        enabledMaterial = material
    }

    fun disabledMaterial(material: Material) {
        disabledMaterial = material
    }

    fun name(name: String) {
        this.name = name
    }

    fun disabledName(name: String) {
        disabledName = name
    }

    fun description(description: String) {
        this.description = description
    }

    fun disabledDescription(description: String) {
        disabledDescription = description
    }

    fun refresh() {
        enabledItem = com.jeroenvdg.minigame_utilities.makeItem(enabledMaterial ?: throw Exception("enabledMaterial must be set!")) {
            named(name ?: throw Exception("Name must be set"))

            setLore {
                line("&aEnabled! &pLEFT CLICK&r to disable")
                line("")
                if (description != null) {
                    for (split in description!!.split("\n")) {
                        line(split)
                    }
                }
            }
        }

        disabledItem = com.jeroenvdg.minigame_utilities.makeItem(disabledMaterial ?: enabledMaterial!!) {
            named(disabledName ?: name!!)

            setLore {
                line("&cDisabled! &pLEFT CLICK&r to enable")
                line("")
                val description = disabledDescription ?: this@GUIToggleSlot.description
                if (description != null) {
                    for (split in description.split("\n")) {
                        line(split)
                    }
                }
            }
        }

        displayItem = if (isEnabled) enabledItem else disabledItem
    }

    override fun handleSlotClicked(event: InventoryClickEvent) {
        event.isCancelled = true
        if (!isValidClick(event)) return
        tryPlaySound(event.player)
        isEnabled = !isEnabled
        clickHandler(event)
    }
}