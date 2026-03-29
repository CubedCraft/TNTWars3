package com.jeroenvdg.minigame_utilities.commands.builders

import com.jeroenvdg.minigame_utilities.Textial
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TextComponent
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.event.HoverEvent


fun TextComponent.Builder.setHoverText(text: Component): TextComponent.Builder {
    return this.hoverEvent(HoverEvent.showText(text));
}


fun TextComponent.Builder.append(text: String): TextComponent.Builder {
    append(Component.text(text))
    return this
}


fun TextComponent.Builder.setCommand(text: String, includeHover: Boolean = true): TextComponent.Builder {
    if (includeHover) this.setHoverText(Textial.cmd.parse("&rRun command /&p${text.drop(1)}"))
    return this.clickEvent(ClickEvent.runCommand(text))
}


fun TextComponent.setHoverText(text: TextComponent): TextComponent {
    return this.hoverEvent(HoverEvent.showText(text));
}


fun TextComponent.setCommand(text: String, includeHover: Boolean = true): TextComponent {
    var comp = this
    if (includeHover) comp = comp.setHoverText(Textial.cmd.parse("&rRun command /&p${text.drop(1)}"))
    return comp.clickEvent(ClickEvent.runCommand(text))
}


fun TextComponent.setSuggestion(text: String, includeHover: Boolean = true): TextComponent {
    var comp = this
    if (includeHover) comp = comp.setHoverText(Textial.cmd.parse("&rSuggest command /&p${text.drop(1)}"))
    return comp.clickEvent(ClickEvent.suggestCommand(text))
}