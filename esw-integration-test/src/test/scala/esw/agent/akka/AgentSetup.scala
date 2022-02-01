package esw.agent.akka

import java.nio.file.Path

import csw.location.api.codec.LocationServiceCodecs
import csw.location.api.models.ComponentId
import csw.location.api.models.ComponentType.SequenceComponent
import csw.location.api.models.Connection.AkkaConnection
import csw.prefix.models.Prefix
import csw.prefix.models.Subsystem.ESW
import esw.agent.akka.client.AgentClient
import esw.agent.service.api.models.SpawnResponse

import scala.concurrent.Future

trait AgentSetup extends LocationServiceCodecs {

  val channel: String                       = "file://" + getClass.getResource("/apps.json").getPath
  val versionConfPath: Path                 = Path.of("/tmt/configs/osw-version.conf")
  val irisPrefix: Prefix                    = Prefix("esw.iris")
  val irisSeqCompConnection: AkkaConnection = AkkaConnection(ComponentId(irisPrefix, SequenceComponent))
  val appVersion                            = "0.1.0-SNAPSHOT"
  val agentPrefix: Prefix                   = Prefix(ESW, "machine_A1")
  val eswVersion: Some[String]              = Some(appVersion)

  // ESW-325: spawns sequence component via agent using coursier with provided sha
  def spawnSequenceComponent(agentClient: AgentClient, componentName: String): Future[SpawnResponse] =
    agentClient.spawnSequenceComponent(componentName, eswVersion)
}
