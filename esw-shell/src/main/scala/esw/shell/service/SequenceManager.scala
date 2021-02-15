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

  def service: SequenceManagerApi =
    new SequenceManagerImpl(locationUtils.findAkkaLocation(SequenceManagerPrefix, Service).map(_.throwLeft).await())

  def provision(config: ProvisionConfig, sequencerScriptsVersion: String): ProvisionResponse = {
    val sm = service
    val seqScriptsVersion =
      s"""
         |scripts {
         |  version = $sequencerScriptsVersion
         |}""".stripMargin

    val configData        = ConfigData.fromString(seqScriptsVersion)
    val versionConfigPath = Paths.get(SequencerScriptVersionConfigPath)
    configServiceExt.saveConfig(versionConfigPath, configData)
    sm.provision(config).await(SequenceManagerTimeouts.Provision)
  }
}
