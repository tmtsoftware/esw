/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package esw.backend.testkit

import java.nio.file.Path

import caseapp.core.Error.UnrecognizedArgument
import caseapp.core.argparser.SimpleArgParser
import caseapp.{ExtraName, HelpMessage, _}
import csw.testkit.scaladsl.CSWService._
import esw.backend.testkit.utils.IOUtils
import esw.ocs.testkit.Service
import esw.ocs.testkit.Service.{AAS, AgentService, Gateway, SequenceManager}

sealed trait TSServicesCommands
object TSServicesCommands {

  private lazy val commandRolesPath = IOUtils.writeResourceToFile("commandRoles.conf")

  implicit val serviceParser: SimpleArgParser[Service] =
    SimpleArgParser.from[Service]("service") {
      case "Location"         => Right(LocationServer)
      case "LocationWithAuth" => Right(LocationServerWithAuth)
      case "Event"            => Right(EventServer)
      case "Alarm"            => Right(AlarmServer)
      case "Config"           => Right(ConfigServer)
      case "Gateway"          => Right(Gateway)
      case "AAS"              => Right(AAS)
      case "SequenceManager"  => Right(SequenceManager)
      case "AgentService"     => Right(AgentService)
      case unknown            => Left(UnrecognizedArgument(unknown))
    }

  @CommandName("start")
  final case class Start(
      @HelpMessage("Service name e.g., Location, Gateway etc")
      @ExtraName("s")
      services: List[Service],
      @HelpMessage("path to command role from root directory")
      @ExtraName("r")
      commandRoles: Path = commandRolesPath,
      @HelpMessage("name of alarm config key")
      @ExtraName("a")
      alarmConf: String = "alarm_key.conf"
  ) extends TSServicesCommands
}
