package com.jeroenvdg.minigame_utilities

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TextComponent
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeModifier
import org.bukkit.enchantments.Enchantment
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.SkullMeta
import org.bukkit.persistence.PersistentDataType
import org.bukkit.plugin.java.JavaPlugin

class ItemBuilder(val itemStack: ItemStack) {

    companion object {
        private lateinit var plugin: JavaPlugin
        fun setup(plugin: JavaPlugin) {
            ItemBuilder.plugin = plugin;
        }
    }

    val meta = itemStack.itemMeta
    private var textial = Textial.msg

    val skullMeta get() = meta as SkullMeta


    fun textial(textial: TextialParser): ItemBuilder {
        this.textial = textial
        return this
    }

    fun amount(amount: Int) { itemStack.amount = amount }

    fun namedDefault(name: String) = this.named(Textial.deserialize(name))
    fun named(name: String) = this.named(textial.parse(name))
    fun named(name: TextComponent) = meta.displayName(name)

    fun setLore(lore: String) = setLore(textial.parse(lore))
    fun setLore(lore: TextComponent) = meta.lore(listOf(lore))
    fun setLore(action: LoreBuilder.() -> Unit) {
        val builder = LoreBuilder(textial)
        action(builder)
        meta.lore(builder.build())
    }

    fun withPersistentData(name: String, data: String) = meta.persistentDataContainer.set(NamespacedKey(plugin, name), PersistentDataType.STRING, data)
    fun <T, Z> withPersistentData(name: String, type: PersistentDataType<T, Z>, data: Z & Any) = meta.persistentDataContainer.set(NamespacedKey(plugin, name), type, data)
    fun withPersistentData(key: NamespacedKey, data: String) = meta.persistentDataContainer.set(key, PersistentDataType.STRING, data)
    fun <T, Z> withPersistentData(key: NamespacedKey, type: PersistentDataType<T, Z>, data: Z & Any) = meta.persistentDataContainer.set(key, type, data)
    fun flag(itemFlag: ItemFlag) = meta.addItemFlags(itemFlag)
    fun attribute(attribute: Attribute, modifier: AttributeModifier) = meta.addAttributeModifier(attribute, modifier)
    fun enchant(enchantment: Enchantment, level: Int) = meta.addEnchant(enchantment, level, true)


    fun build(): ItemStack {
        itemStack.itemMeta = meta
        return itemStack
    }
}


class LoreBuilder(private val textial: TextialParser) {
    private val components = ArrayList<TextComponent>()

    fun defaultLine(string: String) {
        if (string.contains("\n")) {
            for (line in string.split('\n')) {
                this.line(Textial.deserialize(line))
            }
        } else {
            return this.line(Textial.deserialize(string))
        }
    }
    fun line(string: String) {
        if (string.contains("\n")) {
            for (line in string.split('\n')) {
                this.line(textial.parse(line))
            }
        } else {
            return this.line(textial.parse(string))
        }
    }

    fun line(component: TextComponent) {
        components.add(component)
    }

    fun empty() {
        components.add(Component.empty())
    }

    fun build() = components
}


fun makeItem(material: Material, action: ItemBuilder.() -> Unit): ItemStack {
    val builder = ItemBuilder(ItemStack(material))
    action(builder)
    return builder.build()
}


fun makeItem(material: Material, amount: Int, action: ItemBuilder.() -> Unit): ItemStack {
    val builder = ItemBuilder(ItemStack(material, amount))
    action(builder)
    return builder.build()
}


fun editItem(itemStack: ItemStack, action: ItemBuilder.() -> Unit): ItemStack {
    val builder = ItemBuilder(itemStack)
    action(builder)
    return builder.build()
}
