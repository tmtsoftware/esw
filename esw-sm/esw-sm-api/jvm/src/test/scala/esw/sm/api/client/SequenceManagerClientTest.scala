package esw.sm.api.client

import csw.location.api.models.ComponentId
import csw.location.api.models.ComponentType.Sequencer
import csw.prefix.models.Prefix
import csw.prefix.models.Subsystem.ESW
import esw.ocs.api.models.ObsMode
import esw.sm.api.codecs.SequenceManagerServiceCodecs
import esw.sm.api.models.ProvisionConfig
import esw.sm.api.protocol.SequenceManagerRequest.{GetRunningObsModes, _}
import esw.sm.api.protocol._
import esw.testcommons.BaseTestSuite
import io.bullet.borer.{Decoder, Encoder}
import msocket.api.Transport
import org.mockito.ArgumentMatchers.{any, eq => argsEq}

import scala.concurrent.Future

class SequenceManagerClientTest extends BaseTestSuite with SequenceManagerServiceCodecs {
  private val obsMode                  = ObsMode("IRIS_darknight")
  private val componentId: ComponentId = ComponentId(Prefix(ESW, obsMode.name), Sequencer)
  private val seqCompPrefix: Prefix    = Prefix(ESW, "primary")

  val postClient: Transport[SequenceManagerRequest] = mock[Transport[SequenceManagerRequest]]
  val client                                        = new SequenceManagerClient(postClient)

  "SequenceManagerClient" must {
    "return running observation modes for getRunningObsModes request" in {
      when(
        postClient.requestResponse[GetRunningObsModesResponse](argsEq(GetRunningObsModes))(
          any[Decoder[GetRunningObsModesResponse]](),
          any[Encoder[GetRunningObsModesResponse]]()
        )
      ).thenReturn(Future.successful(GetRunningObsModesResponse.Success(Set(obsMode))))

      client.getRunningObsModes.futureValue shouldBe GetRunningObsModesResponse.Success(Set(obsMode))
    }

    "return provision success for provision request | ESW-347" in {

      val provisionConfig = ProvisionConfig(Prefix(ESW, "primary") -> 1)
      when(
        postClient.requestResponse[ProvisionResponse](argsEq(Provision(provisionConfig)))(
          any[Decoder[ProvisionResponse]](),
          any[Encoder[ProvisionResponse]]()
        )
      ).thenReturn(Future.successful(ProvisionResponse.Success))

      client.provision(provisionConfig).futureValue shouldBe ProvisionResponse.Success
    }

    "return configure success response for configure request" in {
      when(
        postClient.requestResponse[ConfigureResponse](argsEq(Configure(obsMode)))(
          any[Decoder[ConfigureResponse]](),
          any[Encoder[ConfigureResponse]]()
        )
      ).thenReturn(Future.successful(ConfigureResponse.Success(componentId)))

      client.configure(obsMode).futureValue shouldBe ConfigureResponse.Success(componentId)
    }

    "return restart sequencer success for restartSequencer request" in {
      val restartSequencerMsg = RestartSequencer(ESW, obsMode)
      when(
        postClient.requestResponse[RestartSequencerResponse](argsEq(restartSequencerMsg))(
          any[Decoder[RestartSequencerResponse]](),
          any[Encoder[RestartSequencerResponse]]()
        )
      ).thenReturn(Future.successful(RestartSequencerResponse.Success(componentId)))

      client.restartSequencer(ESW, obsMode).futureValue shouldBe RestartSequencerResponse.Success(componentId)
    }

    "return success for Shutdown sequencer for shutdownSequencer request | ESW-326" in {
      val shutdownSequencerMsg = ShutdownSequencer(ESW, obsMode)
      when(
        postClient.requestResponse[ShutdownSequencersResponse](argsEq(shutdownSequencerMsg))(
          any[Decoder[ShutdownSequencersResponse]](),
          any[Encoder[ShutdownSequencersResponse]]()
        )
      ).thenReturn(Future.successful(ShutdownSequencersResponse.Success))

      client.shutdownSequencer(ESW, obsMode).futureValue shouldBe ShutdownSequencersResponse.Success
    }

    "return success for Shutdown Sequencers for Subsystem request | ESW-345" in {
      val shutdownSubsystemSequencersMsg = ShutdownSubsystemSequencers(ESW)
      when(
        postClient.requestResponse[ShutdownSequencersResponse](argsEq(shutdownSubsystemSequencersMsg))(
          any[Decoder[ShutdownSequencersResponse]](),
          any[Encoder[ShutdownSequencersResponse]]()
        )
      ).thenReturn(Future.successful(ShutdownSequencersResponse.Success))

      client.shutdownSubsystemSequencers(ESW).futureValue shouldBe ShutdownSequencersResponse.Success
    }

    "return success for Shutdown Sequencers for ObsMode request | ESW-166" in {
      val shutdownObsModeSequencersMsg = ShutdownObsModeSequencers(obsMode)
      when(
        postClient.requestResponse[ShutdownSequencersResponse](argsEq(shutdownObsModeSequencersMsg))(
          any[Decoder[ShutdownSequencersResponse]](),
          any[Encoder[ShutdownSequencersResponse]]()
        )
      ).thenReturn(Future.successful(ShutdownSequencersResponse.Success))

      client.shutdownObsModeSequencers(obsMode).futureValue shouldBe ShutdownSequencersResponse.Success
    }

    "return shutdown all sequencers success for ShutdownAllSequencers request | ESW-324" in {
      when(
        postClient.requestResponse[ShutdownSequencersResponse](
          argsEq(ShutdownAllSequencers)
        )(
          any[Decoder[ShutdownSequencersResponse]](),
          any[Encoder[ShutdownSequencersResponse]]()
        )
      ).thenReturn(Future.successful(ShutdownSequencersResponse.Success))

      client.shutdownAllSequencers().futureValue shouldBe ShutdownSequencersResponse.Success
    }

    "return start sequencer success for startSequencer request" in {
      val startSequencerMsg = StartSequencer(ESW, obsMode)
      when(
        postClient.requestResponse[StartSequencerResponse](argsEq(startSequencerMsg))(
          any[Decoder[StartSequencerResponse]](),
          any[Encoder[StartSequencerResponse]]()
        )
      ).thenReturn(Future.successful(StartSequencerResponse.Started(componentId)))

      client.startSequencer(ESW, obsMode).futureValue shouldBe StartSequencerResponse.Started(componentId)
    }

    "return success response for shutdown sequence component request | ESW-338" in {
      when(
        postClient.requestResponse[ShutdownSequenceComponentResponse](argsEq(ShutdownSequenceComponent(seqCompPrefix)))(
          any[Decoder[ShutdownSequenceComponentResponse]](),
          any[Encoder[ShutdownSequenceComponentResponse]]()
        )
      ).thenReturn(Future.successful(ShutdownSequenceComponentResponse.Success))

      client.shutdownSequenceComponent(seqCompPrefix).futureValue shouldBe ShutdownSequenceComponentResponse.Success
    }

    "return success response for shutdown sequence all component request | ESW-346" in {
      when(
        postClient.requestResponse[ShutdownSequenceComponentResponse](argsEq(ShutdownAllSequenceComponents))(
          any[Decoder[ShutdownSequenceComponentResponse]](),
          any[Encoder[ShutdownSequenceComponentResponse]]()
        )
      ).thenReturn(Future.successful(ShutdownSequenceComponentResponse.Success))

      client.shutdownAllSequenceComponents().futureValue shouldBe ShutdownSequenceComponentResponse.Success
    }

  }
}
