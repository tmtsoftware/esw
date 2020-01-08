package esw.ocs.dsl.highlevel

import csw.logging.api.javadsl.ILogger

interface LoggingDsl {

    val logger: ILogger

    fun trace(message: String, extraInfo: Map<String, Any> = emptyMap()) =
            logger.trace(message, extraInfo)

    fun debug(message: String, extraInfo: Map<String, Any> = emptyMap()) =
            logger.debug(message, extraInfo)

    fun info(message: String, extraInfo: Map<String, Any> = emptyMap()) =
            logger.info(message, extraInfo)

    fun warn(message: String, cause: Throwable? = null, extraInfo: Map<String, Any> = emptyMap()) =
            cause?.let { logger.warn(message, extraInfo, cause) }
                    ?: logger.warn(message, extraInfo)

    fun error(message: String, cause: Throwable? = null, extraInfo: Map<String, Any> = emptyMap()) =
            cause?.let { logger.error(message, extraInfo, cause) }
                    ?: logger.error(message, extraInfo)

    fun fatal(message: String, cause: Throwable? = null, extraInfo: Map<String, Any> = emptyMap()) =
            cause?.let { logger.fatal(message, extraInfo, cause) }
                    ?: logger.fatal(message, extraInfo)
}