package esw.shell.service

import akka.actor.typed.ActorSystem
import com.typesafe.config.ConfigFactory
import csw.config.api.ConfigData
import csw.config.api.exceptions.FileNotFound
import csw.config.api.scaladsl.ConfigService
import csw.location.api.models.ComponentType.Service
import esw.commons.extensions.EitherExt.EitherOps
import esw.commons.extensions.FutureExt.FutureOps
import esw.commons.utils.location.LocationServiceUtil
import esw.constants.SequenceManagerTimeouts
import esw.sm.api.SequenceManagerApi
import esw.sm.api.actor.client.SequenceManagerImpl
import esw.sm.api.models.ProvisionConfig
import esw.sm.api.protocol.ProvisionResponse

import java.nio.file.Paths
import scala.concurrent.ExecutionContext

class SequenceManager(val locationUtils: LocationServiceUtil, configService: ConfigService)(implicit
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
    saveSeqScriptsVersion(sequencerScriptsVersion)
    sm.provision(config).await(SequenceManagerTimeouts.Provision)
  }

  private def saveSeqScriptsVersion(version: String): Unit = {
    val versionConfigPath = Paths.get(SequencerScriptVersionConfigPath)
    val seqScriptsVersion =
      s"""
         |scripts {
         |  version = $version
         |}""".stripMargin
    try {
      val id = configService
        .update(
          versionConfigPath,
          ConfigData.fromString(seqScriptsVersion),
          "Update sequencer scripts version for test setup"
        )
        .await()
      configService.setActiveVersion(versionConfigPath, id, "setting updated version as active").await()
    }
    catch {
      case _: FileNotFound =>
        configService
          .create(
            versionConfigPath,
            ConfigData.fromString(seqScriptsVersion),
            annex = false,
            "Add sequencer scripts version for test setup"
          )
          .await()
    }
  }
}
