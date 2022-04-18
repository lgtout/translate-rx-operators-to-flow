@file:OptIn(ExperimentalKotest::class)

package com.lagostout.flow

import io.kotest.common.ExperimentalKotest
import io.kotest.core.config.AbstractProjectConfig
import io.kotest.core.config.LogLevel
import io.kotest.core.extensions.Extension
import io.kotest.core.test.TestCase
import io.kotest.engine.test.logging.LogEntry
import io.kotest.engine.test.logging.LogExtension

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