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
import esw.ocs.api.actor.messages.SequenceComponentMsg
import esw.ocs.api.actor.messages.SequenceComponentMsg._
import esw.ocs.api.models.SequenceComponentState.{Idle, Running}
import esw.ocs.api.protocol.ScriptError.LoadingScriptFailed
import esw.ocs.api.protocol.SequenceComponentResponse._
import esw.ocs.impl.internal.{SequencerServer, SequencerServerFactory}
import esw.testcommons.BaseTestSuite

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

      val loadScriptResponseProbe = TestProbe[ScriptResponseOrUnhandled]()
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
      val scriptResponseOrUnhandled = loadScriptResponseProbe.receiveMessage()
      scriptResponseOrUnhandled shouldBe a[SequencerLocation]
      val loadScriptLocationResponse: AkkaLocation = scriptResponseOrUnhandled.asInstanceOf[SequencerLocation].location

      loadScriptLocationResponse.connection shouldEqual akkaConnection

      verify(sequencerServerFactory).make(subsystem, observingMode, seqCompLocation)
      verify(sequencerServer).start()

      //GetStatus
      sequenceComponentRef ! GetStatus(getStatusProbe.ref)

      //Assert if get status returns AkkaLocation of sequencer currently running
      val getStatusResponseOrUnhandled = getStatusProbe.receiveMessage(5.seconds)
      getStatusResponseOrUnhandled shouldBe a[GetStatusResponse]
      val getStatusLocationResponse: Location = getStatusResponseOrUnhandled.asInstanceOf[GetStatusResponse].response.get
      getStatusLocationResponse.connection shouldEqual AkkaConnection(
        ComponentId(prefix, ComponentType.Sequencer)
      )

      //UnloadScript
      val unloadScriptProbe = TestProbe[OkOrUnhandled]()
      sequenceComponentRef ! UnloadScript(unloadScriptProbe.ref)

      unloadScriptProbe.expectMessageType[Ok.type]

      //assert if GetStatus returns None after unloading sequencer script
      sequenceComponentRef ! GetStatus(getStatusProbe.ref)
      getStatusProbe.expectMessage(GetStatusResponse(None))
    }

    "load script and give ScriptError if sequencer is already running | ESW-103" in {
      val (sequenceComponentRef, seqCompLocation) = spawnSequenceComponent()

      val loadScriptResponseProbe = TestProbe[ScriptResponseOrUnhandled]()
      val subsystem               = IRIS
      val observingMode           = "darknight"
      val prefix                  = Prefix(s"$subsystem.$observingMode")
      val akkaConnection          = AkkaConnection(ComponentId(prefix, ComponentType.Sequencer))

      when(sequencerServerFactory.make(subsystem, observingMode, seqCompLocation)).thenReturn(sequencerServer)
      when(sequencerServer.start()).thenReturn(Right(AkkaLocation(akkaConnection, URI.create("new_uri"))))

      //LoadScript
      sequenceComponentRef ! LoadScript(subsystem, observingMode, loadScriptResponseProbe.ref)

      //Assert if script loaded and returns AkkaLocation of sequencer
      val response = loadScriptResponseProbe.receiveMessage()
      response shouldBe a[SequencerLocation]
      val loadScriptLocationResponse: AkkaLocation = response.asInstanceOf[SequencerLocation].location
      loadScriptLocationResponse.connection shouldEqual akkaConnection

      sequenceComponentRef ! LoadScript(TCS, "darknight", loadScriptResponseProbe.ref)
      val response1 = loadScriptResponseProbe.receiveMessage()
      response1 should ===(Unhandled(Running, "LoadScript"))

      // verify that these calls are made exactly once as second time load script will return SequenceComponentNotIdle error
      verify(sequencerServerFactory).make(subsystem, observingMode, seqCompLocation)
      verify(sequencerServer).start()
    }

    "load script and give ScriptError if exception on initialization | ESW-243" in {
      val (sequenceComponentRef, seqCompLocation) = spawnSequenceComponent()

      val loadScriptResponseProbe = TestProbe[ScriptResponseOrUnhandled]()
      val subsystem               = ESW
      val observingMode           = "initException"
      val loadingScriptFailed     = LoadingScriptFailed("Script initialization failed with : initialisation failed")

      when(sequencerServerFactory.make(subsystem, observingMode, seqCompLocation)).thenReturn(sequencerServer)
      when(sequencerServer.start()).thenReturn(Left(loadingScriptFailed))

      //LoadScript
      sequenceComponentRef ! LoadScript(subsystem, observingMode, loadScriptResponseProbe.ref)

      val response = loadScriptResponseProbe.receiveMessage()
      response shouldBe a[LoadingScriptFailed]
      response.asInstanceOf[LoadingScriptFailed] shouldBe loadingScriptFailed

    }

    "unload script and return Ok if sequence component is not running any sequencer | ESW-103" in {
      val (sequenceComponentRef, _) = spawnSequenceComponent()

      val unloadScriptResponseProbe = TestProbe[OkOrUnhandled]()
      val getStatusProbe            = TestProbe[GetStatusResponse]()

      //assert if GetStatus returns None after unloading sequencer script
      sequenceComponentRef ! GetStatus(getStatusProbe.ref)
      getStatusProbe.expectMessage(GetStatusResponse(None))

      //UnloadScript
      sequenceComponentRef ! UnloadScript(unloadScriptResponseProbe.ref)

      //Assert if UnloadScript returns Ok
      unloadScriptResponseProbe.expectMessage(Ok)
    }

    "restart sequencer if sequence component is in running state (sequencer can be in any state) | ESW-141" in {
      val (sequenceComponentRef, seqCompLocation) = spawnSequenceComponent()

      val subsystem               = ESW
      val observingMode           = "darknight"
      val prefix                  = Prefix(s"$subsystem.$observingMode")
      val loadScriptResponseProbe = TestProbe[ScriptResponseOrUnhandled]()
      val restartResponseProbe    = TestProbe[ScriptResponseOrUnhandled]()
      val akkaConnection          = AkkaConnection(ComponentId(prefix, ComponentType.Sequencer))

      when(sequencerServerFactory.make(subsystem, observingMode, seqCompLocation)).thenReturn(sequencerServer)
      when(sequencerServer.start()).thenReturn(
        Right(AkkaLocation(akkaConnection, URI.create("first_load_uri"))),
        Right(AkkaLocation(akkaConnection, URI.create("after_restart_uri")))
      )

      //Assert if script loaded and returns AkkaLocation of sequencer
      sequenceComponentRef ! LoadScript(subsystem, observingMode, loadScriptResponseProbe.ref)
      val message = loadScriptResponseProbe.receiveMessage()
      message shouldBe a[SequencerLocation]
      val initialLocation = message.asInstanceOf[SequencerLocation].location

      when(sequencerServer.shutDown()).thenReturn(Done)

      //Restart sequencer and assert if it returns new AkkaLocation of sequencer
      sequenceComponentRef ! RestartScript(restartResponseProbe.ref)

      val message1 = restartResponseProbe.receiveMessage()
      message1 shouldBe a[SequencerLocation]
      val restartLocationResponse: AkkaLocation = message1.asInstanceOf[SequencerLocation].location
      restartLocationResponse.connection shouldEqual AkkaConnection(
        ComponentId(prefix, ComponentType.Sequencer)
      )
      restartLocationResponse should not equal initialLocation
    }

    "restart should fail if sequence component is in idle state | ESW-141" in {
      val (sequenceComponentRef, _) = spawnSequenceComponent()

      val restartResponseProbe = TestProbe[ScriptResponseOrUnhandled]()
      sequenceComponentRef ! RestartScript(restartResponseProbe.ref)
      restartResponseProbe.expectMessage(Unhandled(Idle, "RestartScript"))
    }

    "shutdown itself on Shutdown message | ESW-329" in {
      val system: ActorSystem[SpawnProtocol.Command] = ActorSystem(SpawnProtocol(), "sequencer-shut-down-system")
      val (sequenceComponentRef, _)                  = spawnSequenceComponent()(system)

      when(locationService.unregister(AkkaConnection(ComponentId(sequenceComponentPrefix, SequenceComponent))))
        .thenReturn(Future.successful(Done))

      val shutdownResponseProbe = TestProbe[Ok.type]()(system)
      sequenceComponentRef ! Shutdown(shutdownResponseProbe.ref)
      shutdownResponseProbe.expectMessage(Ok)
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
