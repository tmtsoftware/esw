package esw.ocs.impl.core

import java.net.URI

import akka.Done
import akka.actor.testkit.typed.scaladsl.TestProbe
import akka.actor.typed.SpawnProtocol.Spawn
import akka.actor.typed._
import akka.actor.typed.scaladsl.AskPattern._
import akka.util.Timeout
import csw.location.api.extensions.ActorExtension.RichActor
import csw.location.api.models.ComponentType.SequenceComponent
import csw.location.api.models.Connection.AkkaConnection
import csw.location.api.models.{AkkaLocation, ComponentId, ComponentType, Location}
import csw.location.api.scaladsl.LocationService
import csw.logging.client.scaladsl.LoggerFactory
import csw.prefix.models.Prefix
import csw.prefix.models.Subsystem.{ESW, IRIS, TCS}
import esw.commons.BaseTestSuite
import esw.ocs.api.actor.messages.SequenceComponentMsg
import esw.ocs.api.actor.messages.SequenceComponentMsg._
import esw.ocs.api.protocol.ScriptError.LoadingScriptFailed
import esw.ocs.api.protocol.{GetStatusResponse, ScriptError, ScriptResponse}
import esw.ocs.impl.internal.{SequencerServer, SequencerServerFactory}

import scala.concurrent.Future
import scala.concurrent.duration.DurationLong

