package com.lagostout.flow

import app.cash.turbine.test
import arrow.core.Option.Companion.fromNullable
import arrow.core.left
import arrow.core.right
import com.lagostout.turbine.awaitItems
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import org.junit.jupiter.api.Assertions

class FlowAmbTests1 : StringSpec({

    // region amb

    """Flow.amb with 2 flows and second flow wins""" {
        val f1 = flowOf(1, 2, 3).onStart { delay(100) }
        val f2 = flowOf(10, 20, 30).onStart { delay(50) }
        val f3 = f1.amb(f2)
        f3.test {
            Assertions.assertEquals(10, awaitItem())
            Assertions.assertEquals(20, awaitItem())
            Assertions.assertEquals(30, awaitItem())
            awaitComplete()
        }
    }

    """Flow.amb with 2 flows and first flow wins""" {
        val f1 = flowOf(1, 2, 3).onStart { delay(50) }
        val f2 = flowOf(10, 20, 30).onStart { delay(100) }
        val f3 = f1.amb(f2)
        f3.test {
            Assertions.assertEquals(1, awaitItem())
            Assertions.assertEquals(2, awaitItem())
            Assertions.assertEquals(3, awaitItem())
            awaitComplete()
        }
    }

    // endregion

    // region ambUsingVararg

    """Flow.ambUsingVararg with 3 flows and second flow wins""" {
        val f1 = flowOf(1, 2, 3).onStart { delay(100) }
        val f2 = flowOf(10, 20, 30).onStart { delay(50) }
        val f3 = flowOf(100, 200, 300).onStart { delay(150) }
        val f4 = f1.ambUsingVarargAndChannelFlow(f2, f3)
        f4.test {
            Assertions.assertEquals(10, awaitItem())
            Assertions.assertEquals(20, awaitItem())
            Assertions.assertEquals(30, awaitItem())
            awaitComplete()
        }
    }

    """Flow.ambUsingVararg with 3 flows and first flow wins""" {
        val f1 = flowOf(1, 2, 3).onStart { delay(50) }
        val f2 = flowOf(10, 20, 30).onStart { delay(100) }
        val f3 = flowOf(100, 200, 300).onStart { delay(150) }
        val f4 = f1.ambUsingVarargAndChannelFlow(f2, f3)
        f4.test {
            Assertions.assertEquals(1, awaitItem())
            Assertions.assertEquals(2, awaitItem())
            Assertions.assertEquals(3, awaitItem())
            awaitComplete()
        }
    }

    """Flow.ambUsingVararg with 3 flows and third flow wins""" {
        val f1 = flowOf(1, 2, 3).onStart { delay(100) }
        val f2 = flowOf(10, 20, 30).onStart { delay(150) }
        val f3 = flowOf(100, 200, 300).onStart { delay(50) }
        val f4 = f1.ambUsingVarargAndChannelFlow(f2, f3)
        f4.test {
            Assertions.assertEquals(100, awaitItem())
            Assertions.assertEquals(200, awaitItem())
            Assertions.assertEquals(300, awaitItem())
            awaitComplete()
        }
    }

    // endregion

    // region ambUsingSelect

    """Flow.ambUsingSelect with 3 flows and second flow wins""" {
        val f1 = flowOf(1, 2, 3).onStart { delay(100) }
        val f2 = flowOf(10, 20, 30).onStart { delay(50) }
        val f3 = flowOf(100, 200, 300).onStart { delay(150) }
        val f4 = f1.ambUsingSelect(f2, f3)
        f4.test {
            Assertions.assertEquals(10, awaitItem())
            Assertions.assertEquals(20, awaitItem())
            Assertions.assertEquals(30, awaitItem())
            awaitComplete()
        }
    }

    """Flow.ambUsingSelect with three flows and first flow wins""" {
        val f1 = flowOf(1, 2, 3).onStart { delay(50) }
        val f2 = flowOf(10, 20, 30).onStart { delay(100) }
        val f3 = flowOf(100, 200, 300).onStart { delay(150) }
        val f4 = f1.ambUsingSelect(f2, f3)
        f4.test {
            Assertions.assertEquals(1, awaitItem())
            Assertions.assertEquals(2, awaitItem())
            Assertions.assertEquals(3, awaitItem())
            awaitComplete()
        }
    }

    """Flow.ambUsingSelect with three flows and third flow wins""" {
        val f1 = flowOf(1, 2, 3).onStart { delay(100) }
        val f2 = flowOf(10, 20, 30).onStart { delay(150) }
        val f3 = flowOf(100, 200, 300).onStart { delay(50) }
        val f4 = f1.ambUsingSelect(f2, f3)
        f4.test {
            Assertions.assertEquals(100, awaitItem())
            Assertions.assertEquals(200, awaitItem())
            Assertions.assertEquals(300, awaitItem())
            awaitComplete()
        }
    }

    """Flow.ambUsingSelect with three flows and second empty flow with initial delay wins""" {
        val f1 = flowOf(1, 2, 3).onStart { delay(150) }
        val f2 = emptyFlow<Int>().onStart { delay(50) }
        val f3 = flowOf(100, 200, 300).onStart { delay(100) }
        val f4 = f1.ambUsingSelect(f2, f3)
        f4.test {
            awaitComplete()
        }
    }

    """Flow.ambUsingSelect with three flows and second flow throws exception immediately""" {
        val f1 = flowOf(1, 2, 3).onStart { delay(150) }
        val e = Exception("Bang!")
        val f2 = flow<Int> {
            throw e
        }
        val f3 = flowOf(100, 200, 300).onStart { delay(100) }
        val f4 = f1.ambUsingSelect(f2, f3)
        f4.test {
            e shouldBe awaitError()
        }
    }

    """Flow.ambUsingSelect with three flows and second flow throws exception after initial delay""" {
        val f1 = flowOf(1, 2, 3).onStart { delay(150) }
        val e = Exception("Bang!")
        val f2 = flow<Int> {
            throw e
        }.onStart { delay(50) }
        val f3 = flowOf(100, 200, 300).onStart { delay(100) }
        val f4 = f1.ambUsingSelect(f2, f3)
        f4.test {
            e shouldBe awaitError()
        }
    }

    """Flow.ambUsingSelect with three flows and second flow throws exception after first emission""" {
        val f1 = flowOf(1, 2, 3).onStart { delay(150) }
        val e = Exception("Bang!")
        val f2 = flow {
            emit(10)
            delay(50)
            throw e
        }.onStart { delay(50) }
        val f3 = flowOf(100, 200, 300).onStart { delay(100) }
        val f4 = f1.ambUsingSelect(f2, f3)
        f4.test {
            awaitItem() shouldBe 10
            awaitError() shouldBe e
        }
    }

    """Flow.ambUsingSelect with 2 flows and first flow emission is null and first flow wins""" {
        val f1 = flowOf(null, 1, 2).map {
            fromNullable(it)
        }
        val f2 = flowOf(10, 20, 30).onStart { delay(100) }.map { fromNullable(it) }
        val f3 = f1.ambUsingSelect(f2).map { o ->
            o.fold({ null }) { it }
        }
        f3.test {
            awaitItems(3) shouldBe listOf(null, 1, 2)
            awaitComplete()
        }
    }

    // endregion

    // region ambUsingSelectAndDroppingFailingFlowsFromRace

    """Flow.ambUsingSelectAndDroppingFailingFlowsFromRace with three flows and second flow fails after initial delay""" {
        val f1 = flowOf(1, 2, 3).onStart { delay(150) }
        val e = Exception("Bang!")
        val f2 = flow<Int> {
            throw e
        }.onStart { delay(50) }
        val f3 = flowOf(100, 200, 300).onStart { delay(100) }
        val f4 = f1.ambUsingSelectAndDroppingFailingFlowsFromRace(f2, f3)
        f4.test {
            listOf(awaitItem(), awaitItem(), awaitItem())shouldBe listOf(100, 200, 300)
            awaitComplete()
        }
    }

    """Flow.ambUsingSelectAndDroppingFailingFlowsFromRace with three flows and first and second flows fail after initial delay""" {
        val e1 = Exception("Bang!")
        val e2 = Exception("Bang more!")
        val f1 = flow<Int> { throw e1 }.onStart { delay(100) }
        val f2 = flow<Int> {
            throw e2
        }.onStart { delay(50) }
        val f3 = flowOf(100, 200, 300).onStart { delay(150) }
        val f4 = f1.ambUsingSelectAndDroppingFailingFlowsFromRace(f2, f3)
        f4.test {
            listOf(awaitItem(), awaitItem(), awaitItem())shouldBe listOf(100, 200, 300)
            awaitComplete()
        }
    }

    """Flow.ambUsingSelectAndDroppingFailingFlowsFromRace with three flows and second and third flows fail after initial delay""" {
        val e1 = Exception("Bang!")
        val e2 = Exception("Bang more!")
        val f1 = flowOf(100, 200, 300).onStart { delay(150) }
        val f2 = flow<Int> { throw e1 }.onStart { delay(100) }
        val f3 = flow<Int> { throw e2 }.onStart { delay(50) }
        val f4 = f1.ambUsingSelectAndDroppingFailingFlowsFromRace(f2, f3)
        f4.test {
            listOf(awaitItem(), awaitItem(), awaitItem()) shouldBe listOf(100, 200, 300)
            awaitComplete()
        }
    }

    """Flow.ambUsingSelectAndDroppingFailingFlowsFromRace with three flows and all flows fail after initial delay""" {
        val e1 = Exception("Bang!")
        val e2 = Exception("Bang more!")
        val e3 = Exception("Bang more and more!")
        val f1 = flow<Int>{ throw e1 }.onStart { delay(150) }
        val f2 = flow<Int> { throw e2 }.onStart { delay(100) }
        val f3 = flow<Int> { throw e3 }.onStart { delay(50) }
        val f4 = f1.ambUsingSelectAndDroppingFailingFlowsFromRace(f2, f3)
        f4.test {
            listOf(e3.left(), e2.left(), e3.left()) shouldBe listOf()
            awaitComplete()
        }
    }

    // endregion

    // region ambUsingSelectAndDroppingFailingFlowsFromRaceAndEmittingFailures

    """Flow.ambUsingSelectAndDroppingFailingFlowsFromRaceAndEmittingFailures with three flows and all flows fail after initial delay""" {
        val e1 = Exception("Bang!")
        val e2 = Exception("Bang more!")
        val e3 = Exception("Bang more and more!")
        val f1 = flow<Int>{ throw e1 }.onStart { delay(150) }
        val f2 = flow<Int> { throw e2 }.onStart { delay(100) }
        val f3 = flow<Int> { throw e3 }.onStart { delay(50) }
        val f4 = f1.ambUsingSelectAndDroppingFailingFlowsFromRaceAndEmittingFailures(f2, f3)
        f4.test {
            awaitItems(3) shouldBe listOf(e3, e2, e1).map { it.left() }
            awaitComplete()
        }
    }

    """Flow.ambUsingSelectAndDroppingFailingFlowsFromRaceAndEmittingFailures with three flows and second and third flows fail after initial delay and before first flow wins""" {
        val e1 = Exception("Bang!")
        val e2 = Exception("Bang more!")
        val f1 = flowOf(1, 2, 3).onStart { delay(150) }
        val f2 = flow<Int> { throw e1 }.onStart { delay(100) }
        val f3 = flow<Int> { throw e2 }.onStart { delay(50) }
        val f4 = f1.ambUsingSelectAndDroppingFailingFlowsFromRaceAndEmittingFailures(f2, f3)
        f4.test {
            awaitItems(5) shouldBe listOf(e2, e1).map { it.left() } + listOf(1, 2, 3).map { it.right() }
            awaitComplete()
        }
    }

    // endregion

    // region ambUsingSelectAndDroppingFailingAndEmptyFlowsFromRaceAndEmittingFailures

    """Flow.ambUsingSelectAndDroppingFailingAndEmptyFlowsFromRaceAndEmittingFailures with three flows and all flows fail after initial delay""" {
        val e1 = Exception("Bang!")
        val e2 = Exception("Bang more!")
        val e3 = Exception("Bang more and more!")
        val f1 = flow<Int>{ throw e1 }.onStart { delay(150) }
        val f2 = flow<Int> { throw e2 }.onStart { delay(100) }
        val f3 = flow<Int> { throw e3 }.onStart { delay(50) }
        val f4 = f1.ambUsingSelectAndDroppingFailingAndEmptyFlowsFromRaceAndEmittingFailures(f2, f3)
        f4.test {
            awaitItems(3) shouldBe listOf(e3, e2, e1).map { it.left() }
            awaitComplete()
        }
    }

    """Flow.ambUsingSelectAndDroppingFailingAndEmptyFlowsFromRaceAndEmittingFailures with three flows and first flow fails and second flow is empty""" {
        val e1 = Exception("Bang!")
        val f1 = flow<Int>{ throw e1 }.onStart { delay(100) }
        val f2 = emptyFlow<Int>().onStart { delay(50) }
        val f3 = flowOf(1, 2, 3).onStart { delay(150) }
        val f4 = f1.ambUsingSelectAndDroppingFailingAndEmptyFlowsFromRaceAndEmittingFailures(f2, f3)
        f4.test {
            awaitItems(4) shouldBe listOf(e1.left()) + listOf(1, 2, 3).map { it.right() }
            awaitComplete()
        }
    }

    """Flow.ambUsingSelectAndDroppingFailingAndEmptyFlowsFromRaceAndEmittingFailures with three flows and all flows are empty""" {
        val f1 = emptyFlow<Int>().onStart { delay(100) }
        val f2 = emptyFlow<Int>().onStart { delay(50) }
        val f3 = emptyFlow<Int>().onStart { delay(150) }
        val f4 = f1.ambUsingSelectAndDroppingFailingAndEmptyFlowsFromRaceAndEmittingFailures(f2, f3)
        f4.test {
            awaitComplete()
        }
    }

    """Flow.ambUsingSelectAndDroppingFailingAndEmptyFlowsFromRaceAndEmittingFailures with one flow that fails""" {
        val e = Exception("Bang!")
        val f1 = flow<Int> { throw e }.onStart { delay(150) }
        val f2 = f1.ambUsingSelectAndDroppingFailingAndEmptyFlowsFromRaceAndEmittingFailures()
        f2.test {
            awaitItem() shouldBe e.left()
            awaitComplete()
        }
    }

    """Flow.ambUsingSelectAndDroppingFailingAndEmptyFlowsFromRaceAndEmittingFailures with one empty flow""" {
        val f1 = emptyFlow<Int>().onStart { delay(150) }
        val f2 = f1.ambUsingSelectAndDroppingFailingAndEmptyFlowsFromRaceAndEmittingFailures()
        f2.test {
            awaitComplete()
        }
    }

    // endregion

    // region ambUsingSelect

    """Flow.ambUsingSelect with three flows and third empty flow with no initial delay wins""" {
        val f1 = flowOf(1, 2, 3).onStart { delay(150) }
        val f2 = flowOf(10, 20, 30).onStart { delay(50) }
        val f3 = emptyFlow<Int>()
        val f4 = f1.ambUsingSelect(f2, f3)
        f4.test {
            awaitComplete()
        }
    }

    // endregion

    // region ambUsingChannelsAndEmptyFlowWins

    """Flow.ambUsingChannelsAndEmptyFlowWins with 3 flows and second flow wins""" {
        val f1 = flowOf(1, 2, 3).onStart { delay(100) }
        val f2 = flowOf(10, 20, 30).onStart { delay(50) }
        val f3 = flowOf(100, 200, 300).onStart { delay(150) }
        val f4 = f1.ambUsingChannelsAndEmptyFlowCanWin(f2, f3)
        f4.test {
            Assertions.assertEquals(
                listOf(10, 20, 30), listOf(awaitItem(), awaitItem(), awaitItem())
            )
            awaitComplete()
        }
    }

    """Flow.ambUsingChannelsAndEmptyFlowWins with three flows and first flow wins""" {
        val f1 = flowOf(1, 2, 3).onStart { delay(50) }
        val f2 = flowOf(10, 20, 30).onStart { delay(100) }
        val f3 = flowOf(100, 200, 300).onStart { delay(150) }
        val f4 = f1.ambUsingChannelsAndEmptyFlowCanWin(f2, f3)
        f4.test {
            Assertions.assertEquals(1, awaitItem())
            Assertions.assertEquals(2, awaitItem())
            Assertions.assertEquals(3, awaitItem())
            awaitComplete()
        }
    }

    """Flow.ambUsingChannelsAndEmptyFlowWins with three flows and third flow wins""" {
        val f1 = flowOf(1, 2, 3).onStart { delay(100) }
        val f2 = flowOf(10, 20, 30).onStart { delay(150) }
        val f3 = flowOf(100, 200, 300).onStart { delay(50) }
        val f4 = f1.ambUsingChannelsAndEmptyFlowCanWin(f2, f3)
        f4.test {
            Assertions.assertEquals(100, awaitItem())
            Assertions.assertEquals(200, awaitItem())
            Assertions.assertEquals(300, awaitItem())
            awaitComplete()
        }
    }

    """Flow.ambUsingChannelsAndEmptyFlowWins with three flows and second empty flow with initial delay wins""" {
        val f1 = flowOf(1, 2, 3).onStart { delay(150) }
        val f2 = emptyFlow<Int>().onStart { delay(50) }
        val f3 = flowOf(100, 200, 300).onStart { delay(100) }
        val f4 = f1.ambUsingChannelsAndEmptyFlowCanWin(f2, f3)
        f4.test {
            awaitComplete()
        }
    }

    """Flow.ambUsingChannelsAndEmptyFlowWins with three flows and third empty flow with no initial delay wins""" {
        val f1 = flowOf(1, 2, 3).onStart { delay(150) }
        val f2 = flowOf(10, 20, 30).onStart { delay(50) }
        val f3 = emptyFlow<Int>()
        val f4 = f1.ambUsingChannelsAndEmptyFlowCanWin(f2, f3)
        f4.test {
            awaitComplete()
        }
    }

    // endregion

    // region ambUsingChannelsAndIgnoringEmptyFlows

    """Flow.ambUsingChannelsAndIgnoringEmptyFlows with 3 flows and second flow wins""" {
        val f1 = flowOf(1, 2, 3).onStart { delay(100) }
        val f2 = flowOf(10, 20, 30).onStart { delay(50) }
        val f3 = flowOf(100, 200, 300).onStart { delay(150) }
        val f4 = f1.ambUsingChannelsAndIgnoringEmptyFlows(f2, f3)
        f4.test {
            Assertions.assertEquals(10, awaitItem())
            Assertions.assertEquals(20, awaitItem())
            Assertions.assertEquals(30, awaitItem())
            awaitComplete()
        }
    }

    """Flow.ambUsingChannelsAndIgnoringEmptyFlows with three flows and first flow wins""" {
        val f1 = flowOf(1, 2, 3).onStart { delay(50) }
        val f2 = flowOf(10, 20, 30).onStart { delay(100) }
        val f3 = flowOf(100, 200, 300).onStart { delay(150) }
        val f4 = f1.ambUsingChannelsAndIgnoringEmptyFlows(f2, f3)
        f4.test {
            Assertions.assertEquals(1, awaitItem())
            Assertions.assertEquals(2, awaitItem())
            Assertions.assertEquals(3, awaitItem())
            awaitComplete()
        }
    }

    """Flow.ambUsingChannelsAndIgnoringEmptyFlows with three flows and third flow wins""" {
        val f1 = flowOf(1, 2, 3).onStart { delay(100) }
        val f2 = flowOf(10, 20, 30).onStart { delay(150) }
        val f3 = flowOf(100, 200, 300).onStart { delay(50) }
        val f4 = f1.ambUsingChannelsAndIgnoringEmptyFlows(f2, f3)
        f4.test {
            Assertions.assertEquals(100, awaitItem())
            Assertions.assertEquals(200, awaitItem())
            Assertions.assertEquals(300, awaitItem())
            awaitComplete()
        }
    }

    """Flow.ambUsingChannelsAndIgnoringEmptyFlows with three flows and second empty flow with initial delay and third flow wins""" {
        val f1 = flowOf(1, 2, 3).onStart { delay(150) }
        val f2 = emptyFlow<Int>().onStart { delay(50) }
        val f3 = flowOf(100, 200, 300).onStart { delay(100) }
        val f4 = f1.ambUsingChannelsAndIgnoringEmptyFlows(f2, f3)
        f4.test {
            Assertions.assertEquals(100, awaitItem())
            Assertions.assertEquals(200, awaitItem())
            Assertions.assertEquals(300, awaitItem())
            awaitComplete()
        }
    }

    """Flow.ambUsingChannelsAndIgnoringEmptyFlows with three flows and third empty flow with no initial delay and second flow wins""" {
        val f1 = flowOf(1, 2, 3).onStart { delay(150) }
        val f2 = flowOf(10, 20, 30).onStart { delay(50) }
        val f3 = emptyFlow<Int>()
        val f4 = f1.ambUsingChannelsAndIgnoringEmptyFlows(f2, f3)
        f4.test {
            Assertions.assertEquals(10, awaitItem())
            Assertions.assertEquals(20, awaitItem())
            Assertions.assertEquals(30, awaitItem())
            awaitComplete()
        }
    }

    // endregion
})