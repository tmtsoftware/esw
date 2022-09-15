/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package esw.shell.service

import java.nio.file.Paths
import akka.actor.typed.ActorSystem
import com.typesafe.config.ConfigFactory
import csw.config.api.ConfigData
import csw.location.api.models.ComponentType.Service
import esw.commons.extensions.EitherExt.EitherOps
import esw.commons.extensions.FutureExt.FutureOps
import esw.commons.utils.config.ConfigServiceExt
import esw.commons.utils.location.LocationServiceUtil
import esw.constants.SequenceManagerTimeouts
import esw.http.core.wiring.HttpService
import esw.sm.api.SequenceManagerApi
import esw.sm.api.actor.client.SequenceManagerImpl
import esw.sm.api.models.ProvisionConfig
import esw.sm.api.protocol.ProvisionResponse

import scala.concurrent.ExecutionContext

class SequenceManager(val locationUtils: LocationServiceUtil, configServiceExt: ConfigServiceExt)(implicit
    val actorSystem: ActorSystem[_]
) {
  implicit lazy val ec: ExecutionContext = actorSystem.executionContext

  private lazy val config                                   = ConfigFactory.load().getConfig("agent")
  private lazy val SequencerScriptVersionConfigPath: String = config.getString("osw.version.confPath")
  private val SequenceManagerPrefix                         = "ESW.sequence_manager"

  // creates an akka client for the Sequence Manager
  def service: SequenceManagerApi =
    new SequenceManagerImpl(locationUtils.findAkkaLocation(SequenceManagerPrefix, Service).map(_.throwLeft).await())

  // does provision on SM with the given provision config and sequence script version
  def provision(config: ProvisionConfig, sequencerScriptsVersion: String): ProvisionResponse = {

    val eswVersion = Option(classOf[HttpService].getPackage.getSpecificationVersion).getOrElse("0.1.0-SNAPSHOT")

    val sm = service
    val seqScriptsVersion =
      s"""
         |scripts = $sequencerScriptsVersion
         |esw = $eswVersion
         |""".stripMargin

    val configData        = ConfigData.fromString(seqScriptsVersion)
    val versionConfigPath = Paths.get(SequencerScriptVersionConfigPath)
    configServiceExt.saveConfig(versionConfigPath, configData)
    sm.provision(config).await(SequenceManagerTimeouts.Provision)
  }
}
