package esw.sm.api.client

import csw.location.api.models.ComponentId
import csw.location.api.models.ComponentType.{Machine, SequenceComponent, Sequencer}
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

    "return cleanup success for cleanup request" in {
      val cleanupMsg = Cleanup(obsMode)
      when(
        postClient.requestResponse[CleanupResponse](argsEq(cleanupMsg))(
          any[Decoder[CleanupResponse]](),
          any[Encoder[CleanupResponse]]()
        )
      ).thenReturn(Future.successful(CleanupResponse.Success))

      client.cleanup(obsMode).futureValue shouldBe CleanupResponse.Success
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
      val shutdownSequencerMsg = ShutdownSequencer(ESW, obsMode)
      when(
        postClient.requestResponse[ShutdownSequencerResponse](argsEq(shutdownSequencerMsg))(
          any[Decoder[ShutdownSequencerResponse]](),
          any[Encoder[ShutdownSequencerResponse]]()
        )
      ).thenReturn(Future.successful(ShutdownSequencerResponse.Success))

      client.shutdownSequencer(ESW, obsMode).futureValue shouldBe ShutdownSequencerResponse.Success
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
        postClient.requestResponse[ShutdownAllSequencersResponse](argsEq(ShutdownAllSequencers))(
          any[Decoder[ShutdownAllSequencersResponse]](),
          any[Encoder[ShutdownAllSequencersResponse]]()
        )
      ).thenReturn(Future.successful(ShutdownAllSequencersResponse.Success))

      client.shutdownAllSequencers().futureValue shouldBe ShutdownAllSequencersResponse.Success
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
      val machine: ComponentId = ComponentId(Prefix(TCS, "primary"), Machine)
      val seqComp: ComponentId = ComponentId(Prefix(TCS, seqCompName), SequenceComponent)

      when(
        postClient.requestResponse[SpawnSequenceComponentResponse](argsEq(SpawnSequenceComponent(machine, seqCompName)))(
          any[Decoder[SpawnSequenceComponentResponse]](),
          any[Encoder[SpawnSequenceComponentResponse]]()
        )
      ).thenReturn(Future.successful(SpawnSequenceComponentResponse.Success(seqComp)))

      client.spawnSequenceComponent(machine, seqCompName).futureValue shouldBe SpawnSequenceComponentResponse.Success(seqComp)
    }
  }
}
