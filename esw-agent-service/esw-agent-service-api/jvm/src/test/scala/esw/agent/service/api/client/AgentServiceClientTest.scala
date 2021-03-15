package esw.agent.service.api.client

import csw.location.api.models.ComponentId
import csw.location.api.models.ComponentType.SequenceComponent
import csw.prefix.models.Prefix
import csw.prefix.models.Subsystem.ESW
import esw.agent.service.api.codecs.AgentServiceCodecs
import esw.agent.service.api.models.{KillResponse, SpawnContainersResponse, SpawnResponse}
import esw.agent.service.api.protocol.AgentServiceRequest
import esw.agent.service.api.protocol.AgentServiceRequest.{
  KillComponent,
  SpawnContainers,
  SpawnSequenceComponent,
  SpawnSequenceManager
}
import esw.testcommons.BaseTestSuite
import io.bullet.borer.{Decoder, Encoder}
import msocket.api.Transport
import org.mockito.ArgumentMatchers.{any, eq => argEq}

import java.nio.file.Path
import scala.concurrent.Future

class AgentServiceClientTest extends BaseTestSuite with AgentServiceCodecs {

  val postClient: Transport[AgentServiceRequest] = mock[Transport[AgentServiceRequest]]
  val agentServiceClient                         = new AgentServiceClient(postClient)
  private val agentPrefix: Prefix                = Prefix(ESW, "primary")

  "AgentServiceClient" must {
    "return SpawnResponse for spawnSequenceManager request" in {
      val obsConfigPath        = mock[Path]
      val spawnResponse        = mock[SpawnResponse]
      val spawnSequenceManager = SpawnSequenceManager(agentPrefix, obsConfigPath, isConfigLocal = true, None)

      when(
        postClient.requestResponse[SpawnResponse](argEq(spawnSequenceManager))(
          any[Decoder[SpawnResponse]](),
          any[Encoder[SpawnResponse]]()
        )
      ).thenReturn(Future.successful(spawnResponse))

      agentServiceClient
        .spawnSequenceManager(agentPrefix, obsConfigPath, isConfigLocal = true, None)
        .futureValue shouldBe spawnResponse
    }

    "return SpawnResponse for spawnSequenceComponent request" in {
      val seqComponentName       = "TCS_1̌"
      val spawnSequenceComponent = SpawnSequenceComponent(agentPrefix, seqComponentName, None)
      val spawnResponse          = mock[SpawnResponse]

      when(
        postClient.requestResponse[SpawnResponse](argEq(spawnSequenceComponent))(
          any[Decoder[SpawnResponse]](),
          any[Encoder[SpawnResponse]]()
        )
      ).thenReturn(Future.successful(spawnResponse))

      agentServiceClient.spawnSequenceComponent(agentPrefix, seqComponentName, None).futureValue shouldBe spawnResponse
    }

    "return SpawnContainersResponse for spawnContainers request" in {
      val hostConfigPath          = randomString(5)
      val isConfigLocal           = randomBool
      val spawnContainers         = SpawnContainers(agentPrefix, hostConfigPath, isConfigLocal)
      val spawnContainersResponse = mock[SpawnContainersResponse]

      when(
        postClient.requestResponse[SpawnContainersResponse](argEq(spawnContainers))(
          any[Decoder[SpawnContainersResponse]](),
          any[Encoder[SpawnContainersResponse]]()
        )
      ).thenReturn(Future.successful(spawnContainersResponse))

      agentServiceClient.spawnContainers(agentPrefix, hostConfigPath, isConfigLocal).futureValue shouldBe spawnContainersResponse
    }

    "return KillResponse for killComponent request" in {
      val seqCompPrefix = Prefix(ESW, "TCS_1")
      val componentId   = ComponentId(seqCompPrefix, SequenceComponent)

      val killComponentRequest = KillComponent(componentId)
      val killResponse         = mock[KillResponse]

      when(
        postClient.requestResponse[KillResponse](argEq(killComponentRequest))(
          any[Decoder[KillResponse]](),
          any[Encoder[KillResponse]]()
        )
      ).thenReturn(Future.successful(killResponse))

      agentServiceClient.killComponent(componentId).futureValue shouldBe killResponse
    }
  }

}
