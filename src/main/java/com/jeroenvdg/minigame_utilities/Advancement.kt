package com.jeroenvdg.minigame_utilities

import com.github.retrooper.packetevents.protocol.advancements.AdvancementType
import com.github.retrooper.packetevents.protocol.item.type.ItemTypes
import com.github.retrooper.packetevents.protocol.player.ClientVersion
import com.github.retrooper.packetevents.resources.ResourceLocation
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import org.bukkit.NamespacedKey
import org.bukkit.inventory.ItemStack
import kotlin.properties.Delegates

class Advancement private constructor(val key: Key, private val icon: ItemStack) {
    var title: Component = Component.text("")
    fun title(text: Component): Advancement {
        this.title = text
        return this
    }

    var description: Component = Component.text("")
    fun description(text: Component): Advancement {
        this.description = text
        return this
    }

    var type: AdvancementType = AdvancementType.GOAL
    fun type(type: AdvancementType): Advancement {
        this.type = type
        return this
    }

    var background: ResourceLocation? = null
    fun background(background: NamespacedKey?): Advancement {
        this.background = background?.let{
            ResourceLocation(it.namespace, it.key)
        }
        return this
    }

    var showToast: Boolean = false
    fun showToast(state: Boolean): Advancement {
        this.showToast = state
        return this
    }

    var hidden: Boolean = false
    fun hidden(state: Boolean): Advancement {
        this.hidden = state
        return this
    }

    var x by Delegates.notNull<Float>()
    fun x(x: Float): Advancement {
        this.x = x
        return this
    }

    var y by Delegates.notNull<Float>()
    fun y(y: Float): Advancement {
        this.y = y
        return this
    }

    fun getIcon(): com.github.retrooper.packetevents.protocol.item.ItemStack {
        return com.github.retrooper.packetevents.protocol.item.ItemStack.builder()
            .type(ItemTypes.builder(icon.type.name).setMaxAmount(icon.amount).build())
            .amount(icon.amount)
            .version(ClientVersion.V_1_21_11)
            .build()
    }
}