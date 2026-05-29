package com.jeroenvdg.tntwars.listeners

import com.jeroenvdg.tntwars.player.PlayerManager
import org.bukkit.entity.Minecart
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.vehicle.VehicleDamageEvent
import org.bukkit.event.vehicle.VehicleDestroyEvent
import org.bukkit.event.vehicle.VehicleEntityCollisionEvent

class VehicleListener : Listener {
    @EventHandler
    fun onVehicleDestroy(event: VehicleDestroyEvent) {
        val vehicle = event.vehicle
        val bukkitPlayer = event.attacker
        if(bukkitPlayer !is Player && vehicle !is Minecart) return
        val player = PlayerManager.instance.get(bukkitPlayer as Player) ?: return
        if(!player.team.isSpectatorTeam) return
        event.isCancelled = true
    }
    @EventHandler
    fun onVehicleDamage(event: VehicleDamageEvent) {
        val vehicle = event.vehicle
        val bukkitPlayer = event.attacker
        if(bukkitPlayer !is Player && vehicle !is Minecart) return
        val player = PlayerManager.instance.get(bukkitPlayer as Player) ?: return
        if(!player.team.isSpectatorTeam) return
        event.isCancelled = true
    }

    @EventHandler
    fun onVehicleCollision(event: VehicleEntityCollisionEvent) {
        val vehicle = event.vehicle
        val bukkitPlayer = event.entity
        if(bukkitPlayer !is Player) return
        if(vehicle !is Minecart) return
        val player = PlayerManager.instance.get(bukkitPlayer) ?: return
        if(!player.team.isSpectatorTeam) return
        event.isCancelled = true
    }
}