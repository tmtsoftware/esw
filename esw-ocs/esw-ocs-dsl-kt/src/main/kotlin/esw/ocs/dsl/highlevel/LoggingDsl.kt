package esw.ocs.dsl.highlevel

import csw.logging.api.javadsl.ILogger

interface LoggingDsl {

    val logger: ILogger

    /**
     * Writes trace level log message.
     *
     * @param message the message to be logged
     * @param extraInfo key-value pairs to be logged along with message
     */
    fun trace(message: String, extraInfo: Map<String, Any> = emptyMap()) =
            logger.trace(message, extraInfo)

    /**
     * Writes debug level log message.
     *
     * @param message the message to be logged
     * @param extraInfo key-value pairs to be logged along with message
     */
    fun debug(message: String, extraInfo: Map<String, Any> = emptyMap()) =
            logger.debug(message, extraInfo)

    /**
     * Writes info level log message.
     *
     * @param message the message to be logged
     * @param extraInfo key-value pairs to be logged along with message
     */
    fun info(message: String, extraInfo: Map<String, Any> = emptyMap()) =
            logger.info(message, extraInfo)

    /**
     * Writes warn level log message.
     *
     * @param message the message to be logged
     * @param cause optional exception to be logged together with its stack trace
     * @param extraInfo key-value pairs to be logged along with message
     */
    fun warn(message: String, cause: Throwable? = null, extraInfo: Map<String, Any> = emptyMap()) =
            cause?.let { logger.warn(message, extraInfo, cause) }
                    ?: logger.warn(message, extraInfo)

    /**
     * Writes error level log message.
     *
     * @param message the message to be logged
     * @param cause optional exception to be logged together with its stack trace
     * @param extraInfo key-value pairs to be logged along with message
     */
    fun error(message: String, cause: Throwable? = null, extraInfo: Map<String, Any> = emptyMap()) =
            cause?.let { logger.error(message, extraInfo, cause) }
                    ?: logger.error(message, extraInfo)

    /**
     * Writes fatal level log message.
     *
     * @param message the message to be logged
     * @param cause optional exception to be logged together with its stack trace
     * @param extraInfo key-value pairs to be logged along with message
     */
    fun fatal(message: String, cause: Throwable? = null, extraInfo: Map<String, Any> = emptyMap()) =
            cause?.let { logger.fatal(message, extraInfo, cause) }
                    ?: logger.fatal(message, extraInfo)
}