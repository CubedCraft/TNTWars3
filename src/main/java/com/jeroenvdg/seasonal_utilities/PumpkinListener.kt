package com.jeroenvdg.seasonal_utilities

import com.jeroenvdg.tntwars.TNTWars
import com.jeroenvdg.tntwars.listeners.WorldOwnershipManager.Companion.getOwner
import com.jeroenvdg.tntwars.listeners.WorldOwnershipManager.Companion.setOwner
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.entity.TNTPrimed
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityExplodeEvent
import org.bukkit.persistence.PersistentDataType
import org.bukkit.util.Vector
import kotlin.math.cos
import kotlin.math.sin

class PumpkinListener : Listener {

    companion object {
        val tntKey = NamespacedKey("tntwars", "tnt")

        fun onTNTExplode(event: EntityExplodeEvent) {
            val tnt = event.entity as? TNTPrimed ?: return
            val blocks = event.blockList()

            blocks.filter {
                it.type in listOf(
                Material.CARVED_PUMPKIN,
                Material.JACK_O_LANTERN,
                Material.PUMPKIN
            )
            }.forEach { pumpkin ->
                pumpkin.type = Material.AIR

                //TODO Add Point DB or YAML

                println("pumpkin")

                pumpkin.world.spawn(pumpkin.location, TNTPrimed::class.java) { newTNT ->
                    tnt.getOwner()?.let { newTNT.setOwner(it) }
                    newTNT.source = tnt.source

                    newTNT.fuseTicks = (20..30).random()

                    when ((1..5).random()) {
                        1 -> {
                            newTNT.yield = 6f
                        }

                        2 -> {
                            //tnt ring
                            newTNT.persistentDataContainer.set(tntKey, PersistentDataType.INTEGER, 0)
                            newTNT.velocity = Vector(0.0, 3.0, 0.0)
                        }

                        3 -> {
                            newTNT.persistentDataContainer.set(tntKey, PersistentDataType.INTEGER, 1)
                        }

                        4 -> {
                            pumpkin.world.spawn(pumpkin.location, TNTPrimed::class.java) { newerTNT ->
                                tnt.getOwner()?.let { newerTNT.setOwner(it) }
                                newerTNT.source = tnt.source
                                newerTNT.fuseTicks = newTNT.fuseTicks + 5
                            }
                            pumpkin.world.spawn(pumpkin.location, TNTPrimed::class.java) { newerTNT ->
                                tnt.getOwner()?.let { newerTNT.setOwner(it) }
                                newerTNT.source = tnt.source
                                newerTNT.fuseTicks = newTNT.fuseTicks + 10
                            }
                        }
                    }
                }
            }

            when (tnt.persistentDataContainer.get(tntKey, PersistentDataType.INTEGER)) {
                0 -> {
                    for (i in (1..10)) {
                        tnt.world.spawn(tnt.location, TNTPrimed::class.java) { newTNT ->
                            tnt.getOwner()?.let { newTNT.setOwner(it) }
                            newTNT.source = tnt.source
                            newTNT.fuseTicks = (20..40).random()

                            newTNT.velocity = Vector(sin(Math.toRadians(i * 36.0)), 0.5, cos(Math.toRadians(i * 36.0)))
                        }
                    }
                }

                1 -> {
                    val directions3D = listOf(
                        Vector(-1, 0, 0), Vector(1, 0, 0),
                        Vector(0, -1, 0), Vector(0, 1, 0),
                        Vector(0, 0, -1), Vector(0, 0, 1)
                    )

                    for (block in blocks.filter { (1..10).random() == 1 }) {
                        if (directions3D.none { v -> block.location.add(v).block in blocks }) continue

                        blocks.remove(block)
                        block.type = Material.TNT
                    }
                }
            }
        }
    }

    //TODO add tntBlock breaking
}