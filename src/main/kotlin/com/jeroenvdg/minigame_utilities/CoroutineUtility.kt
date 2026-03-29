package com.jeroenvdg.minigame_utilities

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

fun launchCoroutine(action: suspend (scope: CoroutineScope) -> Unit) : Job {
    return CoroutineScope(Dispatchers.Unconfined).launch {
        action(this)
    }
}

suspend fun <T> runAsync(action: suspend () -> Result<T>): Result<T> {
    Scheduler.async()
    try {
        val result = action()
        Scheduler.sync()
        return result
    } catch (exception: Exception) {
        Scheduler.sync()
        return Result.failure(exception)
    }
}

suspend fun Job.await() {
    join()
}

class JobResult<T>(val action: suspend (scope: CoroutineScope) -> Result<T>) {

    private var result: Result<T>? = null
    private val job: Job = launchCoroutine { result = action(it) }

    fun isCompleted() = result != null
    fun getResultOrThrowIfNotCompleted() = result!!

    suspend fun await(): Result<T> {
        job.join()
        return result!!
    }
}