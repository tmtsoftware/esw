/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package esw.agent.service.api.models

/**
 * Model representing when the request Agent is not up & running.
 * @param msg [[java.lang.String]] - a hint containing information about agent.
 */
case class AgentNotFoundException(msg: String) extends RuntimeException(msg)