class SequenceComponentBehaviorTest extends BaseTestSuite {
  private implicit val system: ActorSystem[SpawnProtocol.Command] = ActorSystem(SpawnProtocol(), "sequencer-test-system")
  private implicit val timeout: Timeout                           = 10.seconds
  private val ocsSequenceComponentName                            = "ESW.ESW_1"
  private val sequenceComponentPrefix                             = Prefix(ocsSequenceComponentName)
  private val factory                                             = new LoggerFactory(Prefix("csw.SequenceComponentTest"))
  private val locationService                                     = mock[LocationService]
  private val sequencerServerFactory: SequencerServerFactory      = mock[SequencerServerFactory]
  val sequencerServer: SequencerServer                            = mock[SequencerServer]

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(locationService, sequencerServer, sequencerServerFactory)
  }

  "SequenceComponentBehavior" must {
    "load/unload script and get appropriate status | ESW-103, ESW-255" in {
      val (sequenceComponentRef, seqCompLocation) = spawnSequenceComponent()

      val loadScriptResponseProbe = TestProbe[ScriptResponse]()
      val getStatusProbe          = TestProbe[GetStatusResponse]()
      val subsystem               = ESW
      val observingMode           = "darknight"
      val prefix                  = Prefix(s"$subsystem.$observingMode")
      val akkaConnection          = AkkaConnection(ComponentId(prefix, ComponentType.Sequencer))

      when(sequencerServerFactory.make(subsystem, observingMode, seqCompLocation)).thenReturn(sequencerServer)
      when(sequencerServer.start()).thenReturn(Right(AkkaLocation(akkaConnection, URI.create("new_uri"))))

      //LoadScript
      sequenceComponentRef ! LoadScript(subsystem, observingMode, loadScriptResponseProbe.ref)

      //Assert if script loaded and returns AkkaLocation of sequencer
      val loadScriptLocationResponse: AkkaLocation = loadScriptResponseProbe.receiveMessage().response.rightValue

      loadScriptLocationResponse.connection shouldEqual akkaConnection

      verify(sequencerServerFactory).make(subsystem, observingMode, seqCompLocation)
      verify(sequencerServer).start()

      //GetStatus
      sequenceComponentRef ! GetStatus(getStatusProbe.ref)

      //Assert if get status returns AkkaLocation of sequencer currently running
      val getStatusLocationResponse: Location = getStatusProbe.receiveMessage(5.seconds).response.get
      getStatusLocationResponse.connection shouldEqual AkkaConnection(
        ComponentId(prefix, ComponentType.Sequencer)
      )

      //UnloadScript
      val unloadScriptProbe = TestProbe[Done]()
      sequenceComponentRef ! UnloadScript(unloadScriptProbe.ref)

      unloadScriptProbe.expectMessageType[Done.type]

      //assert if GetStatus returns None after unloading sequencer script
      sequenceComponentRef ! GetStatus(getStatusProbe.ref)
      getStatusProbe.expectMessage(GetStatusResponse(None))
    }

    "load script and give ScriptError if sequencer is already running | ESW-103" in {
      val (sequenceComponentRef, seqCompLocation) = spawnSequenceComponent()

      val loadScriptResponseProbe = TestProbe[ScriptResponse]()
      val subsystem               = IRIS
      val observingMode           = "darknight"
      val prefix                  = Prefix(s"$subsystem.$observingMode")
      val akkaConnection          = AkkaConnection(ComponentId(prefix, ComponentType.Sequencer))

      when(sequencerServerFactory.make(subsystem, observingMode, seqCompLocation)).thenReturn(sequencerServer)
      when(sequencerServer.start()).thenReturn(Right(AkkaLocation(akkaConnection, URI.create("new_uri"))))

      //LoadScript
      sequenceComponentRef ! LoadScript(subsystem, observingMode, loadScriptResponseProbe.ref)

      //Assert if script loaded and returns AkkaLocation of sequencer
      val loadScriptLocationResponse: AkkaLocation = loadScriptResponseProbe.receiveMessage().response.rightValue
      loadScriptLocationResponse.connection shouldEqual akkaConnection

      sequenceComponentRef ! LoadScript(TCS, "darknight", loadScriptResponseProbe.ref)
      loadScriptResponseProbe.receiveMessage().response.leftValue shouldBe ScriptError.SequenceComponentNotIdle(
        Prefix(subsystem, observingMode)
      )

      // verify that these calls are made exactly once as second time load script will return SequenceComponentNotIdle error
      verify(sequencerServerFactory).make(subsystem, observingMode, seqCompLocation)
      verify(sequencerServer).start()
    }

    "load script and give ScriptError if exception on initialization | ESW-243" in {
      val (sequenceComponentRef, seqCompLocation) = spawnSequenceComponent()

      val loadScriptResponseProbe = TestProbe[ScriptResponse]()
      val subsystem               = ESW
      val observingMode           = "initException"
      val loadingScriptFailed     = LoadingScriptFailed("Script initialization failed with : initialisation failed")

      when(sequencerServerFactory.make(subsystem, observingMode, seqCompLocation)).thenReturn(sequencerServer)
      when(sequencerServer.start()).thenReturn(Left(loadingScriptFailed))

      //LoadScript
      sequenceComponentRef ! LoadScript(subsystem, observingMode, loadScriptResponseProbe.ref)

      loadScriptResponseProbe.receiveMessage().response.leftValue shouldBe loadingScriptFailed
    }

    "unload script and return Done if sequence component is not running any sequencer | ESW-103" in {
      val (sequenceComponentRef, _) = spawnSequenceComponent()

      val unloadScriptResponseProbe = TestProbe[Done]()
      val getStatusProbe            = TestProbe[GetStatusResponse]()

      //assert if GetStatus returns None after unloading sequencer script
      sequenceComponentRef ! GetStatus(getStatusProbe.ref)
      getStatusProbe.expectMessage(GetStatusResponse(None))

      //UnloadScript
      sequenceComponentRef ! UnloadScript(unloadScriptResponseProbe.ref)

      //Assert if UnloadScript returns Done
      unloadScriptResponseProbe.expectMessage(Done)
    }

    "restart sequencer if sequence component is in running state (sequencer can be in any state) | ESW-141" in {
      val (sequenceComponentRef, seqCompLocation) = spawnSequenceComponent()

      val subsystem               = ESW
      val observingMode           = "darknight"
      val prefix                  = Prefix(s"$subsystem.$observingMode")
      val loadScriptResponseProbe = TestProbe[ScriptResponse]()
      val restartResponseProbe    = TestProbe[ScriptResponse]()
      val akkaConnection          = AkkaConnection(ComponentId(prefix, ComponentType.Sequencer))

      when(sequencerServerFactory.make(subsystem, observingMode, seqCompLocation)).thenReturn(sequencerServer)
      when(sequencerServer.start()).thenReturn(
        Right(AkkaLocation(akkaConnection, URI.create("first_load_uri"))),
        Right(AkkaLocation(akkaConnection, URI.create("after_restart_uri")))
      )

      //Assert if script loaded and returns AkkaLocation of sequencer
      sequenceComponentRef ! LoadScript(subsystem, observingMode, loadScriptResponseProbe.ref)
      val message = loadScriptResponseProbe.receiveMessage()
      message shouldBe a[ScriptResponse]
      message.response.isRight shouldBe true
      val initialLocation = message.response.rightValue

      when(sequencerServer.shutDown()).thenReturn(Done)

      //Restart sequencer and assert if it returns new AkkaLocation of sequencer
      sequenceComponentRef ! Restart(restartResponseProbe.ref)

      val restartLocationResponse: AkkaLocation = restartResponseProbe.receiveMessage().response.rightValue
      restartLocationResponse.connection shouldEqual AkkaConnection(
        ComponentId(prefix, ComponentType.Sequencer)
      )
      restartLocationResponse should not equal initialLocation
    }

    "restart should fail if sequence component is in idle state | ESW-141" in {
      val (sequenceComponentRef, _) = spawnSequenceComponent()

      val restartResponseProbe = TestProbe[ScriptResponse]()
      sequenceComponentRef ! Restart(restartResponseProbe.ref)
      restartResponseProbe.expectMessage(ScriptResponse(Left(ScriptError.RestartNotSupportedInIdle)))
    }

    "shutdown itself on Shutdown message | ESW-329" in {
      val system: ActorSystem[SpawnProtocol.Command] = ActorSystem(SpawnProtocol(), "sequencer-shut-down-system")
      val (sequenceComponentRef, _)                  = spawnSequenceComponent()(system)

      when(locationService.unregister(AkkaConnection(ComponentId(sequenceComponentPrefix, SequenceComponent))))
        .thenReturn(Future.successful(Done))

      val shutdownResponseProbe = TestProbe[Done]()(system)
      sequenceComponentRef ! Shutdown(shutdownResponseProbe.ref)
      shutdownResponseProbe.expectMessage(Done)
      shutdownResponseProbe.expectTerminated(sequenceComponentRef)
    }
  }

  private def spawnSequenceComponent()(implicit
      actorSystem: ActorSystem[SpawnProtocol.Command]
  ): (ActorRef[SequenceComponentMsg], AkkaLocation) = {
    val sequenceComponentRef = (actorSystem ? { replyTo: ActorRef[ActorRef[SequenceComponentMsg]] =>
      Spawn(
        (new SequenceComponentBehavior(
          sequenceComponentPrefix,
          factory.getLogger,
          locationService,
          sequencerServerFactory
        )(actorSystem)).idle,
        ocsSequenceComponentName,
        Props.empty,
        replyTo
      )
    })(timeout, actorSystem.scheduler).futureValue

    val seqCompLocation =
      AkkaLocation(AkkaConnection(ComponentId(Prefix(ocsSequenceComponentName), SequenceComponent)), sequenceComponentRef.toURI)

    (sequenceComponentRef, seqCompLocation)
  }
}
