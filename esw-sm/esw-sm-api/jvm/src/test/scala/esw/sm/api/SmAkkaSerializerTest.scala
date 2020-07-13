package esw.sm.api

import akka.actor.testkit.typed.scaladsl.TestProbe
import akka.actor.typed.{ActorSystem, SpawnProtocol}
import akka.serialization.SerializationExtension
import csw.location.api.models.ComponentId
import csw.location.api.models.ComponentType.{SequenceComponent, Sequencer}
import csw.prefix.models.Prefix
import csw.prefix.models.Subsystem.ESW
import esw.ocs.api.models.ObsMode
import esw.sm.api.actor.messages.SequenceManagerMsg._
import esw.sm.api.protocol.CommonFailure.{ConfigurationMissing, LocationServiceError}
import esw.sm.api.protocol.ShutdownSequenceComponentsPolicy.{AllSequenceComponents, SingleSequenceComponent}
import esw.sm.api.protocol.SpawnSequenceComponentResponse.SpawnSequenceComponentFailed
import esw.sm.api.protocol.StartSequencerResponse.LoadScriptError
import esw.sm.api.protocol._
import esw.testcommons.BaseTestSuite
import org.scalatest.prop.TableDrivenPropertyChecks.forAll
import org.scalatest.prop.Tables.Table

import scala.concurrent.Await
import scala.concurrent.duration.DurationLong

class SmAkkaSerializerTest extends BaseTestSuite {
  private final implicit val system: ActorSystem[SpawnProtocol.Command] = ActorSystem(SpawnProtocol(), "SmAkkaSerializerTest")
  private final val serialization                                       = SerializationExtension(system)

  override protected def afterAll(): Unit = {
    system.terminate()
    Await.result(system.whenTerminated, 2.seconds)
  }

  "should use sm serializer for SequenceManagerRemoteMsg (de)serialization" in {
    val configureResponseRef                 = TestProbe[ConfigureResponse]().ref
    val getRunningModesResponseRef           = TestProbe[GetRunningObsModesResponse]().ref
    val shutdownSequencersResponseRef        = TestProbe[ShutdownSequencersResponse]().ref
    val getSmStateRef                        = TestProbe[SequenceManagerState]().ref
    val startSequencerResponseRef            = TestProbe[StartSequencerResponse]().ref
    val restartSequencerResponseRef          = TestProbe[RestartSequencerResponse]().ref
    val spawnSequenceComponentResponseRef    = TestProbe[SpawnSequenceComponentResponse]().ref
    val shutdownSequenceComponentResponseRef = TestProbe[ShutdownSequenceComponentResponse]().ref

    val obsMode = ObsMode("IRIS_Darknight")
    val agent   = Prefix(ESW, "agent1")

    val testData = Table(
      "SequenceManagerRemoteMsg models",
      Configure(obsMode, configureResponseRef),
      GetRunningObsModes(getRunningModesResponseRef),
      GetSequenceManagerState(getSmStateRef),
      StartSequencer(ESW, obsMode, startSequencerResponseRef),
      RestartSequencer(ESW, obsMode, restartSequencerResponseRef),
      ShutdownSequencers(ShutdownSequencersPolicy.SingleSequencer(ESW, obsMode), shutdownSequencersResponseRef),
      ShutdownSequencers(ShutdownSequencersPolicy.SubsystemSequencers(ESW), shutdownSequencersResponseRef),
      ShutdownSequencers(ShutdownSequencersPolicy.ObsModeSequencers(obsMode), shutdownSequencersResponseRef),
      ShutdownSequencers(ShutdownSequencersPolicy.AllSequencers, shutdownSequencersResponseRef),
      SpawnSequenceComponent(agent, "seq_comp", spawnSequenceComponentResponseRef),
      ShutdownSequenceComponents(SingleSequenceComponent(Prefix(ESW, "primary")), shutdownSequenceComponentResponseRef),
      ShutdownSequenceComponents(AllSequenceComponents, shutdownSequenceComponentResponseRef)
    )

    forAll(testData) { sequenceManagerRemoteMsg =>
      val serializer = serialization.findSerializerFor(sequenceManagerRemoteMsg)
      serializer.getClass shouldBe classOf[SmAkkaSerializer]

      val bytes = serializer.toBinary(sequenceManagerRemoteMsg)
      serializer.fromBinary(bytes, Some(sequenceManagerRemoteMsg.getClass)) shouldEqual sequenceManagerRemoteMsg
    }
  }

  "should use sm serializer for ConfigureResponse (de)serialization" in {
    val obsMode1 = ObsMode("IRIS_Darknight")
    val obsMode2 = ObsMode("IRIS_ClearSkies")

    val testData = Table(
      "Sequence Manager ConfigureResponse models",
      ConfigureResponse.Success(ComponentId(Prefix(ESW, "primary"), Sequencer)),
      ConfigureResponse.FailedToStartSequencers(Set("Error1", "Error2")),
      ConfigureResponse.ConflictingResourcesWithRunningObsMode(Set(obsMode1, obsMode2)),
      LocationServiceError("error"),
      ConfigurationMissing(obsMode1)
    )

    forAll(testData) { configureResponse =>
      val serializer = serialization.findSerializerFor(configureResponse)
      serializer.getClass shouldBe classOf[SmAkkaSerializer]

      val bytes = serializer.toBinary(configureResponse)
      serializer.fromBinary(bytes, Some(configureResponse.getClass)) shouldEqual configureResponse
    }
  }

