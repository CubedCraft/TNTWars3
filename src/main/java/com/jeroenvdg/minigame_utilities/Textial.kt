package com.jeroenvdg.minigame_utilities

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TextComponent
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.Style
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.bukkit.Color

enum class Textial {
    Black('0', NamedTextColor.BLACK),
    DarkBlue('1', NamedTextColor.DARK_BLUE),
    DarkGreen('2', NamedTextColor.DARK_GREEN),
    DarkAqua('3', NamedTextColor.DARK_AQUA),
    DarkRed('4', NamedTextColor.DARK_RED),
    DarkPurple('5', NamedTextColor.DARK_PURPLE),
    Gold('6', NamedTextColor.GOLD),
    Gray('7', NamedTextColor.GRAY),
    DarkGray('8', NamedTextColor.DARK_GRAY),
    Blue('9', NamedTextColor.BLUE),
    Green('a', NamedTextColor.GREEN),
    Aqua('b', NamedTextColor.AQUA),
    Red('c', NamedTextColor.RED),
    LightPurple('d', NamedTextColor.LIGHT_PURPLE),
    Yellow('e', NamedTextColor.YELLOW),
    White('f', NamedTextColor.WHITE),
    Primary('p', NamedTextColor.WHITE),
    Secondary('s', NamedTextColor.WHITE),
    Warning('w', NamedTextColor.WHITE),
    Reset('r', NamedTextColor.WHITE);


    companion object {
        private val c_t = HashMap<Char, Textial>()
        private val n_t = HashMap<String, Textial>()
        private val colorRegex = Regex("[_ -]")

        val colorMap = HashMap<String, Textial>()

        val summary = TextialParser("", Aqua, DarkGray, Red, White)
        val cmd = TextialParser("&cTW &7&l» &r", White, Gold, Red, Gray)
        val bc = TextialParser("", Aqua, Aqua, Red, Green)
        val msg = TextialParser("", Aqua, Yellow, Red, Gray)
        val info = TextialParser("&7ℹ ", Aqua, Aqua, Red, Green)
        val bossbar = TextialParser("", Gold, Yellow, Red, White)
        val debug = TextialParser("&f&lDEBUG &7&l» &r", Aqua, Gold, Red, Gray)

        val doubleArrowSymbol = '»'
        val line = cmd.format("&f&m                                                                       ")
        val bcLine = bc.format("&f&m                                                                                ")

        fun info(text: String): TextComponent {
            return Component.text("ℹ ").color(NamedTextColor.GRAY).append(deserialize(text))
        }
        fun deserialize(text: String, defaultDecoration: Boolean = false): TextComponent {
            if(!defaultDecoration) return LegacyComponentSerializer.legacyAmpersand().deserialize(text).decoration(TextDecoration.ITALIC, false) else return LegacyComponentSerializer.legacyAmpersand().deserialize(text)
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

        fun get(char: Char): Textial? {
            return c_t[char]
        }


        fun get(name: String) = n_t[name.uppercase().replace(colorRegex, "")]


        init {
            for (value in entries) {
                c_t[value.char] = value
                n_t[value.name.uppercase()] = value
                colorMap[value.name] = value
            }
         }
    }


    val char: Char
    val color: TextColor
    val namedColor: NamedTextColor

    constructor(char: Char, nc: NamedTextColor) {
        this.char = char
        this.color = nc.toColor().toTextColor()
        namedColor = nc
    }
}


class TextialParser(prefix: String, val primary: Textial, val secondary: Textial, val warning: Textial, val reset: Textial) {
    companion object {
        private val regex = Regex("&([0-9a-flnokmpswr])")
    }

    val prefixComp = parse(prefix)
    val prefix = str(prefix, false)


    fun getColor(color: Textial): TextColor = when(color) {
        Textial.Primary -> primary.color
        Textial.Secondary -> secondary.color
        Textial.Warning -> warning.color
        Textial.Reset -> reset.color
        else -> color.color
    }


    fun getColor(color: String): TextColor {
        if (color.length > 2) throw Exception("Invalid color code $color")
        if (color.length == 2 && "&§".indexOf(color[0]) < 0) throw Exception("Invalid color code $color")
        return getColor(color.last())
    }


    fun getColor(color: Char): TextColor {
        val colorVal = Textial.get(color) ?: throw Exception("Invalid color code $color")
        return getColor(colorVal)
    }


    fun getColorByName(name: String): TextColor? {
        val colorVal = Textial.get(name) ?: return null
        return getColor(colorVal)
    }


