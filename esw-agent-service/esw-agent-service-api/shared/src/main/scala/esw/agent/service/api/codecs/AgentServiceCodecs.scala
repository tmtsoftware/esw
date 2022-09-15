/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package esw.agent.service.api.codecs

import esw.agent.service.api.models.AgentNotFoundException
import esw.agent.service.api.protocol.AgentServiceRequest
import io.bullet.borer.Codec
import io.bullet.borer.derivation.MapBasedCodecs.{deriveAllCodecs, deriveCodec}
import msocket.api.ErrorProtocol

object AgentServiceCodecs extends AgentServiceCodecs

/**
 * Codecs for the http models which are being used while communication via http.
 */
trait AgentServiceCodecs extends AgentCodecs {

  lazy implicit val agentHttpMessageCodecs: Codec[AgentServiceRequest] = deriveAllCodecs

  lazy implicit val agentExceptionCodec: Codec[AgentNotFoundException] = deriveCodec

  implicit lazy val agentHttpErrorProtocol: ErrorProtocol[AgentServiceRequest] =
    ErrorProtocol.bind[AgentServiceRequest, AgentNotFoundException]

}
