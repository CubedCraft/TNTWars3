package com.jeroenvdg.tntwars.misc

import com.jeroenvdg.tntwars.TNTWars
import com.jeroenvdg.tntwars.player.TNTWarsPlayer
import com.jeroenvdg.tntwars.player.PlayerManager
import com.jeroenvdg.tntwars.player.behaviours.PlayerDamageBehaviour
import com.jeroenvdg.minigame_utilities.Textial
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TextComponent
import org.bukkit.Bukkit
import java.util.*

class PlayerDeathHelper {

    companion object {
        val instance get() = TNTWars.instance.playerDeathHelper
    }

    fun getDeathContext(behaviour: PlayerDamageBehaviour): PlayerDeathContext {
        val user = behaviour.user
        val player = behaviour.player

        val lastDamagedIsVoid = behaviour.lastDamageIsVoid.value == true
        val lastDamagedIsFall = behaviour.lastDamageIsFall.value == true
        val lastDamageIsSelf = behaviour.lastDamagedSelf.value == true

        val lastDamagedPlayer = if (behaviour.lastDamagedPlayer.value == null) null else Bukkit.getPlayer(UUID.fromString(behaviour.lastDamagedPlayer.value))
        val lastDamagedUser = if (lastDamagedPlayer != null) PlayerManager.instance.get(lastDamagedPlayer) else null

        val lastDamageByEnemyWasResent = behaviour.lastDamagedPlayerRecent.value == true
        val lastDamagedIsByEnemy = lastDamagedUser != null

        val myName = Textial.msg.parse("&${user.team.primaryColor.char}${player.name}")
        val enemyName: TextComponent = if (lastDamagedUser != null) Textial.msg.parse("&${lastDamagedUser.team.primaryColor.char}${lastDamagedPlayer!!.name}") else Component.text("")

        when {
            lastDamagedIsVoid && lastDamagedIsByEnemy && lastDamageByEnemyWasResent -> {
                val msg = Component.text()
                    .append(myName)
                    .append(Component.text(" got knocked into the void by ").color(Textial.Gray.color))
                    .append(enemyName)
                    .build()
                return PlayerDeathContext(DeathReason.Void, user, msg, lastDamagedUser)
            }
            lastDamagedIsVoid && lastDamagedIsByEnemy -> {
                val msg = Component.text()
                    .append(myName)
                    .append(Component.text(" got knocked into the void whilst trying to escape ").color(Textial.Gray.color))
                    .append(enemyName)
                    .append(Component.text("'s tnt").color(Textial.Gray.color))
                    .build()
                return PlayerDeathContext(DeathReason.Void, user, msg, lastDamagedUser)
            }
            lastDamagedIsVoid && lastDamageIsSelf -> {
                val msg = Component.text()
                    .append(myName)
                    .append(Component.text(" knocked their self into the void").color(Textial.Gray.color))
                    .build()
                return PlayerDeathContext(DeathReason.Void, user, msg, null)
            }
            lastDamagedIsVoid -> {
                val msg = Component.text()
                    .append(myName)
                    .append(Component.text(" fell into the void").color(Textial.Gray.color))
                    .build()
                return PlayerDeathContext(DeathReason.Void, user, msg, null)
            }
            lastDamagedIsFall && lastDamagedIsByEnemy && lastDamageByEnemyWasResent -> {
                val msg = Component.text()
                    .append(myName)
                    .append(Component.text(" hit the ground hard thanks to ").color(Textial.Gray.color))
                    .append(enemyName)
                    .append(Component.text("'s tnt").color(Textial.Gray.color))
                    .build()
                return PlayerDeathContext(DeathReason.Fall, user, msg, lastDamagedUser)
            }
            lastDamagedIsFall && lastDamagedIsByEnemy -> {
                val msg = Component.text()
                    .append(myName)
                    .append(Component.text(" hit the ground hard whilst trying to escape ").color(Textial.Gray.color))
                    .append(enemyName)
                    .append(Component.text("'s tnt").color(Textial.Gray.color))
                    .build()
                return PlayerDeathContext(DeathReason.Fall, user, msg, lastDamagedUser)
            }
            lastDamagedIsFall && lastDamageIsSelf -> {
                val msg = Component.text()
                    .append(myName)
                    .append(Component.text(" hit the ground hard thanks to their own cannon").color(Textial.Gray.color))
                    .build()
                return PlayerDeathContext(DeathReason.Fall, user, msg, null)
            }
            lastDamagedIsFall -> {
                val msg = Component.text()
                    .append(myName)
                    .append(Component.text(" fell to their death").color(Textial.Gray.color))
                    .build()
                return PlayerDeathContext(DeathReason.Fall, user, msg, null)
            }
            lastDamagedIsByEnemy && lastDamageByEnemyWasResent -> {
                val msg = Component.text()
                    .append(myName)
                    .append(Component.text(" got killed by ").color(Textial.Gray.color))
                    .append(enemyName)
                    .build()
                return PlayerDeathContext(DeathReason.Killed, user, msg, lastDamagedUser)
            }
            lastDamagedIsByEnemy -> {
                val msg = Component.text()
                    .append(myName)
                    .append(Component.text(" died whilst trying to escape ").color(Textial.Gray.color))
                    .append(enemyName)
                    .append(Component.text("'s tnt").color(Textial.Gray.color))
                    .build()
                return PlayerDeathContext(DeathReason.Killed, user, msg, lastDamagedUser)
            }
            lastDamageIsSelf -> {
                val msg = Component.text()
                    .append(myName)
                    .append(Component.text(" got killed by their own cannon").color(Textial.Gray.color))
                    .build()
                return PlayerDeathContext(DeathReason.SelfCannon, user, msg, null)
            }
            else -> {
                val msg = Component.text()
                    .append(myName)
                    .append(Component.text(" died").color(Textial.Gray.color))
                    .build()
                return PlayerDeathContext(DeathReason.Unknown, user, msg, null)
            }
        }
    }
}

class PlayerDeathContext(val reason: DeathReason, val user: TNTWarsPlayer, val message: TextComponent, private val enemy: TNTWarsPlayer?) {
    val hasDamager: Boolean = enemy != null
    val damager get() = enemy!!
}

enum class DeathReason {
    Killed,
    Void,
    Fall,
    SelfCannon,
    Unknown
}