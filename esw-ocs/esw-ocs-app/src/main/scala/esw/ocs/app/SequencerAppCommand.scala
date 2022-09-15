/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package esw.ocs.app

import caseapp.core.Error
import caseapp.core.argparser.SimpleArgParser
import caseapp.{CommandName, HelpMessage, ExtraName => Short}
import csw.prefix.models.{Prefix, Subsystem}
import esw.ocs.api.models.{ObsMode, Variation}

import scala.util.Try

/**
 * SequencerAppCommand - a set of command line param written using case app for the sequencer/sequence component.
 */
sealed trait SequencerAppCommand {
  def seqCompSubsystem: Subsystem
  def name: Option[String]
  def agentPrefix: Option[Prefix]
  def simulation: Boolean
}

object SequencerAppCommand {

  implicit val subsystemParser: SimpleArgParser[Subsystem] =
    SimpleArgParser.from[Subsystem]("subsystem") { subsystemStr =>
      Try(Right(Subsystem.withNameInsensitive(subsystemStr)))
        .getOrElse(Left(Error.Other(s"Subsystem [$subsystemStr] is invalid")))
    }

  implicit val prefixParser: SimpleArgParser[Prefix] =
    SimpleArgParser.from[Prefix]("prefix") { prefixStr =>
      Try(Right(Prefix(prefixStr)))
        .getOrElse(Left(Error.Other(s"Prefix [$prefixStr] is invalid")))
    }

  implicit val obsModeParser: SimpleArgParser[ObsMode] =
    SimpleArgParser.from[ObsMode]("obsMode") { obsModeName =>
      Right(ObsMode(obsModeName))
    }

  implicit val variationParser: SimpleArgParser[Variation] =
    SimpleArgParser.from[Variation]("variation") { variation =>
      Right(Variation(variation))
    }

  @CommandName("seqcomp")
  final case class SequenceComponent(
      @HelpMessage("subsystem of the sequence component, ex: tcs")
      @Short("s")
      seqCompSubsystem: Subsystem,
      @HelpMessage("optional argument: name for sequence component, ex: primary, backup etc")
      @Short("n")
      name: Option[String],
      @HelpMessage("optional argument: agentPrefix on which sequence component will be spawned, ex: ESW.agent1, IRIS.agent2 etc")
      @Short("a")
      agentPrefix: Option[Prefix],
      @HelpMessage("simulation mode")
      @Short("simulation")
      simulation: Boolean = false
  ) extends SequencerAppCommand

  final case class Sequencer(
      @HelpMessage("subsystem of the sequence component, ex: tcs")
      @Short("s")
      seqCompSubsystem: Subsystem,
      @HelpMessage("optional argument: name for sequence component, ex: primary, backup etc")
      @Short("n")
      name: Option[String],
      @HelpMessage("optional argument: agentPrefix on which sequence component will be spawned, ex: ESW.agent1, IRIS.agent2 etc")
      @Short("a")
      agentPrefix: Option[Prefix],
      @HelpMessage("optional argument: subsystem of sequencer script, ex: tcs, iris etc. Default value: subsystem provided")
      @Short("i")
      seqSubsystem: Option[Subsystem],
      @HelpMessage("component name = obsmode ex: IRIS_ImagerAndIFS , IRIS_ImagerOnly ")
      @Short("m")
      obsMode: ObsMode,
      @HelpMessage(
        "optional variation part of the sequencer prefix ex: Sequencer Prefix(IRIS.IRIS_ImagerAndIFS.IRIS_IFS), IRIS_IFS needs to be provided as variation"
      )
      @Short("v")
      variation: Option[Variation],
      @HelpMessage("simulation mode")
      @Short("simulation")
      simulation: Boolean = false
  ) extends SequencerAppCommand

}
