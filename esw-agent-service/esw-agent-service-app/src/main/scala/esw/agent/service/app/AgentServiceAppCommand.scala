/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package esw.agent.service.app

import caseapp.{CommandName, ExtraName, HelpMessage}

/**
 * AgentServiceAppCommand - a set of command line param written using case app for the Agent Service App
 */
sealed trait AgentServiceAppCommand

object AgentServiceAppCommand {

  @CommandName("start")
  final case class StartCommand(
      @ExtraName("p")
      @HelpMessage(
        "optional argument: port on which HTTP server will be bound. " +
          "If a value is not provided, it will be randomly picked."
      )
      port: Option[Int]
  ) extends AgentServiceAppCommand
}
