package com.jeroenvdg.minigame_utilities.commands

import com.jeroenvdg.minigame_utilities.TextHelper
import com.jeroenvdg.minigame_utilities.commands.builders.setCommand
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TextComponent
import net.kyori.adventure.text.format.NamedTextColor
import kotlin.math.min


data class PageData(val page: Int, var pageSize: Int, val elements: Int) {
    val pageCount get() = (elements-1) / pageSize + 1
    val start get() = (page-1) * pageSize
    val end get() = min(start + pageSize, elements)-1
    val isInvalidPage get() = page < 1 || start > elements
    val hasPages get() = pageCount > 1

    fun iterateElements(action: (index: Int) -> Unit) {
        for (i in start until end+1) action(i)
    }

    fun iterateEmpty(action: (index: Int) -> Unit) {
        if (page != pageCount) return

        val start = elements
        val max = pageSize * (pageCount)

        for (i in start until max) action(i)
    }

    fun previousPageArrow(cmdTemplate: String): TextComponent {
        val arrowComp = Component.text("«")
        if (page == 1) arrowComp.color(NamedTextColor.GRAY)
        else arrowComp.color(NamedTextColor.WHITE).setCommand(cmdTemplate.replace("_index_", (page-1).toString()))
        return arrowComp
    }

    fun nextPageArrow(cmdTemplate: String): TextComponent {
        val arrowComp = Component.text("»")
        if (page == pageCount) arrowComp.color(NamedTextColor.GRAY)
        else arrowComp.color(NamedTextColor.WHITE).setCommand(cmdTemplate.replace("_index_", (page+1).toString()))
        return arrowComp
    }

    fun pageTextComponent(cmdTemplate: String): TextComponent {
        return Component.text()
            .append(previousPageArrow(cmdTemplate))
            .append(TextHelper.deserialize(" &7$page/$pageCount "))
            .append(nextPageArrow(cmdTemplate))
            .build()
    }

    companion object {
        const val PaginatorDecorationSize: Int = 3
    }
}


fun paginate(header: TextComponent, pageData: PageData, action: (index: Int) -> TextComponent): TextComponent {
    val comp = Component.text()
        .append(TextHelper.line).appendNewline()
        .append(TextHelper.prefixComp)
        .append(header)
        .appendNewline()

    //  Build the body of the message

    pageData.iterateElements { i ->
        val char = chars[if (i == pageData.end) 2 else 0]
        comp.append(TextHelper.deserialize(" &r$char "))
        comp.append(action(i))
        comp.appendNewline()
    }

    pageData.iterateEmpty {
        comp.append(TextHelper.deserialize("\n"))
    }

    //  Build the footer of the message

    comp.append(TextHelper.line)
    return comp.build()
}