package esw.sm.api.client

import csw.location.api.models.ComponentId
import csw.location.api.models.ComponentType.{SequenceComponent, Sequencer}
import csw.prefix.models.Prefix
import csw.prefix.models.Subsystem.{ESW, TCS}
import esw.ocs.api.models.ObsMode
import esw.sm.api.codecs.SequenceManagerHttpCodec
import esw.sm.api.protocol.SequenceManagerPostRequest.{GetRunningObsModes, _}
import esw.sm.api.protocol._
import esw.testcommons.BaseTestSuite
import io.bullet.borer.{Decoder, Encoder}
import msocket.api.Transport
import org.mockito.ArgumentMatchers.{any, eq => argsEq}

import scala.concurrent.Future

class SequenceManagerClientTest extends BaseTestSuite with SequenceManagerHttpCodec {
  private val obsMode                  = ObsMode("IRIS_darknight")
  private val componentId: ComponentId = ComponentId(Prefix(ESW, obsMode.name), Sequencer)
  private val seqCompPrefix: Prefix    = Prefix(ESW, "primary")

  val postClient: Transport[SequenceManagerPostRequest] = mock[Transport[SequenceManagerPostRequest]]
  val client                                            = new SequenceManagerClient(postClient)

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

    "return success for Shutdown Sequencers for ObsMode request" in {
      val shutdownObsModeSequencersMsg = ShutdownSequencers(ShutdownSequencersPolicy.ObsModeSequencers(obsMode))
      when(
        postClient.requestResponse[ShutdownSequencersResponse](argsEq(shutdownObsModeSequencersMsg))(
          any[Decoder[ShutdownSequencersResponse]](),
          any[Encoder[ShutdownSequencersResponse]]()
        )
      ).thenReturn(Future.successful(ShutdownSequencersResponse.Success))

      client.shutdownObsModeSequencers(obsMode).futureValue shouldBe ShutdownSequencersResponse.Success
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

    "return shutdown sequencer success for shutdownSequencer request" in {
      val shutdownSequencerMsg = ShutdownSequencers(ShutdownSequencersPolicy.SingleSequencer(ESW, obsMode))
      when(
        postClient.requestResponse[ShutdownSequencersResponse](argsEq(shutdownSequencerMsg))(
          any[Decoder[ShutdownSequencersResponse]](),
          any[Encoder[ShutdownSequencersResponse]]()
        )
      ).thenReturn(Future.successful(ShutdownSequencersResponse.Success))

      client.shutdownSequencer(ESW, obsMode).futureValue shouldBe ShutdownSequencersResponse.Success
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

    "return shutdown all sequencers success for  ShutdownAllSequencers request" in {
      when(
        postClient.requestResponse[ShutdownSequencersResponse](
          argsEq(ShutdownSequencers(ShutdownSequencersPolicy.AllSequencers))
        )(
          any[Decoder[ShutdownSequencersResponse]](),
          any[Encoder[ShutdownSequencersResponse]]()
        )
      ).thenReturn(Future.successful(ShutdownSequencersResponse.Success))

      client.shutdownAllSequencers().futureValue shouldBe ShutdownSequencersResponse.Success
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

    "return success response for Shutdown Sequence Component request" in {
      when(
        postClient.requestResponse[ShutdownSequenceComponentResponse](argsEq(ShutdownSequenceComponent(seqCompPrefix)))(
          any[Decoder[ShutdownSequenceComponentResponse]](),
          any[Encoder[ShutdownSequenceComponentResponse]]()
        )
      ).thenReturn(Future.successful(ShutdownSequenceComponentResponse.Success))

      client.shutdownSequenceComponent(seqCompPrefix).futureValue shouldBe ShutdownSequenceComponentResponse.Success
    }

    "return spawn sequence component success response for spawnSequenceComponent request | ESW-337" in {
      val seqCompName          = "seq_comp"
      val agent: Prefix        = Prefix(TCS, "primary")
      val seqComp: ComponentId = ComponentId(Prefix(TCS, seqCompName), SequenceComponent)

      when(
        postClient.requestResponse[SpawnSequenceComponentResponse](argsEq(SpawnSequenceComponent(agent, seqCompName)))(
          any[Decoder[SpawnSequenceComponentResponse]](),
          any[Encoder[SpawnSequenceComponentResponse]]()
        )
      ).thenReturn(Future.successful(SpawnSequenceComponentResponse.Success(seqComp)))

      client.spawnSequenceComponent(agent, seqCompName).futureValue shouldBe SpawnSequenceComponentResponse.Success(seqComp)
    }
  }
}
