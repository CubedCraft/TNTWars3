package com.jeroenvdg.tntwars.misc

import org.bukkit.Bukkit
import org.bukkit.plugin.Plugin

class TaskChain(private val plugin: Plugin) {
    fun <T> asyncTask(task: () -> T): TaskChainStep<T> {
        return TaskChainStep(plugin, { complete ->
            Bukkit.getScheduler().runTaskAsynchronously(plugin, Runnable {
                complete(runCatching(task))
            })
        })
    }

    fun <T> syncTask(task: () -> T): TaskChainStep<T> {
        return TaskChainStep(plugin, { complete ->
            Bukkit.getScheduler().runTask(plugin, Runnable {
                complete(runCatching(task))
            })
        })
    }
}

class TaskChainStep<T> internal constructor(
    private val plugin: Plugin,
    private val submit: ((Result<T>) -> Unit) -> Unit,
    private val errorHandler: TaskChainErrorHandler = TaskChainErrorHandler(),
) {
    fun <R> asyncTask(task: (T) -> R): TaskChainStep<R> {
        return TaskChainStep(plugin, { complete ->
            submit { result ->
                val value = result.getOrElse {
                    complete(Result.failure(it))
                    return@submit
                }

                Bukkit.getScheduler().runTaskAsynchronously(plugin, Runnable {
                    complete(runCatching { task(value) })
                })
            }
        }, errorHandler)
    }

    fun <R> syncTask(task: (T) -> R): TaskChainStep<R> {
        return TaskChainStep(plugin, { complete ->
            submit { result ->
                val value = result.getOrElse {
                    complete(Result.failure(it))
                    return@submit
                }

                Bukkit.getScheduler().runTask(plugin, Runnable {
                    complete(runCatching { task(value) })
                })
            }
        }, errorHandler)
    }

    fun onError(handler: (Throwable) -> Unit): TaskChainStep<T> {
        errorHandler.handler = handler
        return this
    }

    fun start(onSuccess: (T) -> Unit = {}) {
        submit { result ->
            Bukkit.getScheduler().runTask(plugin, Runnable {
                result.fold(onSuccess, errorHandler.handler ?: { throw it })
            })
        }
    }
}

internal class TaskChainErrorHandler {
    var handler: ((Throwable) -> Unit)? = null
}
