package com.addemann03.player_morphs.listeners

import com.addemann03.player_morphs.managers.AbilityManager
import com.addemann03.player_morphs.managers.MorphManager
import com.github.retrooper.packetevents.event.PacketListenerAbstract
import com.github.retrooper.packetevents.event.PacketReceiveEvent
import com.github.retrooper.packetevents.protocol.packettype.PacketType
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientEntityAction
import org.bukkit.entity.Player

class AbilityListener(val abilityManager: AbilityManager) : PacketListenerAbstract() {
    override fun onPacketReceive(event: PacketReceiveEvent) {
        if(event.packetType == PacketType.Play.Client.ENTITY_ACTION) {
            val wrapper = WrapperPlayClientEntityAction(event)
            onPlayerAction(event.getPlayer(), wrapper)
        }
    }

    private fun onPlayerAction(player: Player, wrapper: WrapperPlayClientEntityAction) {
        val morphData = MorphManager.getMorphData(player)
        if(morphData != null) {
            val (clazz, morph) = morphData
            if(!morph.hasAbilities) return
            when(wrapper.action) {
                WrapperPlayClientEntityAction.Action.START_SNEAKING -> abilityManager.activateAbilities(morph)
                WrapperPlayClientEntityAction.Action.STOP_SNEAKING -> abilityManager.deactivateAbilities(morph)
                else -> {}
            }
        }
    }
}