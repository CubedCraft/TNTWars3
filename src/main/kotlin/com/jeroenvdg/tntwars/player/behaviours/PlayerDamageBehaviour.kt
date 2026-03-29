package com.jeroenvdg.tntwars.player.behaviours

import com.jeroenvdg.tntwars.game.GameManager
import com.jeroenvdg.tntwars.listeners.BlockOwnershipManager.Companion.getOwnerAsPlayer
import com.jeroenvdg.tntwars.listeners.BlockOwnershipManager.Companion.getTeam
import com.jeroenvdg.tntwars.misc.PlayerDeathHelper
import com.jeroenvdg.tntwars.player.PlayerBehaviour
import com.jeroenvdg.tntwars.player.TNTWarsPlayer
import com.jeroenvdg.minigame_utilities.Scheduler
import org.bukkit.Bukkit
import org.bukkit.damage.DamageType
import org.bukkit.entity.Player
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityDamageEvent

class PlayerDamageBehaviour(user: TNTWarsPlayer) : PlayerBehaviour(user) {

    var canTakeDamage = false
    var canDieInVoid = true

    val lastDamageIsFall = DamageValue<Boolean>(5)
    val lastDamageIsVoid = DamageValue<Boolean>(5)
    val lastDamagedPlayer = DamageValue<String>(30 * 20)
    val lastDamagedPlayerRecent = DamageValue<Boolean>(5 * 20)
    val lastDamagedSelf = DamageValue<Boolean>(5 * 20)
    var checkForDeathRoutine: Int = -1

    init {
        setToDefaults()
    }

    fun setToDefaults() {
        canTakeDamage = false
        canDieInVoid = true
        lastDamageIsFall.reset()
        lastDamageIsVoid.reset()
        lastDamagedPlayer.reset()
        lastDamagedPlayerRecent.reset()
        lastDamagedSelf.reset()
    }

    override fun onActivate() {
        user.onDamaged += ::handleDamaged
        checkForDeathRoutine = Scheduler.delayRepeating(0L, 5L, ::checkForVoidDeathRoutine)
    }

    override fun onDeactivate() {
        Scheduler.stop(checkForDeathRoutine)
        checkForDeathRoutine = -1
        user.onDamaged -= ::handleDamaged
    }

    private fun checkForVoidDeathRoutine() {
        if (!canDieInVoid) return
        val map = GameManager.instance.activeMap
        if (map.voidHeight > player.location.y) {
            lastDamageIsVoid.set(true)
            killPlayer()
        }
    }

    private fun handleDamaged(e: EntityDamageEvent) {
        if (!canTakeDamage) {
            e.isCancelled = true
            return
        }

        if (e is EntityDamageByEntityEvent && e.damager is Player) {
            e.isCancelled = true
            return
        }

        setLastDamagedPlayer(e)

        if (player.health - e.finalDamage <= 0) {
            e.isCancelled = true
            killPlayer()
            return
        }
    }

    private fun setLastDamagedPlayer(event: EntityDamageEvent) {
        lastDamageIsFall.reset()
        if (event is EntityDamageByEntityEvent) {
            val owner = event.damager.getOwnerAsPlayer()
            val team = event.damager.getTeam()
            if (owner != null) {
                if (owner == player) {
                    lastDamagedSelf.set(true)
                } else if (team != user.team) {
                    lastDamagedPlayerRecent.set(true)
                    lastDamagedPlayer.set(owner.uniqueId.toString())
                }
            }
        }
        if (event.damageSource.damageType == DamageType.FALL) {
            lastDamageIsFall.set(true)
        }
    }

    private fun killPlayer() {
        user.heal()
        user.onPlayerDeath.invoke(PlayerDeathHelper.instance.getDeathContext(this))
    }

    class DamageValue<T>(timeoutTicks: Int) {
        var created = 0; private set
        var value: T? = null
            get() {
                if (!isValid()) field = null
                return field
            }
            private set

        private var timeout = timeoutTicks

        fun reset() {
            created = 0
            value = null
        }

        fun set(value: T) {
            this.value = value
            this.created = Bukkit.getCurrentTick()
        }

        fun isValid() = (created + timeout) > Bukkit.getCurrentTick()
    }

}