package com.jeroenvdg.tntwars.interfaces

import com.jeroenvdg.tntwars.TNTWars
import org.bukkit.entity.Player

interface IPlayerGUI {
    val name: String
    fun open(player: Player)
    fun create()
}


class GUIManager : Iterable<IPlayerGUI> {
    private val interfaces = HashMap<String, IPlayerGUI>()

    fun find(name: String): IPlayerGUI? = interfaces[name]
    fun add(gui: IPlayerGUI, create: Boolean = true) {
        if (create) gui.create()
        interfaces[gui.name] = gui
    }

    fun clear() { interfaces.clear() }

    override fun iterator() = interfaces.values.iterator()
}


open class GUISingleton<T: IPlayerGUI>(val guiName: String) {
    val instance: T get() = TNTWars.instance.guiManager.find(guiName) as T

    fun open(player: Player) {
        instance.open(player)
    }
}