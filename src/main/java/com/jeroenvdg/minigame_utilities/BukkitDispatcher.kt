package com.jeroenvdg.minigame_utilities

import com.jeroenvdg.tntwars.TNTWars
import kotlinx.coroutines.CoroutineDispatcher
import org.bukkit.Bukkit
import kotlin.coroutines.CoroutineContext

object BukkitDispatcher : CoroutineDispatcher() {
    override fun dispatch(context: CoroutineContext, block: Runnable) {
        Bukkit.getScheduler().runTask(TNTWars.instance, block)
    }
}