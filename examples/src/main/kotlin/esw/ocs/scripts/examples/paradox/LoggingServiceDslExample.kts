package esw.ocs.scripts.examples.paradox

import esw.ocs.dsl.core.script

script {

    val highTempRaisedEx: Exception by lazy { RuntimeException("Temperature threshold crossed!") }

    //#trace
    trace(message = "logging at trace level")

    trace(message = "logging at trace level",
            extraInfo = mapOf("key1" to "value1", "key2" to "value2"))
    //#trace

    //#debug
    debug(message = "logging at debug level")

    debug(message = "logging at debug level",
            extraInfo = mapOf("key1" to "value1", "key2" to "value2"))
    //#debug

    //#info
    info(message = "logging at info level")

    info(message = "logging at info level",
            extraInfo = mapOf("key1" to "value1", "key2" to "value2"))
    //#info

    //#warn
    warn(message = "logging at warn level")

    warn(message = "logging at warn level", cause = highTempRaisedEx)

    warn(message = "logging at warn level",
            extraInfo = mapOf("key1" to "value1", "key2" to "value2"))

    warn(message = "logging at warn level", cause = highTempRaisedEx,
            extraInfo = mapOf("key1" to "value1", "key2" to "value2"))
    //#warn

    //#error
    error(message = "logging at error level")

    error(message = "logging at error level", cause = highTempRaisedEx)

    error(message = "logging at error level",
            extraInfo = mapOf("key1" to "value1", "key2" to "value2"))

    error(message = "logging at error level", cause = highTempRaisedEx,
            extraInfo = mapOf("key1" to "value1", "key2" to "value2"))
    //#error
}