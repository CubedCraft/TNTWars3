package com.jeroenvdg.tntwars.interfaces

import com.jeroenvdg.minigame_utilities.Soundial
import com.jeroenvdg.minigame_utilities.Textial
import com.jeroenvdg.minigame_utilities.gui.guibuilders.ChestMenu
import com.jeroenvdg.minigame_utilities.gui.player
import com.jeroenvdg.minigame_utilities.gui.slots.addButton
import com.jeroenvdg.minigame_utilities.gui.slots.addItem
import com.jeroenvdg.minigame_utilities.makeItem
import com.jeroenvdg.minigame_utilities.setDisplayName
import com.jeroenvdg.tntwars.TNTWars
import com.jeroenvdg.tntwars.listeners.GenericItemListener
import com.jeroenvdg.tntwars.player.PlayerManager
import org.bukkit.Material
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import org.bukkit.inventory.ItemStack
import kotlin.math.ceil


class ExperimentalItemSelector : IPlayerGUI {

    companion object : GUISingleton<ExperimentalItemSelector>("ExperimentalItemSelector")

    override val name = guiName

    override fun open(player: Player) {
        val config = TNTWars.instance.config.experimentalItemSelectorConfig

        val rows = ceil(config.items.size / 9.0).toInt()

        var slot = 0
        val menu = ChestMenu("Experimental Items", rows + 1) {
            val noDispensers = config.items.filter{it != Material.DISPENSER}
            for (i in noDispensers.indices) {
                slot = i
                addButton(i) {
                    val material = config.items[i]
                    displayItem = when(material) {
                        Material.WOODEN_SPEAR -> {
                            makeItem(material) {
                                named("&cTNT Spear")
                                enchant(Enchantment.KNOCKBACK, 2)
                                setLore {
                                    line("Can be used for spear")
                                    line(" cannons in the game")
                                }
                            }
                        }
                        Material.MACE -> {
                            makeItem(material) {
                                namedDefault("&x&C&6&3&6&3&6T&x&C&C&4&C&4&CN&x&D&3&6&3&6&3T &x&D&F&8&F&8&FB&x&E&6&A&6&A&6o&x&E&C&B&C&B&Cn&x&F&2&D&2&D&2k&x&F&9&E&9&E&9e&x&F&F&F&F&F&Fr")
                                enchant(Enchantment.WIND_BURST, 3)
                                setLore {
                                    line("Bonk the explosives")
                                }
                            }
                        }
                        Material.DISPENSER -> {
                            makeItem(material) {
                                named("&eSand Dispenser")
                            }
                        }

                        else -> {
                            ItemStack(material)
                        }
                    }
                    allowHotBarSwitch = true

                    onClick { event ->
                        val player = event.player
                        if (event.clickedInventory == player.inventory) return@onClick

                        val item = ItemStack(material, material.maxStackSize)

                        if (material == Material.MACE) {
                            item.editMeta {
                                it.addEnchant(Enchantment.WIND_BURST, 3, true)
                            }

                            item.setDisplayName(Textial.deserialize("&x&C&6&3&6&3&6T&x&C&C&4&C&4&CN&x&D&3&6&3&6&3T &x&D&F&8&F&8&FB&x&E&6&A&6&A&6o&x&E&C&B&C&B&Cn&x&F&2&D&2&D&2k&x&F&9&E&9&E&9e&x&F&F&F&F&F&Fr"))
                        } else if (material == Material.WOODEN_SPEAR) {
                            item.editMeta {
                                it.addEnchant(Enchantment.KNOCKBACK, 2, true)
                            }

                            item.setDisplayName(Textial.deserialize("&cTNT Spear"))
                        } else if (material == Material.DISPENSER) {
                            item.setDisplayName(Textial.deserialize("&eSand Dispenser"))
                        } else if(material == Material.TNT_MINECART) {
                            item.editMeta {
                                it.setMaxStackSize(16)
                            }

                            item.amount = 16
                        }

                        if (event.click == ClickType.NUMBER_KEY) {
                            if (player.inventory.getItem(event.hotbarButton)?.itemMeta?.persistentDataContainer?.has(GenericItemListener.movableKey) == true) {
                                return@onClick
                            }
                            player.inventory.setItem(event.hotbarButton, item)
                        } else {
                            player.inventory.addItem(item)
                        }
                    }
                }
            }

            addButton(slot + 1) {
                displayItem = makeItem(Material.DISPENSER) {
                            named("&eSand Dispenser")
                        }

                allowHotBarSwitch = true

                onClick { event ->
                    val player = event.player
                    if (event.clickedInventory == player.inventory) return@onClick

                    val item = ItemStack(Material.DISPENSER, Material.DISPENSER.maxStackSize)

                    if (event.slot == slot + 1) {

                        item.setDisplayName(Textial.deserialize("&eSand Dispenser"))
                    }

                    if (event.click == ClickType.NUMBER_KEY) {
                        if (player.inventory.getItem(event.hotbarButton)?.itemMeta?.persistentDataContainer?.has(GenericItemListener.movableKey) == true) {
                            return@onClick
                        }
                        player.inventory.setItem(event.hotbarButton, item)
                    } else {
                        player.inventory.addItem(item)
                    }
                }
            }

            addButton(slot + 2) {
                displayItem = makeItem(Material.DISPENSER) {
                    named("&cTNT Minecart Dispenser")
                }

                allowHotBarSwitch = true

                onClick { event ->
                    val player = event.player
                    if (event.clickedInventory == player.inventory) return@onClick

                    val item = ItemStack(Material.DISPENSER, Material.DISPENSER.maxStackSize)

                    if (event.slot == slot + 2) {

                        item.setDisplayName(Textial.deserialize("&cTNT Minecart Dispenser"))
                    }

                    if (event.click == ClickType.NUMBER_KEY) {
                        if (player.inventory.getItem(event.hotbarButton)?.itemMeta?.persistentDataContainer?.has(GenericItemListener.movableKey) == true) {
                            return@onClick
                        }
                        player.inventory.setItem(event.hotbarButton, item)
                    } else {
                        player.inventory.addItem(item)
                    }
                }
            }

            val fillerItem = makeItem(Material.GRAY_STAINED_GLASS_PANE) { named(" ") }
            for (i in (rows * 9) until (rows * 9 + 9)) { addItem(i, fillerItem) }

            addButton(rows * 9 + 0) {
                displayItem = makeItem(Material.CHEST) {
                    named("&e&lNormal Items")
                    setLore{
                        defaultLine("&6Click to go back.")
                    }
                }
                clickSound = Soundial.Bass
                onClick { event ->
                    ItemSelector.open(event.player)
                }
            }
            addButton(rows * 9 + 2) {
                displayItem = makeItem(Material.LAVA_BUCKET) {
                    named("&e&lClear Inventory")
                    setLore{
                        defaultLine("&6Click to clear your inventory.")
                    }
                }
                clickSound = Soundial.Bass
                onClick { event ->
                    val user = PlayerManager.instance.get(event.player) ?: return@onClick
                    user.resetInventory()
                }
            }

            addButton(rows * 9 + 6) {
                displayItem = makeItem(Material.EMERALD) {
                    named("&e&lShop")
                    setLore{
                        defaultLine("&6Click to go the shop.")
                    }
                }
                playSound = false
                onClick { event ->
                    ShopInterface.open(event.player)
                }
            }
        }

        menu.open(player)
        Soundial.play(player, Soundial.UIOpen)
    }

    override fun create() {
    }
}