package com.lagostout.flow

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

class SharedFlowTests : StringSpec({

    """Given a SharedFlow that is created with shareIn and a custom scope with a custom Job and sharing is started Lazily When the SharedFlow is collected And the Job of the scope argument to sharedIn is cancelled Then the code collecting the SharedFlow will remain suspended And the SharedFlow will stop emitting""" {
        val f1 = flowOf(1,2,3,4).onEach { delay(100) }
        val job = Job()
        val scope = CoroutineScope(job)
        val f2 = f1.shareIn(scope, SharingStarted.Lazily)
        launch {
            delay(250)
            job.cancel()
            println("cancelled")
        }
        f2.collect {
            println(it)
        }
    }

    """Given a SharedFlow that is created with shareIn and a custom scope with a custom job and sharing is started WhileSubscribed And a coroutine that cancels the job after a delay When the SharedFlow is collected And the custom job is cancelled Then collection of the SharedFlow remains suspended And the upstream Flow of SharedFlow will stop emitting""" {
        val f1 = flowOf(1,2,3,4).onEach { delay(100) }
        val job = Job()
        val scope = CoroutineScope(job)
        val f2 = f1.shareIn(scope, SharingStarted.WhileSubscribed())
        launch {
            delay(250)
            job.cancel()
            println("cancelled")
        }
        f2.collect {
            println(it)
        }
    }

    """Given a SharedFlow that is created using shareIn and starts sharing WhileSubscribed And a coroutine that collects the SharedFlow When the coroutine's job is cancelled Then the collection of the SharedFlow completes And the collecting coroutine is not suspended And only part of the upstream Flow of the SharedFlow has been emitted""" {
        val f1 = flowOf(1, 2, 3).onEach { delay(100) }
        val f2 = f1.shareIn(
            CoroutineScope(Job()),
            SharingStarted.WhileSubscribed()
        )
        var completed = false
        val items = mutableListOf<Int>()
        val j = launch {
            f2.onCompletion {
                completed = true
            }.collect {
                items.add(it)
            }
        }
        delay(250)
        j.cancelAndJoin()
        completed shouldBe true
        items shouldBe listOf(1, 2)
    }
})