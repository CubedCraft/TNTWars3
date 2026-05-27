package xyz.pondwader.replay_engine

import com.destroystokyo.paper.event.entity.EntityRemoveFromWorldEvent
import com.destroystokyo.paper.event.player.PlayerArmorChangeEvent
import io.papermc.paper.event.entity.EntityMoveEvent
import io.papermc.paper.event.player.PlayerArmSwingEvent
import org.bukkit.Material
import org.bukkit.GameMode
import org.bukkit.Location
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockBurnEvent
import org.bukkit.event.block.BlockDamageAbortEvent
import org.bukkit.event.block.BlockDamageEvent
import org.bukkit.event.block.BlockExplodeEvent
import org.bukkit.event.block.BlockFadeEvent
import org.bukkit.event.block.BlockFormEvent
import org.bukkit.event.block.BlockGrowEvent
import org.bukkit.event.block.BlockMultiPlaceEvent
import org.bukkit.event.block.BlockPistonExtendEvent
import org.bukkit.event.block.BlockPistonRetractEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.block.BlockSpreadEvent
import org.bukkit.event.block.FluidLevelChangeEvent
import org.bukkit.event.block.Action
import org.bukkit.event.entity.EntityChangeBlockEvent
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.EntityExplodeEvent
import org.bukkit.event.entity.EntitySpawnEvent
import org.bukkit.event.entity.EntityTeleportEvent
import org.bukkit.event.entity.EntityToggleGlideEvent
import org.bukkit.event.entity.EntityToggleSwimEvent
import org.bukkit.event.entity.ItemDespawnEvent
import org.bukkit.event.entity.EntityPickupItemEvent
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryDragEvent
import org.bukkit.event.inventory.InventoryAction
import org.bukkit.event.player.PlayerDropItemEvent
import org.bukkit.event.player.PlayerGameModeChangeEvent
import org.bukkit.event.player.PlayerItemHeldEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.event.player.PlayerSwapHandItemsEvent
import org.bukkit.event.player.PlayerTeleportEvent
import org.bukkit.event.player.PlayerToggleSneakEvent
import org.bukkit.event.player.PlayerToggleSprintEvent
import org.bukkit.event.player.PlayerVelocityEvent
import org.bukkit.inventory.ItemStack

