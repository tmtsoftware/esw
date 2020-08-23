package esw.agent.service.api.client

import java.nio.file.Path

import csw.location.api.models.ComponentId
import csw.location.api.models.ComponentType.SequenceComponent
import csw.location.api.models.Connection.AkkaConnection
import csw.prefix.models.Prefix
import csw.prefix.models.Subsystem.ESW
import esw.agent.service.api.codecs.AgentServiceCodecs
import esw.agent.service.api.models.{KillResponse, SpawnResponse}
import esw.agent.service.api.protocol.AgentServiceRequest
import esw.agent.service.api.protocol.AgentServiceRequest.{KillComponent, SpawnSequenceComponent, SpawnSequenceManager}
import io.bullet.borer.{Decoder, Encoder}
import msocket.api.Transport
import org.mockito.ArgumentMatchers.{any, eq => argEq}
import org.mockito.MockitoSugar.{mock, when}
import org.scalatest.concurrent.ScalaFutures.convertScalaFuture
import org.scalatest.matchers.must.Matchers
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper
import org.scalatest.wordspec.AnyWordSpec

import scala.concurrent.Future

class AgentServiceClientTest extends AnyWordSpec with Matchers with AgentServiceCodecs {

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

    "return KillResponse for killComponent request" in {
      val seqCompPrefix = Prefix(ESW, "TCS_1")
      val componentId   = ComponentId(seqCompPrefix, SequenceComponent)
      val connection    = AkkaConnection(componentId)

      val killComponentRequest = KillComponent(connection)
      val killResponse         = mock[KillResponse]

      when(
        postClient.requestResponse[KillResponse](argEq(killComponentRequest))(
          any[Decoder[KillResponse]](),
          any[Encoder[KillResponse]]()
        )
      ).thenReturn(Future.successful(killResponse))

      agentServiceClient.killComponent(connection).futureValue shouldBe killResponse
    }
  }

}
