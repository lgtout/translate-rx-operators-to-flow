@file:OptIn(ExperimentalKotest::class)

package com.lagostout.kotest.examples

import io.kotest.common.ExperimentalKotest
import io.kotest.core.config.AbstractProjectConfig
import io.kotest.core.config.LogLevel
import io.kotest.core.extensions.Extension
import io.kotest.core.test.TestCase
import io.kotest.engine.test.logging.LogEntry
import io.kotest.engine.test.logging.LogExtension

// Uncomment.  Commented out to not interfere with config elsewhere, since it is project-wide.

object ProjectConfig : AbstractProjectConfig() {
    override val logLevel: LogLevel = LogLevel.Debug
    override fun extensions(): List<Extension> = listOf(
        object : LogExtension {
            override suspend fun handleLogs(testCase: TestCase, logs: List<LogEntry>) {
                logs.forEach { println(it.level.name + " - " + it.message) }
            }
        }
    )
}