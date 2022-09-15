/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package esw.gateway.api

import akka.Done
import csw.logging.models.Level
import csw.prefix.models.Prefix

import scala.concurrent.Future

trait LoggingApi {

  /**
   * Logs the message into gateway server
   *
   * @param prefix      prefix for the component with `subsystem` and `name`
   * @param level       represents log level to set
   * @param message     the log message
   * @param metadata    the log metadata containing supporting info
   * @return            a Future that completes and logs the message
   */
  def log(prefix: Prefix, level: Level, message: String, metadata: Map[String, Any] = Map.empty): Future[Done]
}