    fun format(msg: String): TextComponent {
        return prefixComp.append(parse(msg))
    }


    fun str(msg: String, withPrefix: Boolean = true): String {
        if (msg.length <= 1) return msg
        val b = "${if (withPrefix) prefix else ""}$msg".toCharArray()
        for (i in b.indices-2) {
            if (b[i] != '&' || "0123456789abcdefpswrABCDEFPSWRlnokmLNOKM".indexOf(b[i+1]) < 0) continue
            b[i] = '§'
            b[i+1] = when(b[i+1]) {
                'p', 'P' -> primary.char
                's', 'S' -> secondary.char
                'w', 'W' -> warning.char
                'r', 'R' -> reset.char
                else -> b[i+1].lowercaseChar()
            }
        }
        return b.joinToString("")
    }

    fun parse(msg: List<String>): TextComponent {
        if (msg.size == 1) {
            return parseSingle(msg[0])
        }

        val comp = Component.text()
        for (i in msg.indices) {
            comp.append(parseSingle(msg[i]))
            if (i < msg.size - 1) {
                comp.appendNewline()
            }
        }
        return comp.build()
    }

    fun parse(msg: String): TextComponent {
        val lines = msg.split('\n')
        if (lines.size == 1) {
            return parseSingle(msg)
        }

        val comp = Component.text()
        for (i in lines.indices) {
            comp.append(parseSingle(lines[i]))
            if (i < lines.size - 1) {
                comp.appendNewline()
            }
        }
        return comp.build()
    }

    private fun parseSingle(msg: String) : TextComponent {
        val matchResults = regex.findAll(msg).toList()
        val comp = Component.text()

        var color = getColor(reset)

        var bold = false
        var strikeThrough = false
        var underline = false
        var italic = false
        var obfuscated = false

        var lastEnd = 0

        fun appendComp(start: Int, end: Int) {
            val styleBuilder = Style.style().color(color)
            if (bold) styleBuilder.decorate(TextDecoration.BOLD)
            if (strikeThrough) styleBuilder.decorate(TextDecoration.STRIKETHROUGH)
            if (underline) styleBuilder.decorate(TextDecoration.UNDERLINED)
            if (obfuscated) styleBuilder.decorate(TextDecoration.OBFUSCATED)
            styleBuilder.decoration(TextDecoration.ITALIC, italic)

            comp.append(Component.text(msg.subSequence(start, end).toString()).style(styleBuilder.build()))
        }

        fun setColor(newColor: Textial) {
            bold = false
            strikeThrough = false
            underline = false
            italic = false
            obfuscated = false

            color = getColor(newColor)
        }

        if (matchResults.isEmpty()) {
            appendComp(0, msg.length)
            return comp.build()
        }

        for (i in matchResults.indices) {
            val matchResult = matchResults[i]

            if (matchResult.range.first - lastEnd > 0) {
                appendComp(lastEnd, matchResult.range.first)
            }
            lastEnd = matchResult.range.last+1

            when (matchResult.groups[1]!!.value) {
                "0" -> setColor(Textial.Black)
                "1" -> setColor(Textial.DarkBlue)
                "2" -> setColor(Textial.DarkGreen)
                "3" -> setColor(Textial.DarkAqua)
                "4" -> setColor(Textial.DarkRed)
                "5" -> setColor(Textial.DarkPurple)
                "6" -> setColor(Textial.Gold)
                "7" -> setColor(Textial.Gray)
                "8" -> setColor(Textial.DarkGray)
                "9" -> setColor(Textial.Blue)
                "a" -> setColor(Textial.Green)
                "b" -> setColor(Textial.Aqua)
                "c" -> setColor(Textial.Red)
                "d" -> setColor(Textial.LightPurple)
                "e" -> setColor(Textial.Yellow)
                "f" -> setColor(Textial.White)
                "p" -> setColor(Textial.Primary)
                "s" -> setColor(Textial.Secondary)
                "r" -> setColor(Textial.Reset)
                "w" -> setColor(Textial.Warning)
                "l" -> bold = true
                "n" -> underline = true
                "o" -> italic = true
                "k" -> obfuscated = true
                "m" -> strikeThrough = true
            }

            if (i == matchResults.size-1) {
                appendComp(matchResult.range.last+1, msg.length)
            }
        }

        return comp.build()
    }
}


fun TextColor.toColor() : Color {
    return Color.fromRGB(this.red(), this.green(), this.blue())
}

fun Color.toTextColor() : TextColor {
    return TextColor.color(this.red, this.green, this.blue)
}