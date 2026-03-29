package com.jeroenvdg.minigame_utilities

import io.papermc.paper.threadedregions.scheduler.AsyncScheduler
import io.papermc.paper.threadedregions.scheduler.ScheduledTask
import kotlinx.coroutines.*
import org.bukkit.Bukkit
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scheduler.BukkitScheduler
import java.lang.Runnable
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * A higher level API for the Bukkit Scheduler
 */
class Scheduler {
    companion object {
        private lateinit var scheduler: BukkitScheduler
        private lateinit var asyncScheduler: AsyncScheduler
        private lateinit var plugin: JavaPlugin

        fun sync(action: () -> Unit) {
            if (Bukkit.isPrimaryThread()) action()
            else scheduler.runTask(plugin, SchedulerRunnable(action))
        }

        fun delay(ticks: Long, action: () -> Unit) = scheduler.scheduleSyncDelayedTask(plugin, SchedulerRunnable(action), ticks)
        fun delayRepeating(delay: Long, interval: Long, action: () -> Unit) = scheduler.scheduleSyncRepeatingTask(plugin, SchedulerRunnable(action), delay, interval)
        fun delayTick(action: () -> Unit) = scheduler.scheduleSyncDelayedTask(plugin, SchedulerRunnable(action), 1L)
        fun stop(id: Int) = scheduler.cancelTask(id)
        fun stopAll() = scheduler.cancelTasks(plugin)

        fun async(action: () -> Unit) = asyncScheduler.runNow(plugin) { action() }
        fun delayAsync(ticks: Long, action: () -> Unit) = asyncScheduler.runDelayed(plugin, { action() }, ticks * 50, TimeUnit.MILLISECONDS )
        fun delayRepeatingAsync(delay: Long, interval: Long, action: () -> Unit) = asyncScheduler.runAtFixedRate(plugin, { action() }, delay * 50, interval * 50, TimeUnit.MILLISECONDS)
        fun stopAsync(scheduledTask: ScheduledTask) = scheduledTask.cancel()
        fun stopAllAsync() = asyncScheduler.cancelTasks(plugin)

        suspend fun sync() = suspendCoroutine {
            sync {
                if (it.context.isActive) {
                    it.resume(Unit)
                }
            }
        }

        suspend fun delay(ticks: Long) = suspendCoroutine {
            delay(ticks) {
                if (it.context.isActive) {
                    it.resume(Unit)
                }
            }
        }

        suspend fun async() = suspendCoroutine {
            async {
                if (it.context.isActive) {
                    it.resume(Unit)
                }
            }
        }

        suspend fun delayAsync(ticks: Long) = suspendCoroutine {
            delayAsync(ticks) {
                if (it.context.isActive) {
                    it.resume(Unit)
                }
            }
        }


        fun setup(plugin: JavaPlugin) {
            scheduler = Bukkit.getScheduler()
            asyncScheduler = Bukkit.getAsyncScheduler()
            this.plugin = plugin
        }
    }
}

class SchedulerRunnable(val action: () -> Unit) : Runnable {
    override fun run() {
        action.invoke()
    }
}
