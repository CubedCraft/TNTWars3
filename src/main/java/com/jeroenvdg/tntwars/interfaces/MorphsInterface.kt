package com.jeroenvdg.tntwars.interfaces

import com.addemann03.player_morphs.Morph
import com.addemann03.player_morphs.MorphTypes
import com.addemann03.player_morphs.managers.MorphManager
import com.jeroenvdg.minigame_utilities.*
import com.jeroenvdg.minigame_utilities.Textial.Companion.deserialize
import com.jeroenvdg.minigame_utilities.gui.guibuilders.ChestMenu
import com.jeroenvdg.minigame_utilities.gui.player
import com.jeroenvdg.minigame_utilities.gui.slots.addButton
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack
import kotlin.collections.set
import kotlin.math.ceil
import kotlin.math.max

class MorphsInterface : IPlayerGUI {

    companion object : GUISingleton<MorphsInterface>("Morphs") {
    }

    override val name get() = guiName

    lateinit var morphs: List<MorphTypes<Morph.MorphType<*>>> private set
    lateinit var selectedMorphs: HashMap<String, MorphTypes<*>> private set

    override fun create() {
        morphs = MorphTypes.VALUES
        selectedMorphs = HashMap()
    }

    override fun open(player: Player) {
        val morphData = selectedMorphs[player.uniqueId.toString()]
        var selectedSlot: ItemStack? = null
        val menu = ChestMenu("Morphs",max(1, ceil(morphs.size / 9f).toInt())) {
            for (i in morphs.indices) {
                val morphTypes = morphs[i]
                val morphType = morphTypes.type
                val info = morphType.info
                var morphItem: ItemStack
                if(morphData?.type?.morphClass == morphType.morphClass) {
                    morphItem = makeItem(info.material) {
                        named("&a${morphType.info.name} Morph")
                        enchant(Enchantment.UNBREAKING, 1)
                        flag(ItemFlag.HIDE_ENCHANTS)
                        flag(ItemFlag.HIDE_ATTRIBUTES)
                        setLore {
                            line("")
                            info.description.forEach {
                                line(it)
                            }
                            line("")
                            defaultLine("&x&F&C&D&0&5&C▶ ${Textial.toSmallText("click to unselect")}")
                        }
                    }
                    selectedSlot = morphItem
                } else {
                    morphItem = makeItem(info.material) {
                        named("&a${morphType.info.name} Morph")
                        flag(ItemFlag.HIDE_ATTRIBUTES)
                        flag(ItemFlag.HIDE_PLACED_ON)
                        setLore {
                            line("")
                            info.description.forEach {
                                line(it)
                            }
                            line("")
                            defaultLine("&x&F&C&D&0&5&C▶ ${Textial.toSmallText("click to select")}")
                        }
                    }
                }

                addButton(i) {
                    displayItem = morphItem
                    onClick { event ->
                        val isSelected = selectedMorphs[player.uniqueId.toString()]?.let {
                            (it.type.morphClass == morphType.morphClass)
                        } ?: false
                        if (isSelected) {
                            Soundial.play(event.player, Soundial.Fail)
                            displayItem = editItem(displayItem!!){
                                named("&a${morphType.info.name} Morph")
                                flag(ItemFlag.HIDE_ATTRIBUTES)
                                flag(ItemFlag.HIDE_PLACED_ON)
                                setLore {
                                    line("")
                                    info.description.forEach {
                                        line(it)
                                    }
                                    line("")
                                    defaultLine("&x&F&C&D&0&5&C▶ ${Textial.toSmallText("click to select")}")
                                }
                            }
                            selectedSlot = null
                            selectedMorphs[player.uniqueId.toString()]?.let { selectedMorphs.remove(player.uniqueId.toString()) }
                        } else {
                            selectedMorphs[event.player.uniqueId.toString()] = morphTypes
                            Soundial.play(event.player, Soundial.Success)
                            displayItem = editItem(morphItem) {
                                named("&a${morphType.info.name} Morph")
                                damageableMeta.isUnbreakable = false
                                damageableMeta.damage = 10


                                flag(ItemFlag.HIDE_ENCHANTS)
                                flag(ItemFlag.HIDE_ATTRIBUTES)
                                setLore {
                                    line("")
                                    info.description.forEach {
                                        line(it)
                                    }
                                    line("")
                                    defaultLine("&x&F&C&D&0&5&C▶ ${Textial.toSmallText("click to unselect")}")
                                }
                            }

                            selectedSlot?.let { it1 ->
                                editItem(it1) {
                                    named("&a${morphType.info.name} Morph")
                                    flag(ItemFlag.HIDE_ATTRIBUTES)
                                    flag(ItemFlag.HIDE_PLACED_ON)
                                    setLore {
                                        line("")
                                        info.description.forEach {
                                            line(it)
                                        }
                                        line("")
                                        defaultLine("&x&F&C&D&0&5&C▶ ${Textial.toSmallText("click to select")}")
                                    }
                                }
                            }
                            selectedSlot = morphItem
                        }
                    }
                }
            }
        }
        menu.open(player)
        Soundial.play(player, Soundial.UIOpen)
    }

}


