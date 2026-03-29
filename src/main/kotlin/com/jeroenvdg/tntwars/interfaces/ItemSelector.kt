package com.jeroenvdg.tntwars.interfaces

import com.jeroenvdg.minigame_utilities.SoundHelper
import com.jeroenvdg.minigame_utilities.Soundial
import com.jeroenvdg.minigame_utilities.gui.guibuilders.ChestMenu
import com.jeroenvdg.minigame_utilities.gui.guibuilders.IMenu
import com.jeroenvdg.minigame_utilities.gui.player
import com.jeroenvdg.minigame_utilities.gui.slots.addButton
import com.jeroenvdg.minigame_utilities.gui.slots.addItem
import com.jeroenvdg.minigame_utilities.makeItem
import com.jeroenvdg.minigame_utilities.setDisplayName
import com.jeroenvdg.tntwars.TNTWars
import com.jeroenvdg.tntwars.listeners.GenericItemListener
import com.jeroenvdg.tntwars.player.PlayerManager
import io.papermc.paper.datacomponent.DataComponentTypes
import io.papermc.paper.datacomponent.item.Tool
import io.papermc.paper.registry.RegistryKey
import io.papermc.paper.registry.keys.BlockTypeKeys
import io.papermc.paper.registry.set.RegistrySet
import net.kyori.adventure.util.TriState
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import kotlin.math.ceil


@Suppress("UnstableApiUsage")
class ItemSelector : IPlayerGUI {

    companion object : GUISingleton<ItemSelector>("ItemSelector") {
        lateinit var item: ItemStack private set
    }

    override val name = guiName

    private lateinit var menu: IMenu

    override fun open(player: Player) {
        menu.open(player)
        SoundHelper.play(player, SoundHelper.Sounds.UIOpen)
    }

    override fun create() {
        val config = TNTWars.instance.config.itemSelectorConfig

        item = makeItem(config.selectorItem) {
            named("&aItem Selector")
            setLore("&7Right click to select")
            withPersistentData(GenericItemListener.guiKey, guiName)
            withPersistentData(GenericItemListener.movableKey, PersistentDataType.BOOLEAN, false)
            withPersistentData(GenericItemListener.droppableKey, PersistentDataType.BOOLEAN, false)
        }

        val rows = ceil(config.items.size / 9.0).toInt()

        menu = ChestMenu("Item Selector", rows + 1) {
            for (i in config.items.indices) {
                addButton(i) {
                    val material = config.items[i]
                    if (material == Material.DIAMOND_PICKAXE) {
                        displayItem = makeItem(material) {
                            named("&fMultitool")
                            setLore {
                                line("Can be used to quickly break")
                                line(" every block in the game")
                            }
                        }
                    } else {
                        displayItem = ItemStack(material)
                    }
                    allowHotBarSwitch = true

                    onClick { event ->
                        val player = event.player
                        if (event.clickedInventory == player.inventory) return@onClick

                        val item = ItemStack(material, material.maxStackSize)

                        if (material == Material.DIAMOND_PICKAXE) {
                            item.setData(DataComponentTypes.TOOL, Tool.tool()
                                .addRules(ItemStack(Material.DIAMOND_SHOVEL).getData(DataComponentTypes.TOOL)!!.rules())
                                .addRules(ItemStack(Material.DIAMOND_AXE).getData(DataComponentTypes.TOOL)!!.rules().map { Tool.rule(it.blocks(), it.speed()?.times(2f) ?: 1f, it.correctForDrops()) })
                                .addRules(ItemStack(Material.DIAMOND_PICKAXE).getData(DataComponentTypes.TOOL)!!.rules().map { Tool.rule(it.blocks(), it.speed()?.times(3f) ?: 1f, it.correctForDrops()) })
                                .addRules(ItemStack(Material.SHEARS).getData(DataComponentTypes.TOOL)!!.rules())
                                .addRule(Tool.rule(RegistrySet.keySet(RegistryKey.BLOCK, BlockTypeKeys.COBWEB), 100f, TriState.FALSE))
                                .defaultMiningSpeed(10f)
                                .damagePerBlock(0)
                                .build())

                            item.setDisplayName("&fMultitool")
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

            val fillerItem = makeItem(Material.GRAY_STAINED_GLASS_PANE) { named(" ") }
            for (i in (rows * 9) until (rows * 9 + 9)) { addItem(i, fillerItem) }

            addButton(rows * 9 + 2) {
                displayItem = makeItem(Material.LAVA_BUCKET) { named("&aClear Inventory") }
                clickSound = SoundHelper.Sounds.Bass
                onClick { event ->
                    val user = PlayerManager.instance.get(event.player) ?: return@onClick
                    user.resetInventory()
                }
            }

            addButton(rows * 9 + 6) {
                displayItem = makeItem(Material.EMERALD) { named("&aShop") }
                playSound = false
                onClick { event ->
                    ShopInterface.open(event.player)
                }
            }
        }
    }
}