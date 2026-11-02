package com.jeroenvdg.tntwars.player.states.playerGameStates

import com.jeroenvdg.minigame_utilities.makeItem
import com.jeroenvdg.minigame_utilities.toColor
import com.jeroenvdg.tntwars.EventBus
import com.jeroenvdg.tntwars.TNTWars
import com.jeroenvdg.tntwars.game.GameManager
import com.jeroenvdg.tntwars.game.Team
import com.jeroenvdg.tntwars.listeners.GenericItemListener
import com.jeroenvdg.tntwars.misc.PlayerDeathContext
import com.jeroenvdg.tntwars.player.TNTWarsPlayer
import com.jeroenvdg.tntwars.player.gameContexts.IPlayerGameContext
import com.jeroenvdg.tntwars.player.states.BasePlayerState
import net.kyori.adventure.audience.Audience
import net.kyori.adventure.title.Title
import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.LeatherArmorMeta
import org.bukkit.persistence.PersistentDataType

class PlayerGameStateMachine(user: TNTWarsPlayer) : BasePlayerState(user) {

    private val currentState get() = activeState as BasePlayerGameState
    lateinit var playerContext: IPlayerGameContext; private set

    init {
        addState(PlayerGamePlayingState(user))
        addState(PlayerGameRespawningState(user))
        addState(PlayerGameSpectateState(user))
    }

    fun applyArmor() {
        player.inventory.chestplate = createArmorItem(Material.LEATHER_CHESTPLATE)
        player.inventory.leggings = createArmorItem(Material.LEATHER_LEGGINGS)
        player.inventory.boots = createArmorItem(Material.LEATHER_BOOTS)
    }

    override fun onActivate() {
        super.onActivate()
        playerContext = GameManager.instance.playerGameContext.getPlayerGameContext(user, this)

        applyArmor()
        user.onTeamChanged += ::handleTeamChanged
        EventBus.onPlayerGameContextProviderChanged += ::handlePlayerGameContextChanged

        val config = TNTWars.instance.config
        val title = Title.title(
            config.message.matchStartTitle,
            config.message.matchStartSubtitle
        )

        Audience.audience(player).showTitle(title)
        player.sendMessage(config.message.matchStartMessage)

        playerContext.onActivate()
        user.resetInventory()
    }

    override fun onDeactivate() {
        super.onDeactivate()
        user.onTeamChanged -= ::handleTeamChanged
        EventBus.onPlayerGameContextProviderChanged -= ::handlePlayerGameContextChanged
        gotoNoState(true)
        playerContext.onDeactivate()
    }

    override fun onDeath(deathContext: PlayerDeathContext) {
        currentState.onDeath(deathContext)
    }

    override fun onInventoryReset() {
        playerContext.onInventoryReset()

        applyArmor()
    }

    private fun createArmorItem(material: Material) : ItemStack {
        return makeItem(material) {
            withPersistentData(GenericItemListener.movableKey, PersistentDataType.BOOLEAN, false)
            withPersistentData(GenericItemListener.droppableKey, PersistentDataType.BOOLEAN, false)

            meta.isUnbreakable = true

            val meta = this.meta as LeatherArmorMeta
            meta.setColor(user.team.primaryColor.color.toColor())
        }
    }

    private fun handleTeamChanged(old: Team, new: Team) {
        if (new.isSpectatorTeam) return
        gotoState(PlayerGamePlayingState::class.java)
    }

    private fun handlePlayerGameContextChanged(provider: IPlayerGameContext.IProvider) {
        playerContext.onDeactivate()
        playerContext = provider.getPlayerGameContext(user, this)
        playerContext.onActivate()
        user.resetInventory()
    }
}