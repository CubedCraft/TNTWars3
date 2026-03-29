package com.jeroenvdg.minigame_utilities

import com.jeroenvdg.minigame_utilities.Textial.Companion.bc
import com.jeroenvdg.minigame_utilities.Textial.Companion.cmd
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TextComponent
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.bukkit.Material
import org.bukkit.entity.Player

object TextHelper {
    val debug: String = "&3[&lDebug&3]" + " "
    val prefix: String = "&c[&lTNTWars&c]" + " "
    val parameterColor: String = "&x&F&C&D&0&5&C"
    val mainColor: String = "&x&3&3&9&8&D&2"
    val infoPrefix = "&7ℹ" + " "
    val prefixComp: TextComponent
        get() = deserialize(prefix)
    val line = cmd.format("&f&m                                                                       ")
    val bcLine = bc.format("&f&m                                                                                ")
    class MiniMessageSerializer {
        private val serializer: MiniMessage = MiniMessage.miniMessage()
        fun serialize(component: Component): String {
            return serializer.serialize(component)
        }

        fun deserialize(text: String): Component {
            return serializer.deserialize(text)
        }
    }
    val minimessage: MiniMessageSerializer = MiniMessageSerializer()

    fun format(text: String): TextComponent {
        return prefixComp.append(deserialize(text))
    }

    fun format(component: TextComponent): TextComponent {
        return prefixComp.append(component)
    }

    fun info(text: String): TextComponent {
        return deserialize(infoPrefix).append(deserialize(text))
    }

    fun error(text: String): TextComponent {
        return deserialize(text).color(NamedTextColor.RED)
    }
    fun deserialize(text: String, defaultDecoration: Boolean = false): TextComponent {
        if(!defaultDecoration) return LegacyComponentSerializer.legacyAmpersand().deserialize(text).decoration(TextDecoration.ITALIC, false) else return LegacyComponentSerializer.legacyAmpersand().deserialize(text)
    }
    fun deserialize(vararg lines: String): TextComponent {
        val baseComponent = Component.text()
        for(line in lines) {
            baseComponent.append(deserialize(line))
                if(lines.indexOf(line) != lines.lastIndex) baseComponent.appendNewline()
        }
        return baseComponent.build()
    }

    fun serialize(component: TextComponent): String {
        return LegacyComponentSerializer.legacyAmpersand().serialize(component)
    }

    fun head(player: Player): String {
        return "<head:${player.uniqueId}>"
    }

    fun sprite(type: Material): String {
        return if(type.isBlock) "<sprite:blocks:block/${type.name.lowercase()}>" else "<sprite:items:item/${type.name.lowercase()}>"
    }

    fun toSmallText(text: String): String {
        val normal = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        val small = "ᴀʙᴄᴅᴇꜰɢʜɪᴊᴋʟᴍɴᴏᴘǫʀѕᴛᴜᴠᴡxʏᴢ⁰¹²³⁴⁵⁶⁷⁸⁹"
        val chars = text.uppercase().toCharArray()
        val builder = StringBuilder()
        chars.forEach{
            val index = normal.indexOf(it)
            val newChar = small.getOrNull(index)
            if(index == -1 || newChar == null) {
                builder.append(it)
                return@forEach
            }

            builder.append(newChar)
        }

        return builder.toString()
    }
}