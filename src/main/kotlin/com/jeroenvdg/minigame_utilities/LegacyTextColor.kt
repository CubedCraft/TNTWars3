package com.jeroenvdg.minigame_utilities

import net.kyori.adventure.text.format.NamedTextColor

fun fromLegacyCode(code: Char): NamedTextColor? {
    return when (code) {
        '0' -> NamedTextColor.BLACK
        '1' -> NamedTextColor.DARK_BLUE
        '2' -> NamedTextColor.DARK_GREEN
        '3' -> NamedTextColor.DARK_AQUA
        '4' -> NamedTextColor.DARK_RED
        '5' -> NamedTextColor.DARK_PURPLE
        '6' -> NamedTextColor.GOLD
        '7' -> NamedTextColor.GRAY
        '8' -> NamedTextColor.DARK_GRAY
        '9' -> NamedTextColor.BLUE
        'a' -> NamedTextColor.GREEN
        'b' -> NamedTextColor.AQUA
        'c' -> NamedTextColor.RED
        'd' -> NamedTextColor.LIGHT_PURPLE
        'e' -> NamedTextColor.YELLOW
        'f' -> NamedTextColor.WHITE
        else -> null
    }
}