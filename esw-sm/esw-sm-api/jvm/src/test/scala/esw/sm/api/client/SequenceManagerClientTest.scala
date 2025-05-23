package esw.sm.api.client

import csw.prefix.models.Prefix
import csw.prefix.models.Subsystem.ESW
import esw.ocs.api.models.{ObsMode, Variation}
import esw.sm.api.codecs.SequenceManagerServiceCodecs
import esw.sm.api.models.ProvisionConfig
import esw.sm.api.protocol.*
import esw.sm.api.protocol.SequenceManagerRequest.*
import esw.testcommons.BaseTestSuite
import io.bullet.borer.{Decoder, Encoder}
import msocket.api.Transport
import org.mockito.ArgumentMatchers.{any, eq => argsEq}
import org.mockito.Mockito.when

import scala.concurrent.Future

class SequenceManagerClientTest extends BaseTestSuite with SequenceManagerServiceCodecs {
  private val obsMode                               = ObsMode("IRIS_darknight")
  private val seqCompPrefix: Prefix                 = Prefix(ESW, "primary")
  val variation: Option[Variation]                  = Some(Variation("variation"))
  val postClient: Transport[SequenceManagerRequest] = mock[Transport[SequenceManagerRequest]]
  val client                                        = new SequenceManagerClient(postClient)

