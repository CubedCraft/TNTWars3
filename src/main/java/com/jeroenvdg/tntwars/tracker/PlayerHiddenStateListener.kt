package com.jeroenvdg.tntwars.tracker

import com.github.retrooper.packetevents.event.PacketListenerAbstract
import com.github.retrooper.packetevents.event.PacketReceiveEvent
import com.github.retrooper.packetevents.event.PacketSendEvent
import com.github.retrooper.packetevents.protocol.packettype.PacketType
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerDestroyEntities
import com.jeroenvdg.minigame_utilities.Textial.Companion.deserialize
import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerHideEntityEvent

class PlayerHiddenStateListener : Listener {
    @EventHandler
    fun onPlayerHide(event: PlayerHideEntityEvent) {
        Bukkit.getOnlinePlayers().forEach {
            if(event.player.canSee(it)) Bukkit.broadcast(deserialize("${event.player.name} can see ${it.name}")) else Bukkit.broadcast(deserialize("${event.player.name} can not see ${it.name}"))
        }
    }
}