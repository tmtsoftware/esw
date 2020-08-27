package esw.sm.api.client

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
  private val obsMode               = ObsMode("IRIS_darknight")
  private val seqCompPrefix: Prefix = Prefix(ESW, "primary")

  val postClient: Transport[SequenceManagerRequest] = mock[Transport[SequenceManagerRequest]]
  val client                                        = new SequenceManagerClient(postClient)

  "SequenceManagerClient" must {
    "return running observation modes for getRunningObsModes request | ESW-362" in {
      val getRunningObsModesResponse = mock[GetRunningObsModesResponse]
      when(
        postClient.requestResponse[GetRunningObsModesResponse](argsEq(GetRunningObsModes))(
          any[Decoder[GetRunningObsModesResponse]](),
          any[Encoder[GetRunningObsModesResponse]]()
        )
      ).thenReturn(Future.successful(getRunningObsModesResponse))

      client.getRunningObsModes.futureValue shouldBe getRunningObsModesResponse
    }

    "return provision response for provision request | ESW-347, ESW-362" in {
      val provisionResponse = mock[ProvisionResponse]
      val provisionConfig   = ProvisionConfig(Prefix(ESW, "primary") -> 1)
      when(
        postClient.requestResponse[ProvisionResponse](argsEq(Provision(provisionConfig)))(
          any[Decoder[ProvisionResponse]](),
          any[Encoder[ProvisionResponse]]()
        )
      ).thenReturn(Future.successful(provisionResponse))

      client.provision(provisionConfig).futureValue shouldBe provisionResponse
    }

    "return configure response for configure request | ESW-362" in {
      val configureResponse = mock[ConfigureResponse]
      when(
        postClient.requestResponse[ConfigureResponse](argsEq(Configure(obsMode)))(
          any[Decoder[ConfigureResponse]](),
          any[Encoder[ConfigureResponse]]()
        )
      ).thenReturn(Future.successful(configureResponse))

      client.configure(obsMode).futureValue shouldBe configureResponse
    }

    "return restart sequencer response for restartSequencer request | ESW-362" in {
      val restartSequencerResponse = mock[RestartSequencerResponse]
      val restartSequencerMsg      = RestartSequencer(ESW, obsMode)
      when(
        postClient.requestResponse[RestartSequencerResponse](argsEq(restartSequencerMsg))(
          any[Decoder[RestartSequencerResponse]](),
          any[Encoder[RestartSequencerResponse]]()
        )
      ).thenReturn(Future.successful(restartSequencerResponse))

      client.restartSequencer(ESW, obsMode).futureValue shouldBe restartSequencerResponse
    }

    "return ShutdownSequencersResponse for shutdownSequencer request | ESW-326, ESW-362" in {
      val shutdownSequencersResponse = mock[ShutdownSequencersResponse]
      val shutdownSequencerMsg       = ShutdownSequencer(ESW, obsMode)
      when(
        postClient.requestResponse[ShutdownSequencersResponse](argsEq(shutdownSequencerMsg))(
          any[Decoder[ShutdownSequencersResponse]](),
          any[Encoder[ShutdownSequencersResponse]]()
        )
      ).thenReturn(Future.successful(shutdownSequencersResponse))

      client.shutdownSequencer(ESW, obsMode).futureValue shouldBe shutdownSequencersResponse
    }

    "return ShutdownSequencersResponse for Shutdown Sequencers for Subsystem request | ESW-345, ESW-362" in {
      val shutdownSequencersResponse     = mock[ShutdownSequencersResponse]
      val shutdownSubsystemSequencersMsg = ShutdownSubsystemSequencers(ESW)
      when(
        postClient.requestResponse[ShutdownSequencersResponse](argsEq(shutdownSubsystemSequencersMsg))(
          any[Decoder[ShutdownSequencersResponse]](),
          any[Encoder[ShutdownSequencersResponse]]()
        )
      ).thenReturn(Future.successful(shutdownSequencersResponse))

      client.shutdownSubsystemSequencers(ESW).futureValue shouldBe shutdownSequencersResponse
    }

    "return ShutdownSequencersResponse for Shutdown Sequencers for ObsMode request | ESW-166, ESW-362" in {
      val shutdownSequencersResponse   = mock[ShutdownSequencersResponse]
      val shutdownObsModeSequencersMsg = ShutdownObsModeSequencers(obsMode)
      when(
        postClient.requestResponse[ShutdownSequencersResponse](argsEq(shutdownObsModeSequencersMsg))(
          any[Decoder[ShutdownSequencersResponse]](),
          any[Encoder[ShutdownSequencersResponse]]()
        )
      ).thenReturn(Future.successful(shutdownSequencersResponse))

      client.shutdownObsModeSequencers(obsMode).futureValue shouldBe shutdownSequencersResponse
    }

    "return ShutdownSequencersResponse for ShutdownAllSequencers request | ESW-324, ESW-362" in {
      val shutdownSequencersResponse = mock[ShutdownSequencersResponse]
      when(
        postClient.requestResponse[ShutdownSequencersResponse](
          argsEq(ShutdownAllSequencers)
        )(
          any[Decoder[ShutdownSequencersResponse]](),
          any[Encoder[ShutdownSequencersResponse]]()
        )
      ).thenReturn(Future.successful(shutdownSequencersResponse))

      client.shutdownAllSequencers().futureValue shouldBe shutdownSequencersResponse
    }

    "return StartSequencerResponse for startSequencer request | ESW-362" in {
      val startSequencerResponse = mock[StartSequencerResponse]
      val startSequencerMsg      = StartSequencer(ESW, obsMode)
      when(
        postClient.requestResponse[StartSequencerResponse](argsEq(startSequencerMsg))(
          any[Decoder[StartSequencerResponse]](),
          any[Encoder[StartSequencerResponse]]()
        )
      ).thenReturn(Future.successful(startSequencerResponse))

      client.startSequencer(ESW, obsMode).futureValue shouldBe startSequencerResponse
    }

    "return ShutdownSequenceComponentResponse for shutdown sequence component request | ESW-338, ESW-362" in {
      val shutdownSequenceComponentResponse = mock[ShutdownSequenceComponentResponse]
      when(
        postClient.requestResponse[ShutdownSequenceComponentResponse](argsEq(ShutdownSequenceComponent(seqCompPrefix)))(
          any[Decoder[ShutdownSequenceComponentResponse]](),
          any[Encoder[ShutdownSequenceComponentResponse]]()
        )
      ).thenReturn(Future.successful(shutdownSequenceComponentResponse))

      client.shutdownSequenceComponent(seqCompPrefix).futureValue shouldBe shutdownSequenceComponentResponse
    }

    "return ShutdownSequenceComponentResponse for shutdown all sequence component request | ESW-346, ESW-362" in {
      val shutdownSequenceComponentResponse = mock[ShutdownSequenceComponentResponse]
      when(
        postClient.requestResponse[ShutdownSequenceComponentResponse](argsEq(ShutdownAllSequenceComponents))(
          any[Decoder[ShutdownSequenceComponentResponse]](),
          any[Encoder[ShutdownSequenceComponentResponse]]()
        )
      ).thenReturn(Future.successful(shutdownSequenceComponentResponse))

      client.shutdownAllSequenceComponents().futureValue shouldBe shutdownSequenceComponentResponse
    }

  }
}