class CaptureListener(private val capture: GameCapture) : Listener {
    private var explosionDedupeTick = -1L
    private val explosionDedupeKeys = HashSet<ExplosionKey>()

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onBlockPlace(event: BlockPlaceEvent) {
        if (event is BlockMultiPlaceEvent) return
        if (!isInCaptureWorld(event.block)) return

        emit(
            CaptureBlockChangeEvent(
                position = event.block.toCaptureBlockPosition(),
                newBlockData = event.block.blockData.asString,
            )
        )
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onBlockMultiPlace(event: BlockMultiPlaceEvent) {
        if (!isInCaptureWorld(event.block)) return

        for (state in event.replacedBlockStates) {
            val block = state.block
            emit(
                CaptureBlockChangeEvent(
                    position = block.toCaptureBlockPosition(),
                    newBlockData = block.blockData.asString,
                )
            )
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onBlockBreak(event: BlockBreakEvent) {
        if (!isInCaptureWorld(event.block)) return

        emit(
            CaptureBlockChangeEvent(
                position = event.block.toCaptureBlockPosition(),
                newBlockData = Material.AIR.createBlockData().asString,
            )
        )
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onBlockDamage(event: BlockDamageEvent) {
        if (!isInCaptureWorld(event.block)) return
        emit(
            CaptureBlockDamageEvent(
                position = event.block.toCaptureBlockPosition(),
                progress = 0.1f,
                playerUuid = event.player.uniqueId.toString(),
            )
        )
    }

    @EventHandler(priority = EventPriority.MONITOR)
    fun onBlockDamageAbort(event: BlockDamageAbortEvent) {
        if (!isInCaptureWorld(event.block)) return
        emit(
            CaptureBlockDamageEvent(
                position = event.block.toCaptureBlockPosition(),
                progress = 0.0f,
                playerUuid = event.player.uniqueId.toString(),
            )
        )
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onBlockGrow(event: BlockGrowEvent) {
        emitBlockStateChange(event.block, event.newState.blockData.asString)
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onBlockForm(event: BlockFormEvent) {
        emitBlockStateChange(event.block, event.newState.blockData.asString)
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onBlockSpread(event: BlockSpreadEvent) {
        emitBlockStateChange(event.block, event.newState.blockData.asString)
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onBlockFade(event: BlockFadeEvent) {
        emitBlockStateChange(event.block, event.newState.blockData.asString)
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onBlockBurn(event: BlockBurnEvent) {
        emitBlockStateChange(event.block, Material.AIR.createBlockData().asString)
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onFluidLevelChange(event: FluidLevelChangeEvent) {
        emitBlockStateChange(event.block, event.newData.asString)
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onEntityChangeBlock(event: EntityChangeBlockEvent) {
        if (!isInCaptureWorld(event.block)) return
        emit(
            CaptureBlockChangeEvent(
                position = event.block.toCaptureBlockPosition(),
                newBlockData = event.blockData.asString,
            )
        )
    }

    @EventHandler(priority = EventPriority.MONITOR)
    fun onBlockExplode(event: BlockExplodeEvent) {
        if (!isInCaptureWorld(event.block)) return
        emitExplosion(event.block.location)
        if (event.isCancelled) return
        emitBlocksChangedToAir(event.blockList(), "block_explode", null)
    }

    @EventHandler(priority = EventPriority.MONITOR)
    fun onEntityExplode(event: EntityExplodeEvent) {
        if (event.location.world != capture.world) return
        emitExplosion(event.location)
        if (event.isCancelled) return
        emitBlocksChangedToAir(event.blockList(), "entity_explode", event.entity.entityId)
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onPistonExtend(event: BlockPistonExtendEvent) {
        if (!isInCaptureWorld(event.block)) return
        for (block in event.blocks) {
            val target = block.getRelative(event.direction)
            emit(
                CaptureBlockChangeEvent(
                    position = target.toCaptureBlockPosition(),
                    newBlockData = block.blockData.asString,
                )
            )
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onPistonRetract(event: BlockPistonRetractEvent) {
        if (!isInCaptureWorld(event.block)) return
        for (block in event.blocks) {
            val target = block.getRelative(event.direction)
            emit(
                CaptureBlockChangeEvent(
                    position = target.toCaptureBlockPosition(),
                    newBlockData = block.blockData.asString,
                )
            )
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onEntitySpawn(event: EntitySpawnEvent) {
        if (!isInCaptureWorld(event.entity)) return
        if (event.entity is Player && (event.entity as Player).gameMode == GameMode.SPECTATOR) return
        capture.trackEntity(event.entity)
        emit(event.entity.toCaptureSpawnEvent())
    }

    @EventHandler(priority = EventPriority.MONITOR)
    fun onEntityRemove(event: EntityRemoveFromWorldEvent) {
        if (event.world != capture.world) return
        capture.untrackEntity(event.entity.entityId)
        emit(event.entity.toCaptureRemoveEvent())
    }

    @EventHandler(priority = EventPriority.MONITOR)
    fun onItemDespawn(event: ItemDespawnEvent) {
        if (!isInCaptureWorld(event.entity)) return
        emit(event.entity.toCaptureRemoveEvent())
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onEntityMove(event: EntityMoveEvent) {
        if (event.entity is Player) return
        if (!event.hasChangedPosition() && !event.hasChangedOrientation()) return
        if (event.to.world != capture.world) return

        emit(
            CaptureEntityMoveEvent(
                entityId = event.entity.entityId,
                to = event.to.toCaptureLocation(),
            )
        )
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onPlayerMove(event: PlayerMoveEvent) {
        if (event is PlayerTeleportEvent) return
        if (!isInCaptureWorld(event.player)) return
        if (event.player.gameMode == GameMode.SPECTATOR) return
        val to = event.to
        if (!hasLocationChanged(event.from, to)) return

        emit(
            CaptureEntityMoveEvent(
                entityId = event.player.entityId,
                to = to.toCaptureLocation(),
            )
        )
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onEntityTeleport(event: EntityTeleportEvent) {
        if (event.entity is Player) return
        val to = event.to ?: return
        if (to.world != capture.world) return

        emit(
            CaptureEntityMoveEvent(
                entityId = event.entity.entityId,
                to = to.toCaptureLocation(),
            )
        )
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onPlayerTeleport(event: PlayerTeleportEvent) {
        if (event.player.gameMode == GameMode.SPECTATOR) return
        val to = event.to
        if (to.world != capture.world) {
            if (event.from.world == capture.world) {
                capture.untrackEntity(event.player.entityId)
                emit(event.player.toCaptureRemoveEvent())
            }
            return
        }

        emit(
            CaptureEntityMoveEvent(
                entityId = event.player.entityId,
                to = to.toCaptureLocation(),
            )
        )
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onPlayerVelocity(event: PlayerVelocityEvent) {
        if (!isInCaptureWorld(event.player)) return
        emit(
            CaptureEntityVelocityEvent(
                entityId = event.player.entityId,
                velocity = event.velocity.toCaptureVector(),
            )
        )
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onEntityDamage(event: EntityDamageEvent) {
        if (!isInCaptureWorld(event.entity)) return
        emit(
            CaptureEntityDamageEvent(
                entityId = event.entity.entityId,
                yaw = event.entity.location.yaw,
            )
        )
    }

    @EventHandler(priority = EventPriority.MONITOR)
    fun onPlayerJoin(event: PlayerJoinEvent) {
        if (!isInCaptureWorld(event.player)) return
        if (event.player.gameMode == GameMode.SPECTATOR) return
        capture.trackEntity(event.player)
        emit(event.player.toCaptureSpawnEvent())
    }

    @EventHandler(priority = EventPriority.MONITOR)
    fun onPlayerQuit(event: PlayerQuitEvent) {
        if (!isInCaptureWorld(event.player)) return
        capture.untrackEntity(event.player.entityId)
        emit(event.player.toCaptureRemoveEvent())
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onPlayerInteract(event: PlayerInteractEvent) {
        if (event.action != Action.RIGHT_CLICK_BLOCK) return
        val block = event.clickedBlock ?: return
        if (!isInCaptureWorld(block)) return

        val previousBlockData = block.blockData.asString
        capture.captureEventNextTick {
            if (!isInCaptureWorld(block)) return@captureEventNextTick null
            val newBlockData = block.blockData.asString
            if (previousBlockData == newBlockData) return@captureEventNextTick null

            CaptureBlockChangeEvent(
                position = block.toCaptureBlockPosition(),
                newBlockData = newBlockData,
            )
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onPlayerArmSwing(event: PlayerArmSwingEvent) {
        if (!isInCaptureWorld(event.player)) return
        emit(
            CapturePlayerAnimationEvent(
                entityId = event.player.entityId,
                hand = event.hand.name,
            )
        )
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onPlayerItemHeld(event: PlayerItemHeldEvent) {
        if (!isInCaptureWorld(event.player)) return
        emit(
            CapturePlayerHeldItemEvent(
                entityId = event.player.entityId,
                newItem = event.player.inventory.getItem(event.newSlot).toCaptureItemStack(),
            )
        )
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onPlayerToggleSneak(event: PlayerToggleSneakEvent) {
        if (!isInCaptureWorld(event.player)) return
        emit(
            CaptureEntityVisualStateEvent(
                entityId = event.player.entityId,
                visualState = event.player.toCaptureVisualState().copy(
                    sneaking = event.isSneaking
                )
            )
        )
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onPlayerToggleSprint(event: PlayerToggleSprintEvent) {
        if (!isInCaptureWorld(event.player)) return
        emit(
            CaptureEntityVisualStateEvent(
                entityId = event.player.entityId,
                visualState = event.player.toCaptureVisualState().copy(
                    sprinting = event.isSprinting
                )
            )
        )
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onEntityToggleSwim(event: EntityToggleSwimEvent) {
        if (!isInCaptureWorld(event.entity)) return
        emit(
            CaptureEntityVisualStateEvent(
                entityId = event.entity.entityId,
                visualState = event.entity.toCaptureVisualState().copy(
                    swimming = event.isSwimming
                )
            )
        )
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onEntityToggleGlide(event: EntityToggleGlideEvent) {
        if (!isInCaptureWorld(event.entity)) return
        emit(
            CaptureEntityVisualStateEvent(
                entityId = event.entity.entityId,
                visualState = event.entity.toCaptureVisualState().copy(
                    gliding = event.isGliding
                )
            )
        )
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onPlayerGameModeChange(event: PlayerGameModeChangeEvent) {
        if (!isInCaptureWorld(event.player)) return

        if (event.newGameMode == GameMode.SPECTATOR) {
            capture.untrackEntity(event.player.entityId)
            emit(event.player.toCaptureRemoveEvent())
            return
        }

        if (event.player.gameMode == GameMode.SPECTATOR) {
            // Ensure correct data is captured to spawn the player into the replay
            capture.captureEventNextTick {
                if (!isInCaptureWorld(event.player)) return@captureEventNextTick null
                capture.trackEntity(event.player)
                event.player.toCaptureSpawnEvent()
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onPlayerArmorChange(event: PlayerArmorChangeEvent) {
        if (!isInCaptureWorld(event.player)) return
        emitVisibleEquipmentNextTick(event.player)
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onPlayerSwapHandItems(event: PlayerSwapHandItemsEvent) {
        if (!isInCaptureWorld(event.player)) return
        emit(
            CapturePlayerHeldItemEvent(
                entityId = event.player.entityId,
                newItem = event.mainHandItem.toCaptureItemStack(),
            )
        )
        emit(
            CapturePlayerOffhandItemEvent(
                entityId = event.player.entityId,
                newItem = event.offHandItem.toCaptureItemStack(),
            )
        )
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onPlayerDropItem(event: PlayerDropItemEvent) {
        if (!isInCaptureWorld(event.player)) return
        capture.captureEventNextTick {
            if (!isInCaptureWorld(event.player)) return@captureEventNextTick null
            CapturePlayerHeldItemEvent(
                entityId = event.player.entityId,
                newItem = event.player.inventory.itemInMainHand.toCaptureItemStack(),
            )
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onEntityPickupItem(event: EntityPickupItemEvent) {
        val player = event.entity as? Player ?: return
        if (!isInCaptureWorld(player)) return
        emitVisibleEquipmentIfChangedNextTick(player)
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onInventoryClick(event: InventoryClickEvent) {
        val player = event.whoClicked as? Player ?: return
        if (!isInCaptureWorld(player)) return
        emitVisibleEquipmentIfChangedNextTick(player)
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onInventoryDrag(event: InventoryDragEvent) {
        val player = event.whoClicked as? Player ?: return
        if (!isInCaptureWorld(player)) return
        emitVisibleEquipmentIfChangedNextTick(player)
    }

    private fun emit(event: CaptureEventPayload) {
        capture.captureEvent(event)
    }

    private fun emitExplosion(location: Location) {
        val tick = capture.currentTick()
        if (tick != explosionDedupeTick) {
            explosionDedupeTick = tick
            explosionDedupeKeys.clear()
        }

        val key = ExplosionKey(location.x.toRawBits(), location.y.toRawBits(), location.z.toRawBits())
        if (!explosionDedupeKeys.add(key)) return
        capture.captureEvent(CaptureExplosionEvent(location.toCaptureLocation()))
    }

    private fun emitVisibleEquipmentNextTick(player: Player) {
        capture.captureEventNextTick {
            if (!isInCaptureWorld(player)) return@captureEventNextTick null
            CaptureEntityStateEvent(
                entityId = player.entityId,
                equipment = player.toCaptureEquipment(),
            )
        }
    }

    private fun emitVisibleEquipmentIfChangedNextTick(player: Player) {
        val previous = player.visibleEquipmentSnapshot()
        capture.captureEventNextTick {
            if (!isInCaptureWorld(player)) return@captureEventNextTick null
            if (previous == player.visibleEquipmentSnapshot()) return@captureEventNextTick null

            CaptureEntityStateEvent(
                entityId = player.entityId,
                equipment = player.toCaptureEquipment(),
            )
        }
    }

    private fun Player.visibleEquipmentSnapshot(): VisibleEquipmentSnapshot {
        val equipment = equipment
        return VisibleEquipmentSnapshot(
            mainHand = inventory.itemInMainHand.visibleCopy(),
            offHand = inventory.itemInOffHand.visibleCopy(),
            helmet = equipment.helmet.visibleCopy(),
            chestplate = equipment.chestplate.visibleCopy(),
            leggings = equipment.leggings.visibleCopy(),
            boots = equipment.boots.visibleCopy(),
        )
    }

    private fun ItemStack?.visibleCopy(): ItemStack? {
        if (this == null || isEmpty) return null
        return clone()
    }

    private fun emitBlockStateChange(block: org.bukkit.block.Block, newBlockData: String) {
        if (!isInCaptureWorld(block)) return
        emit(
            CaptureBlockChangeEvent(
                position = block.toCaptureBlockPosition(),
                newBlockData = newBlockData,
            )
        )
    }

    private fun emitBlocksChangedToAir(blocks: List<org.bukkit.block.Block>, cause: String, entityId: Int?) {
        val air = Material.AIR.createBlockData().asString
        for (block in blocks) {
            emit(
                CaptureBlockChangeEvent(
                    position = block.toCaptureBlockPosition(),
                    newBlockData = air,
                )
            )
        }
    }

    private fun isInCaptureWorld(block: org.bukkit.block.Block): Boolean {
        return block.world == capture.world
    }

    private fun isInCaptureWorld(entity: Entity): Boolean {
        return entity.world == capture.world
    }

    private fun hasLocationChanged(from: org.bukkit.Location, to: org.bukkit.Location): Boolean {
        return from.x != to.x || from.y != to.y || from.z != to.z || from.yaw != to.yaw || from.pitch != to.pitch
    }

    private data class ExplosionKey(
        val x: Long,
        val y: Long,
        val z: Long,
    )

    private data class VisibleEquipmentSnapshot(
        val mainHand: ItemStack?,
        val offHand: ItemStack?,
        val helmet: ItemStack?,
        val chestplate: ItemStack?,
        val leggings: ItemStack?,
        val boots: ItemStack?,
    )

    private companion object {
        val ARMOR_SLOTS = 36..39
        const val OFFHAND_SLOT = 40
    }
}
