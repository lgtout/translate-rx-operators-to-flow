@file:OptIn(FlowPreview::class)

package com.lagostout.flow

import app.cash.turbine.test
import arrow.core.left
import arrow.core.right
import com.lagostout.turbine.awaitItems
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*

class FlowAmbTests2 : StringSpec({

    // region ambUsingJobsToCancelLosingRacersAndUsingEndSignalAndEmittingFailures

    """Flow.ambUsingJobsToCancelLosingRacersAndUsingEndSignalAndEmittingFailures with three flows and third flow wins and flow emissions overlap in time""" {
        val e1 = Exception("Bang!")
        val f1 = flowOf(100, 200, 300, 400).onEach { delay(150) }
        val f2 = flowOf(10, 20, 30, 40).onEach { delay(100) }
        val f3 = flowOf(1, 2, 3, 4).onEach { delay(50) }
        val f4 = f1.ambUsingJobsToCancelLosingRacersAndUsingEndSignalAndEmittingFailures(f2, f3)
        f4.test {
            awaitItems(4) shouldBe listOf(1, 2, 3, 4).map { it.right() }
            awaitComplete()
        }
    }

    """Flow.ambUsingJobsToCancelLosingRacersAndUsingEndSignalAndEmittingFailures with three flows and first flow fails with no initial delay and second flow wins""" {
        val e1 = Exception("Bang!")
        val f1 = flow<Int> { throw e1 }.onStart { delay(50) }
        val f2 = flowOf(10, 20, 30).onStart { delay(150) }
        val f3 = flowOf(1, 2, 3, 4).onEach { delay(100) }
        val f4 = f1.ambUsingJobsToCancelLosingRacersAndUsingEndSignalAndEmittingFailures(f2, f3)
        f4.test {
            awaitItems(5) shouldBe listOf(e1.left()) + listOf(1, 2, 3, 4).map { it.right() }
            awaitComplete()
        }
    }

    """Flow.ambUsingJobsToCancelLosingRacersAndUsingEndSignalAndEmittingFailures with three flows and first flow fails while winner is emitting and second flow wins""" {
        val e1 = Exception("Bang!")
        val f1 = flow<Int> { throw e1 }.onStart { delay(100) }
        val f2 = flowOf(10, 20, 30).onEach { delay(50) }
        val f3 = flowOf(1, 2, 3, 4).onEach { delay(200) }
        val f4 = f1.ambUsingJobsToCancelLosingRacersAndUsingEndSignalAndEmittingFailures(f2, f3)
        f4.test {
            awaitItems(3) shouldBe listOf(10, 20, 30).map { it.right() }
            awaitComplete()
        }
    }

    """Flow.ambUsingJobsToCancelLosingRacersAndUsingEndSignalAndEmittingFailures with one flow and flow throws an exception""" {
        val e1 = Exception("Bang!")
        val f1 = flow<Int> { throw e1 }.onStart { delay(50) }
        val f2 = f1.ambUsingJobsToCancelLosingRacersAndUsingEndSignalAndEmittingFailures()
        f2.test {
            awaitItem() shouldBe e1.left()
            awaitComplete()
        }
    }

    """Flow.ambUsingJobsToCancelLosingRacersAndUsingEndSignalAndEmittingFailures with two flows and both flows throw an exception""" {
        val e1 = Exception("Bang1!")
        val e2 = Exception("Bang2!")
        val f1 = flow<Int> { throw e1 }.onStart { delay(150) }
        val f2 = flow<Int> { throw e2 }.onStart { delay(50) }
        val f3 = f1.ambUsingJobsToCancelLosingRacersAndUsingEndSignalAndEmittingFailures(f2)
        f3.test {
            awaitItems(2) shouldBe listOf(e2, e1).map { it.left() }
            awaitComplete()
        }
    }

    // endregion

    // region ambUsingSelectAndDroppingFailingAndEmptyFlowsFromRaceAndEmittingFailures

    """Flow.ambUsingSelectAndDroppingFailingAndEmptyFlowsFromRaceAndEmittingFailures with one empty flow""" {
        val f1 = emptyFlow<Int>()
        val f2 = f1.ambUsingSelectAndDroppingFailingAndEmptyFlowsFromRaceAndEmittingFailures()
        f2.test {
            awaitComplete()
        }
    }

    """Flow.ambUsingSelectAndDroppingFailingAndEmptyFlowsFromRaceAndEmittingFailures with two empty flows""" {
        val f1 = emptyFlow<Int>().onStart { delay(100) }
        val f2 = emptyFlow<Int>().onStart { delay(50) }
        val f3 = f1.ambUsingSelectAndDroppingFailingAndEmptyFlowsFromRaceAndEmittingFailures(f2)
        f3.test {
            awaitComplete()
        }
    }

    """Flow.ambUsingSelectAndDroppingFailingAndEmptyFlowsFromRaceAndEmittingFailures with one empty flow and one failing flow and the failing flow fails before the empty flow completes""" {
        val e = Exception("Bang!")
        val f1 = emptyFlow<Int>().onStart { delay(100) }
        val f2 = flow<Int> { throw(e) }.onStart { delay(50) }
        val f3 = f1.ambUsingSelectAndDroppingFailingAndEmptyFlowsFromRaceAndEmittingFailures(f2)
        f3.test {
            awaitItem() shouldBe e.left()
            awaitComplete()
        }
    }

    """Flow.ambUsingSelectAndDroppingFailingAndEmptyFlowsFromRaceAndEmittingFailures with one flow with a single emission and a second failing flow and the failing flow fails before the empty flow emits""" {
        val e = Exception("Bang!")
        val f1 = flowOf(1).onStart { delay(100) }
        val f2 = flow<Int> { throw(e) }.onStart { delay(50) }
        val f3 = f1.ambUsingSelectAndDroppingFailingAndEmptyFlowsFromRaceAndEmittingFailures(f2)
        f3.test {
            awaitItems(2) shouldBe listOf(e.left(), 1.right())
            awaitComplete()
        }
    }

    // TODO More Tests!?

    // endregion

    // region ambUsingProduceInAndFirstFlowThatEmitsOrCompletesWinsAndFailuresRethrown

    """Flow.ambUsingProduceInAndFirstFlowThatEmitsOrCompletesWinsAndFailuresRethrown with one empty flow""" {
        val f1 = emptyFlow<Int>()
        val f2 = f1.ambUsingProduceInAndFirstFlowThatEmitsOrCompletesWinsAndFailuresRethrown()
        f2.test {
            awaitComplete()
        }
    }

    """Flow.ambUsingProduceInAndFirstFlowThatEmitsOrCompletesWinsAndFailuresRethrown with two empty flows""" {
        val f1 = emptyFlow<Int>().onStart { delay(50) }
        val f2 = emptyFlow<Int>().onStart { delay(100) }
        val f3 = f1.ambUsingProduceInAndFirstFlowThatEmitsOrCompletesWinsAndFailuresRethrown(f2)
        f3.test {
            awaitComplete()
        }
    }

    """Flow.ambUsingProduceInAndFirstFlowThatEmitsOrCompletesWinsAndFailuresRethrown with empty flows ???""" {
        val f1 = emptyFlow<Int>().onStart { delay(50) }
        val f2 = emptyFlow<Int>().onStart { delay(100) }
        val f3 = f1.ambUsingProduceInAndFirstFlowThatEmitsOrCompletesWinsAndFailuresRethrown(f2)
        f3.test {
            awaitComplete()
        }
    }
    // endregion

    """foo""" {
    }

})