  "SequenceManagerClient" must {

    "return observation modes with status for obsModesDetails request | ESW-466" in {
      val obsModesDetailsResponse = mock[ObsModesDetailsResponse]
      when(
        postClient.requestResponse[ObsModesDetailsResponse](argsEq(GetObsModesDetails))(using
          any[Decoder[ObsModesDetailsResponse]](),
          any[Encoder[ObsModesDetailsResponse]]()
        )
      ).thenReturn(Future.successful(obsModesDetailsResponse))

      client.getObsModesDetails.futureValue shouldBe obsModesDetailsResponse
    }

    "return provision response for provision request | ESW-347, ESW-362" in {
      val provisionResponse = mock[ProvisionResponse]
      val provisionConfig   = ProvisionConfig(Prefix(ESW, "primary") -> 1)
      when(
        postClient.requestResponse[ProvisionResponse](argsEq(Provision(provisionConfig)))(using
          any[Decoder[ProvisionResponse]](),
          any[Encoder[ProvisionResponse]]()
        )
      ).thenReturn(Future.successful(provisionResponse))

      client.provision(provisionConfig).futureValue shouldBe provisionResponse
    }

    "return configure response for configure request | ESW-362" in {
      val configureResponse = mock[ConfigureResponse]
      when(
        postClient.requestResponse[ConfigureResponse](argsEq(Configure(obsMode)))(using
          any[Decoder[ConfigureResponse]](),
          any[Encoder[ConfigureResponse]]()
        )
      ).thenReturn(Future.successful(configureResponse))

      client.configure(obsMode).futureValue shouldBe configureResponse
    }

    "return restart sequencer response for restartSequencer request | ESW-362" in {
      val restartSequencerResponse = mock[RestartSequencerResponse]
      val restartSequencerMsg      = RestartSequencer(ESW, obsMode, None)
      when(
        postClient.requestResponse[RestartSequencerResponse](argsEq(restartSequencerMsg))(using
          any[Decoder[RestartSequencerResponse]](),
          any[Encoder[RestartSequencerResponse]]()
        )
      ).thenReturn(Future.successful(restartSequencerResponse))

      client.restartSequencer(ESW, obsMode, None).futureValue shouldBe restartSequencerResponse
    }

    "return restart sequencer with variation response for restartSequencer request | ESW-362, ESW-561" in {
      val restartSequencerResponse = mock[RestartSequencerResponse]

      val restartSequencerMsg = RestartSequencer(ESW, obsMode, variation)
      when(
        postClient.requestResponse[RestartSequencerResponse](argsEq(restartSequencerMsg))(using
          any[Decoder[RestartSequencerResponse]](),
          any[Encoder[RestartSequencerResponse]]()
        )
      ).thenReturn(Future.successful(restartSequencerResponse))

      client.restartSequencer(ESW, obsMode, variation).futureValue shouldBe restartSequencerResponse
    }

    "return ShutdownSequencersResponse for shutdownSequencer request | ESW-326, ESW-362" in {
      val shutdownSequencersResponse = mock[ShutdownSequencersResponse]
      val shutdownSequencerMsg       = ShutdownSequencer(ESW, obsMode, None)
      when(
        postClient.requestResponse[ShutdownSequencersResponse](argsEq(shutdownSequencerMsg))(using
          any[Decoder[ShutdownSequencersResponse]](),
          any[Encoder[ShutdownSequencersResponse]]()
        )
      ).thenReturn(Future.successful(shutdownSequencersResponse))

      client.shutdownSequencer(ESW, obsMode, None).futureValue shouldBe shutdownSequencersResponse
    }

    "return ShutdownSequencersResponse for shutdownSequencer request with variation | ESW-326, ESW-362, ESW-561" in {
      val shutdownSequencersResponse = mock[ShutdownSequencersResponse]
      val shutdownSequencerMsg       = ShutdownSequencer(ESW, obsMode, variation)
      when(
        postClient.requestResponse[ShutdownSequencersResponse](argsEq(shutdownSequencerMsg))(using
          any[Decoder[ShutdownSequencersResponse]](),
          any[Encoder[ShutdownSequencersResponse]]()
        )
      ).thenReturn(Future.successful(shutdownSequencersResponse))

      client.shutdownSequencer(ESW, obsMode, variation).futureValue shouldBe shutdownSequencersResponse
    }
    "return ShutdownSequencersResponse for Shutdown Sequencers for Subsystem request | ESW-345, ESW-362" in {
      val shutdownSequencersResponse     = mock[ShutdownSequencersResponse]
      val shutdownSubsystemSequencersMsg = ShutdownSubsystemSequencers(ESW)
      when(
        postClient.requestResponse[ShutdownSequencersResponse](argsEq(shutdownSubsystemSequencersMsg))(using
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
        postClient.requestResponse[ShutdownSequencersResponse](argsEq(shutdownObsModeSequencersMsg))(using
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
        )(using
          any[Decoder[ShutdownSequencersResponse]](),
          any[Encoder[ShutdownSequencersResponse]]()
        )
      ).thenReturn(Future.successful(shutdownSequencersResponse))

      client.shutdownAllSequencers().futureValue shouldBe shutdownSequencersResponse
    }

    "return StartSequencerResponse for startSequencer request | ESW-362" in {
      val startSequencerResponse = mock[StartSequencerResponse]
      val startSequencerMsg      = StartSequencer(ESW, obsMode, None)
      when(
        postClient.requestResponse[StartSequencerResponse](argsEq(startSequencerMsg))(using
          any[Decoder[StartSequencerResponse]](),
          any[Encoder[StartSequencerResponse]]()
        )
      ).thenReturn(Future.successful(startSequencerResponse))

      client.startSequencer(ESW, obsMode, None).futureValue shouldBe startSequencerResponse
    }

    "return StartSequencerResponse for startSequencer request with variation | ESW-362, ESW-561" in {
      val startSequencerResponse = mock[StartSequencerResponse]
      val startSequencerMsg      = StartSequencer(ESW, obsMode, variation)
      when(
        postClient.requestResponse[StartSequencerResponse](argsEq(startSequencerMsg))(using
          any[Decoder[StartSequencerResponse]](),
          any[Encoder[StartSequencerResponse]]()
        )
      ).thenReturn(Future.successful(startSequencerResponse))

      client.startSequencer(ESW, obsMode, variation).futureValue shouldBe startSequencerResponse
    }

    "return ShutdownSequenceComponentResponse for shutdown sequence component request | ESW-338, ESW-362" in {
      val shutdownSequenceComponentResponse = mock[ShutdownSequenceComponentResponse]
      when(
        postClient.requestResponse[ShutdownSequenceComponentResponse](argsEq(ShutdownSequenceComponent(seqCompPrefix)))(using
          any[Decoder[ShutdownSequenceComponentResponse]](),
          any[Encoder[ShutdownSequenceComponentResponse]]()
        )
      ).thenReturn(Future.successful(shutdownSequenceComponentResponse))

      client.shutdownSequenceComponent(seqCompPrefix).futureValue shouldBe shutdownSequenceComponentResponse
    }

    "return ShutdownSequenceComponentResponse for shutdown all sequence component request | ESW-346, ESW-362" in {
      val shutdownSequenceComponentResponse = mock[ShutdownSequenceComponentResponse]
      when(
        postClient.requestResponse[ShutdownSequenceComponentResponse](argsEq(ShutdownAllSequenceComponents))(using
          any[Decoder[ShutdownSequenceComponentResponse]](),
          any[Encoder[ShutdownSequenceComponentResponse]]()
        )
      ).thenReturn(Future.successful(shutdownSequenceComponentResponse))

      client.shutdownAllSequenceComponents().futureValue shouldBe shutdownSequenceComponentResponse
    }

  }
}
