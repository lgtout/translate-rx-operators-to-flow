package com.lagostout.kotest.examples

import io.kotest.common.ExperimentalKotest
import io.kotest.core.spec.style.FunSpec
import io.kotest.engine.test.logging.debug
import io.kotest.engine.test.logging.warn
import io.kotest.engine.test.logging.error

@OptIn(ExperimentalKotest::class)
class TestsWithLogging : FunSpec({
    test("foo") {
        warn { "bar" }
    }

    test("foo2") {
        warn { "barbaz" }
        warn { "bloop" }
        debug { "bam-bam" }
        error { "boom" }
    }
})