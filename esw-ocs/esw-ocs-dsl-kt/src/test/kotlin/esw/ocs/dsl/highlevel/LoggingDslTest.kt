/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package esw.ocs.dsl.highlevel

import csw.logging.api.javadsl.ILogger
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test

@Suppress("DANGEROUS_CHARACTERS")
class LoggingDslTest : LoggingDsl {
    override val logger: ILogger = mockk()

    private val message = "log message"
    val emptyMap = emptyMap<String, Any>()

    @Test
    fun `trace method should call the Logger#trace with given message and a map only with prefix entry when map is not provided | ESW-127`() {
        every { logger.trace(message, emptyMap) }.answers { Unit }

        trace(message)
        verify { logger.trace(message, emptyMap) }
    }

    @Test
    fun `trace method should call the Logger#trace with given message and given map inserted with prefix entry | ESW-127`() {
        val map = mapOf("a" to "c", "g" to "h")
        every { logger.trace(message, map) }.answers { Unit }

        trace(message, map)
        verify { logger.trace(message, map) }
    }

    @Test
    fun `debug method should call the Logger#debug with given message and a map only with prefix entry when map is not provided | ESW-127`() {
        every { logger.debug(message, emptyMap) }.answers { Unit }

        debug(message)
        verify { logger.debug(message, emptyMap) }
    }

    @Test
    fun `debug method should call the Logger#debug with given message and given map inserted with prefix entry | ESW-127`() {
        val map = mapOf("a" to "c", "g" to "h")
        every { logger.debug(message, map) }.answers { Unit }

        debug(message, map)
        verify { logger.debug(message, map) }
    }

    @Test
    fun `info method should call the Logger#info with given message and a map only with prefix entry when map is not provided | ESW-127`() {
        every { logger.info(message, emptyMap) }.answers { Unit }

        info(message)
        verify { logger.info(message, emptyMap) }
    }

    @Test
    fun `info method should call the Logger#info with given message and given map inserted with prefix entry | ESW-127`() {
        val map = mapOf("a" to "c", "g" to "h")
        every { logger.info(message, map) }.answers { Unit }

        info(message, map)
        verify { logger.info(message, map) }
    }

    @Test
    fun `warn method should call the Logger#warn with given message and with default value of absent argument | ESW-127`() {
        every { logger.warn(message, emptyMap) }.answers { Unit }

        warn(message)
        verify { logger.warn(message, emptyMap) }
    }

    @Test
    fun `warn method should call the Logger#warn with given message, given map inserted with prefix entry and given exception | ESW-127`() {
        val map = mapOf("a" to "c", "g" to "h")
        val ex = Exception("logging failed")
        every { logger.warn(message, map, ex) }.answers { Unit }

        warn(message, ex, map)
        verify { logger.warn(message, map, ex) }
    }

    @Test
    fun `error method should call the Logger#error with given message and with default value of absent argument | ESW-127`() {
        every { logger.error(message, emptyMap) }.answers { Unit }

        error(message)
        verify { logger.error(message, emptyMap) }
    }

    @Test
    fun `error method should call the Logger#error with given message, given map inserted with prefix entry and given exception | ESW-127`() {
        val map = mapOf("a" to "c", "g" to "h")
        val ex = Exception("logging failed")
        every { logger.error(message, map, ex) }.answers { Unit }

        error(message, ex, map)
        verify { logger.error(message, map, ex) }
    }

    @Test
    fun `fatal method should call the Logger#fatal with given message and with default value of absent argument | ESW-127`() {
        every { logger.fatal(message, emptyMap) }.answers { Unit }

        fatal(message)
        verify { logger.fatal(message, emptyMap) }
    }

    @Test
    fun `fatal method should call the Logger#fatal with given message, given map inserted with prefix entry and given exception | ESW-127`() {
        val map = mapOf("a" to "c", "g" to "h")
        val ex = Exception("logging failed")
        every { logger.fatal(message, map, ex) }.answers { Unit }

        fatal(message, ex, map)
        verify { logger.fatal(message, map, ex) }
    }

}