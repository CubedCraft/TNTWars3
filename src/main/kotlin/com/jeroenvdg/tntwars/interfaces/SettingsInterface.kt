package com.jeroenvdg.tntwars.interfaces

import com.jeroenvdg.minigame_utilities.SoundHelper
import com.jeroenvdg.minigame_utilities.Soundial
import com.jeroenvdg.minigame_utilities.gui.guibuilders.ChestMenu
import com.jeroenvdg.minigame_utilities.gui.slots.addCarousel
import com.jeroenvdg.minigame_utilities.gui.slots.addItem
import com.jeroenvdg.minigame_utilities.gui.slots.addToggle
import com.jeroenvdg.minigame_utilities.makeItem
import com.jeroenvdg.tntwars.listeners.GenericItemListener
import com.jeroenvdg.tntwars.player.PlayerManager
import com.jeroenvdg.tntwars.services.playerSettings.DispenserPlaceAssistLevel
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType

class SettingsInterface : IPlayerGUI {

    companion object : GUISingleton<SettingsInterface>("SettingsInterface") {
        lateinit var settingsItem: ItemStack private set
    }

    override val name: String get() = guiName

    override fun create() {
        settingsItem = makeItem(Material.REPEATER) {
            named("&aSettings")
            setLore("&7Click to open the settings menu")
            withPersistentData(GenericItemListener.guiKey, name)
            withPersistentData(GenericItemListener.movableKey, PersistentDataType.BOOLEAN, false)
            withPersistentData(GenericItemListener.droppableKey, PersistentDataType.BOOLEAN, false)
        }
    }

    override fun open(player: Player) {
        val user = PlayerManager.instance[player.uniqueId] ?: return
        SoundHelper.play(player, SoundHelper.Sounds.UIOpen)

        val menu = ChestMenu("Settings", 3) {
            addCarousel(0) {
                addOption(makeItem(Material.CHEST) {
                    named("&aChest Item Selector")
                    lore {
                        line("")
                        line("&7A chest will be added to the last hotbar slot")
                        line("&bRIGHT CLICK &7the chest to open the item selector")
                    }
                })
                addOption(makeItem(Material.FEATHER) {
                    named("&aOffhand Item Selector")
                    lore { line("&7Press the &bSWAP ITEM &7key to open the item selector") }
                })

                onClick { user.settings.offhandSelector = currentOption != 0 }
                currentOption = if (user.settings.offhandSelector) 1 else 0
            }

            addToggle(1) {
                name("&aRotate Fences")
                description("&eSHIFT + LEFT CLICK&7 on a fence or pressure plate to\n rotate to the next one\nitems: &fFence&7, &fPressure Plate")
                material(Material.BIRCH_FENCE)
                disabledMaterial(Material.OAK_FENCE)

                onClick { user.settings.rotateFence = isEnabled }
                isEnabled = user.settings.rotateFence
            }

//            addToggle(3) {
//                name("&aFriendly TNT Pushing")
//                description("Allow the tnt of team members to push TNT of your tnt\nThis means your tnt won't be able to push friendly tnt either!")
//                material(Material.TNT)
//                disabledMaterial(Material.STONE)
//
//                onClick { user.settings.friendlyTNTPushing = isEnabled }
//                isEnabled = user.settings.friendlyTNTPushing
//            }

            addCarousel(2) {
                addOption(makeItem(Material.DISPENSER) {
                    named("&aDispenser Place Assist: &cNone")
                    lore {
                        line("&8[&a✔&8] &fNo place assist")
                        line("&8[&7✔&8] &7Assist with placing dispensers when &6shifting")
                        line("&8[&7✔&8] &7Always assist with placing dispensers")
                    }
                })

                addOption(makeItem(Material.DISPENSER) {
                    named("&aDispenser Place Assist: &6Shift Only")
                    lore {
                        line("&8[&7✔&8] &7No place assist")
                        line("&8[&a✔&8] &fAssist with placing dispensers when &6shifting")
                        line("&8[&7✔&8] &7Always assist with placing dispensers")
                    }
                })

                addOption(makeItem(Material.DISPENSER) {
                    named("&aDispenser Place Assist: &aFull")
                    lore {
                        line("&8[&7✔&8] &7No place assist")
                        line("&8[&7✔&8] &7Assist with placing dispensers when &6shifting")
                        line("&8[&a✔&8] &fAlways assist with placing dispensers")
                    }
                })

                onClick { event ->
                    user.settings.dispenserAssistLevel = DispenserPlaceAssistLevel.entries[currentOption]
                }
                currentOption = user.settings.dispenserAssistLevel.ordinal
            }

            val pane = makeItem(Material.GRAY_STAINED_GLASS_PANE) { named(" ") }
            for (i in 0 until 9) {
                addItem(i + 18, pane)
            }
        }

        menu.open(player)
    }
}