//@file:OptIn(ExperimentalCoroutinesApi::class)

@file:Suppress("EXPERIMENTAL_API_USAGE")
@file:OptIn(ExperimentalTime::class)

package com.lagostout.flow

import app.cash.turbine.test
import com.lagostout.flow.dropAll
import com.lagostout.flow.emitOnCompletionJust
import com.lagostout.flow.takeUntilUsingChannelFlowAndTerminatingOnOtherFlowFirstEmissionOrCompletion
import com.lagostout.flow.takeUntilUsingFlatMapLatest
import io.kotest.core.spec.style.StringSpec
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import org.junit.jupiter.api.Assertions.assertEquals
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.ExperimentalTime

class MiscFlowOperatorTests : StringSpec({

    """Flow.takeUntil using channel and terminating on other flow first emission""" {
        val f1 = flowOf("A").onStart { delay(150) }
        val f2 = flowOf(1, 2, 3).onEach { delay(100) }
        val f3 = f2.takeUntilUsingChannelFlowAndTerminatingOnOtherFlowFirstEmissionOrCompletion(f1)
        f3.test {
            assertEquals(1, awaitItem())
            awaitComplete()
        }
    }

    """Flow.takeUntil using a *latest operator""" {
        val f1 = flowOf("A").onStart { delay(199) }
        val f2 = flowOf(1, 2, 3).onEach { delay(100) }
        val f3 = f2.takeUntilUsingFlatMapLatest(f1)
        f3.test {
            assertEquals(1, awaitItem())
            awaitComplete()
        }
    }

    """Flow.emitOnCompletion""" {
        val f1 = flowOf(1, 2, 3).onEach { delay(100) }
        val f2 = f1.emitOnCompletionJust(4)
        f2.test {
            assertEquals(4, awaitItem())
            awaitComplete()
        }
    }

    """Flow.dropAll""" {
        val f1 = flowOf(1, 2, 3).onEach { delay(1_000) }
        val f2 = f1.dropAll()
        f2.test(timeout = 3_010.milliseconds) {
            awaitComplete()
        }
    }
})