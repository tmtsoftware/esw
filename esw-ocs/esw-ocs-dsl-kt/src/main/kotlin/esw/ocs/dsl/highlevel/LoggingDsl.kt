package esw.ocs.dsl.highlevel

import csw.logging.api.javadsl.ILogger

interface LoggingDsl {

    val logger: ILogger

    fun trace(message: String, map: Map<String, Any> = emptyMap()) =
            logger.trace(message, map)

    fun debug(message: String, map: Map<String, Any> = emptyMap()) =
            logger.debug(message, map)

    fun info(message: String, map: Map<String, Any> = emptyMap()) =
            logger.info(message, map)

    fun warn(message: String, map: Map<String, Any> = emptyMap(), ex: Throwable? = null) =
            ex?.let { logger.warn(message, map, ex) }
                    ?: logger.warn(message, map)

    fun error(message: String, map: Map<String, Any> = emptyMap(), ex: Throwable? = null) =
            ex?.let { logger.error(message, map, ex) }
                    ?: logger.error(message, map)

    fun fatal(message: String, map: Map<String, Any> = emptyMap(), ex: Throwable? = null) =
            ex?.let { logger.fatal(message, map, ex) }
                    ?: logger.fatal(message, map)
}