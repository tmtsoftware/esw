/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package esw.gateway.api.clients

import akka.Done
import csw.logging.models.Level
import csw.prefix.models.Prefix
import esw.gateway.api.LoggingApi
import esw.gateway.api.codecs.GatewayCodecs
import esw.gateway.api.protocol.GatewayRequest
import esw.gateway.api.protocol.GatewayRequest.Log
import msocket.api.Transport

import scala.concurrent.Future

/**
 * HTTP client for the Logging Service
 * @param postClient - An Transport class for HTTP calls for the Logging Service
 */
class LoggingClient(postClient: Transport[GatewayRequest]) extends LoggingApi with GatewayCodecs {
  def log(prefix: Prefix, level: Level, message: String, metadata: Map[String, Any]): Future[Done] =
    postClient.requestResponse[Done](Log(prefix, level, message, metadata))
}
