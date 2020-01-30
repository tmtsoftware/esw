package esw.http.core.commons

import csw.location.api.models.Connection.HttpConnection
import csw.logging.client.scaladsl.LoggerFactory

/**
 * All the logs generated from the service will have a fixed prefix, which is picked from configuration.
 * "http-server.prefix" is read from configuration file and used as the componentName for LoggerFactory.
 * The prefix helps in production to filter out logs from a particular component and in this case,
 * it helps to filter out logs generated from the service that uses this template.
 */
class ServiceLogger(httpConnection: HttpConnection) extends LoggerFactory(httpConnection.prefix)
