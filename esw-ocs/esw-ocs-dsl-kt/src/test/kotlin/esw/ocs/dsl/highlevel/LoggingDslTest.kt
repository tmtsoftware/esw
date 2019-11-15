package esw.ocs.dsl.highlevel

import csw.logging.api.javadsl.ILogger
import csw.params.core.models.Prefix
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test

class LoggingDslTest : LoggingDsl {
    override val logger: ILogger = mockk()
    override val prefix: Prefix = mockk()

    private val message = "log message"
    private val mapOnlyWithPrefixEntry = mapOf("prefix" to prefix)

    @Test
    fun `trace method should call the Logger#trace with given message and a map only with prefix entry when map is not provided | ESW-127`() {
        every { logger.trace(message, mapOnlyWithPrefixEntry) }.answers { Unit }

        trace(message)
        verify { logger.trace(message, mapOnlyWithPrefixEntry) }
    }

    @Test
    fun `trace method should call the Logger#trace with given message and given map inserted with prefix entry | ESW-127`() {
        val map = mapOf("a" to "c", "g" to "h")
        val mapWithPrefixEntry = map + mapOnlyWithPrefixEntry
        every { logger.trace(message, mapWithPrefixEntry) }.answers { Unit }

        trace(message, map)
        verify { logger.trace(message, mapWithPrefixEntry) }
    }

    @Test
    fun `debug method should call the Logger#debug with given message and a map only with prefix entry when map is not provided | ESW-127`() {
        every { logger.debug(message, mapOnlyWithPrefixEntry) }.answers { Unit }

        debug(message)
        verify { logger.debug(message, mapOnlyWithPrefixEntry) }
    }

    @Test
    fun `debug method should call the Logger#debug with given message and given map inserted with prefix entry | ESW-127`() {
        val map = mapOf("a" to "c", "g" to "h")
        val mapWithPrefixEntry = map + mapOnlyWithPrefixEntry
        every { logger.debug(message, mapWithPrefixEntry) }.answers { Unit }

        debug(message, map)
        verify { logger.debug(message, mapWithPrefixEntry) }
    }

    @Test
    fun `info method should call the Logger#info with given message and a map only with prefix entry when map is not provided | ESW-127`() {
        every { logger.info(message, mapOnlyWithPrefixEntry) }.answers { Unit }

        info(message)
        verify { logger.info(message, mapOnlyWithPrefixEntry) }
    }

    @Test
    fun `info method should call the Logger#info with given message and given map inserted with prefix entry | ESW-127`() {
        val map = mapOf("a" to "c", "g" to "h")
        val mapWithPrefixEntry = map + mapOnlyWithPrefixEntry
        every { logger.info(message, mapWithPrefixEntry) }.answers { Unit }

        info(message, map)
        verify { logger.info(message, mapWithPrefixEntry) }
    }

    @Test
    fun `warn method should call the Logger#warn with given message and with default value of absent argument | ESW-127`() {
        every { logger.warn(message, mapOnlyWithPrefixEntry, any<Throwable>()) }.answers { Unit }

        warn(message)
        verify { logger.warn(message, mapOnlyWithPrefixEntry, any<Throwable>()) }
    }

    @Test
    fun `warn method should call the Logger#warn with given message, given map inserted with prefix entry and given exception | ESW-127`() {
        val map = mapOf("a" to "c", "g" to "h")
        val mapWithPrefixEntry = map + mapOnlyWithPrefixEntry
        val ex = Exception("logging failed")
        every { logger.warn(message, mapWithPrefixEntry, ex) }.answers { Unit }

        warn(message, map, ex)
        verify { logger.warn(message, mapWithPrefixEntry, ex) }
    }

    @Test
    fun `error method should call the Logger#error with given message and with default value of absent argument | ESW-127`() {
        every { logger.error(message, mapOnlyWithPrefixEntry, any<Throwable>()) }.answers { Unit }

        error(message)
        verify { logger.error(message, mapOnlyWithPrefixEntry, any<Throwable>()) }
    }

    @Test
    fun `error method should call the Logger#error with given message, given map inserted with prefix entry and given exception | ESW-127`() {
        val map = mapOf("a" to "c", "g" to "h")
        val mapWithPrefixEntry = map + mapOnlyWithPrefixEntry
        val ex = Exception("logging failed")
        every { logger.error(message, mapWithPrefixEntry, ex) }.answers { Unit }

        error(message, map, ex)
        verify { logger.error(message, mapWithPrefixEntry, ex) }
    }

    @Test
    fun `fatal method should call the Logger#fatal with given message and with default value of absent argument | ESW-127`() {
        every { logger.fatal(message, mapOnlyWithPrefixEntry, any<Throwable>()) }.answers { Unit }

        fatal(message)
        verify { logger.fatal(message, mapOnlyWithPrefixEntry, any<Throwable>()) }
    }

    @Test
    fun `fatal method should call the Logger#fatal with given message, given map inserted with prefix entry and given exception | ESW-127`() {
        val map = mapOf("a" to "c", "g" to "h")
        val mapWithPrefixEntry = map + mapOnlyWithPrefixEntry
        val ex = Exception("logging failed")
        every { logger.fatal(message, mapWithPrefixEntry, ex) }.answers { Unit }

        fatal(message, map, ex)
        verify { logger.fatal(message, mapWithPrefixEntry, ex) }
    }

}