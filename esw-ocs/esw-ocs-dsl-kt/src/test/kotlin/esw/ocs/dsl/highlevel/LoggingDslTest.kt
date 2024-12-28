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
    fun `trace_method_should_call_the_Logger#trace_with_given_message_and_a_map_only_with_prefix_entry_when_map_is_not_provided_|_ESW-127`() {
        every { logger.trace(message, emptyMap) }.answers { Unit }

        trace(message)
        verify { logger.trace(message, emptyMap) }
    }

    @Test
    fun `trace_method_should_call_the_Logger#trace_with_given_message_and_given_map_inserted_with_prefix_entry_|_ESW-127`() {
        val map = mapOf("a" to "c", "g" to "h")
        every { logger.trace(message, map) }.answers { Unit }

        trace(message, map)
        verify { logger.trace(message, map) }
    }

    @Test
    fun `debug_method_should_call_the_Logger#debug_with_given_message_and_a_map_only_with_prefix_entry_when_map_is_not_provided_|_ESW-127`() {
        every { logger.debug(message, emptyMap) }.answers { Unit }

        debug(message)
        verify { logger.debug(message, emptyMap) }
    }

    @Test
    fun `debug_method_should_call_the_Logger#debug_with_given_message_and_given_map_inserted_with_prefix_entry_|_ESW-127`() {
        val map = mapOf("a" to "c", "g" to "h")
        every { logger.debug(message, map) }.answers { Unit }

        debug(message, map)
        verify { logger.debug(message, map) }
    }

    @Test
    fun `info_method_should_call_the_Logger#info_with_given_message_and_a_map_only_with_prefix_entry_when_map_is_not_provided_|_ESW-127`() {
        every { logger.info(message, emptyMap) }.answers { Unit }

        info(message)
        verify { logger.info(message, emptyMap) }
    }

    @Test
    fun `info_method_should_call_the_Logger#info_with_given_message_and_given_map_inserted_with_prefix_entry_|_ESW-127`() {
        val map = mapOf("a" to "c", "g" to "h")
        every { logger.info(message, map) }.answers { Unit }

        info(message, map)
        verify { logger.info(message, map) }
    }

    @Test
    fun `warn_method_should_call_the_Logger#warn_with_given_message_and_with_default_value_of_absent_argument_|_ESW-127`() {
        every { logger.warn(message, emptyMap) }.answers { Unit }

        warn(message)
        verify { logger.warn(message, emptyMap) }
    }

    @Test
    fun `warn_method_should_call_the_Logger#warn_with_given_message,_given_map_inserted_with_prefix_entry_and_given_exception_|_ESW-127`() {
        val map = mapOf("a" to "c", "g" to "h")
        val ex = Exception("logging failed")
        every { logger.warn(message, map, ex) }.answers { Unit }

        warn(message, ex, map)
        verify { logger.warn(message, map, ex) }
    }

    @Test
    fun `error_method_should_call_the_Logger#error_with_given_message_and_with_default_value_of_absent_argument_|_ESW-127`() {
        every { logger.error(message, emptyMap) }.answers { Unit }

        error(message)
        verify { logger.error(message, emptyMap) }
    }

    @Test
    fun `error_method_should_call_the_Logger#error_with_given_message,_given_map_inserted_with_prefix_entry_and_given_exception_|_ESW-127`() {
        val map = mapOf("a" to "c", "g" to "h")
        val ex = Exception("logging failed")
        every { logger.error(message, map, ex) }.answers { Unit }

        error(message, ex, map)
        verify { logger.error(message, map, ex) }
    }

    @Test
    fun `fatal_method_should_call_the_Logger#fatal_with_given_message_and_with_default_value_of_absent_argument_|_ESW-127`() {
        every { logger.fatal(message, emptyMap) }.answers { Unit }

        fatal(message)
        verify { logger.fatal(message, emptyMap) }
    }

    @Test
    fun `fatal_method_should_call_the_Logger#fatal_with_given_message,_given_map_inserted_with_prefix_entry_and_given_exception_|_ESW-127`() {
        val map = mapOf("a" to "c", "g" to "h")
        val ex = Exception("logging failed")
        every { logger.fatal(message, map, ex) }.answers { Unit }

        fatal(message, ex, map)
        verify { logger.fatal(message, map, ex) }
    }

}