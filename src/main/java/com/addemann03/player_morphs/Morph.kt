package com.addemann03.player_morphs

import com.addemann03.player_morphs.entities.MorphInfo
import com.addemann03.player_morphs.entities.MorphProperty
import com.jeroenvdg.tntwars.TNTWars
import com.jeroenvdg.tntwars.player.TNTWarsPlayer
import me.libraryaddict.disguise.disguisetypes.DisguiseType
import me.libraryaddict.disguise.disguisetypes.MobDisguise
import me.libraryaddict.disguise.disguisetypes.watchers.LivingWatcher
import net.kyori.adventure.key.Key
import net.kyori.adventure.key.Keyed
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player
import kotlin.reflect.KClass

abstract class Morph<F : LivingWatcher>(entityType: EntityType) : Keyed {

    abstract class MorphType<M: Morph<*>>(val morphClass: KClass<out M>) {
        abstract val info: MorphInfo
        open val enabled = true
    }
    var player: Player? = null
        private set

    val abilities: MutableList<Ability<Morph<*>>> = mutableListOf()

    override fun key(): Key = Key.key(TNTWars.instance, disguiseType.name)

    private val disguiseType: DisguiseType = DisguiseType.getType(entityType)
        ?: throw IllegalArgumentException("EntityType $entityType stöds inte som disguise.")

    private val disguise: MobDisguise = MobDisguise(disguiseType)

    @Suppress("UNCHECKED_CAST")
    protected val flagWatcher: F
        get() = disguise.watcher as? F
            ?: throw IllegalStateException("Watcher does not match the expected type.")

    val isMorphed: Boolean
        get() = disguise.isDisguiseInUse && player != null

    protected abstract fun modifier(watcher: F)
    val properties = listOf<MorphProperty<Any>>()

    val hasAbilities: Boolean
        get() = abilities.isNotEmpty()

    fun registerAbility(ability: Ability<Morph<*>>) {
        ability.attach(this)
        abilities.add(ability)
    }

    fun unregisterAbility(ability: Ability<Morph<*>>) {
        val ability = abilities.find { it == ability }
        if(ability != null) abilities.remove(ability)
        ability?.detach()
    }

    init {
        // Kör modifieraren när objektet skapas
        applyProperties(disguise)
        modifier(flagWatcher)
    }

    /**
     * Aktiverar disguisen för den angivna spelaren.
     */
    fun morph(player: Player) {
        // Om spelaren redan har denna morph aktiv, gör inget
        if (isMorphed) return

        disguise.entity = player

        flagWatcher.isCustomNameVisible = true
        flagWatcher.customName = player.name
        disguise.startDisguise()

        if (disguise.isDisguiseInUse) {
            this.player = player
            TNTWars.instance.logger.info("Spelaren ${player.name} förvandlades till $disguiseType.")
        } else {
            TNTWars.instance.logger.warning("Misslyckades att förvandla ${player.name} till $disguiseType.")
        }
    }

    /**
     * Avslutar disguisen.
     */
    fun unmorph() {
        check(isMorphed) { "Kan inte avbryta morph: spelaren är inte förvandlad." }

        val playerName = player?.name ?: "Okänd spelare"
        disguise.stopDisguise()
        this.player = null

        TNTWars.instance.logger.info("Morph avslutades för $playerName.")
    }

    protected open fun applyProperties(disguise: MobDisguise) {
        disguise.isPlayIdleSounds = true
        disguise.isSelfDisguiseVisible = false
        disguise.isHearSelfDisguise = true
        disguise.isScalePlayerToDisguise = true
        disguise.isKeepDisguiseOnPlayerDeath = true
    }

    fun activateAbilities() {
        abilities.forEach {
            it.activated = true
            it.onActivate(player!!)
        }
    }

    fun deactivateAbilities() {
        abilities.forEach {
            it.activated = false
            it.onDeactivate(player!!)
        }
    }
}