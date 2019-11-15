package esw.ocs.dsl.highlevel

import csw.logging.api.javadsl.ILogger
import csw.params.core.models.Prefix

interface LoggingDsl {

    val logger: ILogger
    val prefix: Prefix

    private fun noLogException(): Exception = Exception("No Log Exception")

    private fun insertPrefix(map: Map<String, Any>): Map<String, Any> = map + ("prefix" to prefix)

    fun trace(message: String, map: Map<String, Any> = emptyMap()) {
        logger.trace(message, insertPrefix(map))
    }

    fun debug(message: String, map: Map<String, Any> = emptyMap()) {
        logger.debug(message, insertPrefix(map))
    }

    fun info(message: String, map: Map<String, Any> = emptyMap()) {
        logger.info(message, insertPrefix(map))
    }

    fun warn(message: String, map: Map<String, Any> = emptyMap(), ex: Throwable = noLogException()) {
        logger.warn(message, insertPrefix(map), ex)
    }

    fun error(message: String, map: Map<String, Any> = emptyMap(), ex: Throwable = noLogException()) {
        logger.error(message, insertPrefix(map), ex)
    }

    fun fatal(message: String, map: Map<String, Any> = emptyMap(), ex: Throwable = noLogException()) {
        logger.fatal(message, insertPrefix(map), ex)
    }
}