  "should use sm serializer for GetRunningObsModesResponse (de)serialization" in {
    val obsMode1 = ObsMode("IRIS_Darknight")
    val obsMode2 = ObsMode("IRIS_ClearSkies")

    val testData = Table(
      "Sequence Manager GetRunningObsModesResponse models",
      GetRunningObsModesResponse.Success(Set(obsMode1, obsMode2)),
      GetRunningObsModesResponse.Failed("error")
    )

    forAll(testData) { getRunningObsModesResponse =>
      val serializer = serialization.findSerializerFor(getRunningObsModesResponse)
      serializer.getClass shouldBe classOf[SmAkkaSerializer]

      val bytes = serializer.toBinary(getRunningObsModesResponse)
      serializer.fromBinary(bytes, Some(getRunningObsModesResponse.getClass)) shouldEqual getRunningObsModesResponse
    }
  }

  "should use sm serializer for StartSequencerResponse (de)serialization" in {
    val componentId = ComponentId(Prefix("IRIS.darknight"), Sequencer)
    val testData = Table(
      "Sequence Manager StartSequencerResponse models",
      StartSequencerResponse.Started(componentId),
      StartSequencerResponse.AlreadyRunning(componentId),
      LoadScriptError("error"),
      LocationServiceError("error"),
      SpawnSequenceComponentFailed("error")
    )

    forAll(testData) { startSequencerResponse =>
      val serializer = serialization.findSerializerFor(startSequencerResponse)
      serializer.getClass shouldBe classOf[SmAkkaSerializer]

      val bytes = serializer.toBinary(startSequencerResponse)
      serializer.fromBinary(bytes, Some(startSequencerResponse.getClass)) shouldEqual startSequencerResponse
    }
  }

  "should use sm serializer for ShutdownSequencersResponse (de)serialization | ESW-324, ESW-166, ESW-345, ESW-326" in {
    val testData = Table(
      "Sequence Manager ShutdownSequencersResponse models",
      ShutdownSequencersResponse.Success,
      LocationServiceError("error")
    )

    forAll(testData) { shutdownSequencersResponse =>
      val serializer = serialization.findSerializerFor(shutdownSequencersResponse)
      serializer.getClass shouldBe classOf[SmAkkaSerializer]

      val bytes = serializer.toBinary(shutdownSequencersResponse)
      serializer.fromBinary(bytes, Some(shutdownSequencersResponse.getClass)) shouldEqual shutdownSequencersResponse
    }
  }

  "should use sm serializer for SpawnSequenceComponentResponse (de)serialization" in {
    val seqComp = ComponentId(Prefix("IRIS.seq_comp"), SequenceComponent)
    val testData = Table(
      "Sequence Manager SpawnSequenceComponentResponse models",
      SpawnSequenceComponentResponse.Success(seqComp),
      LocationServiceError("error"),
      SpawnSequenceComponentFailed("error")
    )

    forAll(testData) { spawnSequenceComponentResponse =>
      val serializer = serialization.findSerializerFor(spawnSequenceComponentResponse)
      serializer.getClass shouldBe classOf[SmAkkaSerializer]

      val bytes = serializer.toBinary(spawnSequenceComponentResponse)
      serializer.fromBinary(bytes, Some(spawnSequenceComponentResponse.getClass)) shouldEqual spawnSequenceComponentResponse
    }
  }

  "should use sm serializer for ShutdownSequenceComponentResponse (de)serialization" in {
    val testData = Table(
      "Sequence Manager ShutdownSequenceComponentResponse models",
      ShutdownSequenceComponentResponse.Success,
      LocationServiceError("error")
    )

    forAll(testData) { shutdownSequencerResponse =>
      val serializer = serialization.findSerializerFor(shutdownSequencerResponse)
      serializer.getClass shouldBe classOf[SmAkkaSerializer]

      val bytes = serializer.toBinary(shutdownSequencerResponse)
      serializer.fromBinary(bytes, Some(shutdownSequencerResponse.getClass)) shouldEqual shutdownSequencerResponse
    }
  }

  "should use sm serializer for SequenceManagerState (de)serialization" in {
    val testData = Table(
      "Sequence Manager SequenceManagerState models",
      SequenceManagerState.Idle,
      SequenceManagerState.Processing
    )

    forAll(testData) { sequenceManagerState =>
      val serializer = serialization.findSerializerFor(sequenceManagerState)
      serializer.getClass shouldBe classOf[SmAkkaSerializer]

      val bytes = serializer.toBinary(sequenceManagerState)
      serializer.fromBinary(bytes, Some(sequenceManagerState.getClass)) shouldEqual sequenceManagerState
    }
  }
}
