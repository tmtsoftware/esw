package esw.ocs.script

import akka.actor.testkit.typed.scaladsl.TestProbe
import akka.actor.typed.scaladsl.AskPattern._
import akka.actor.typed.{ActorRef, ActorSystem, SpawnProtocol}
import akka.util.Timeout
import com.typesafe.config.ConfigFactory
import csw.alarm.client.AlarmServiceFactory
import csw.alarm.models.AlarmSeverity
import csw.alarm.models.Key.AlarmKey
import csw.command.client.messages.sequencer.{SequencerMsg, SubmitSequenceAndWait}
import csw.event.client.EventServiceFactory
import csw.location.api.extensions.URIExtension.RichURI
import csw.location.api.scaladsl.LocationService
import csw.location.client.scaladsl.HttpLocationServiceFactory
import csw.logging.client.scaladsl.LoggingSystemFactory
import csw.logging.models.Level
import csw.params.commands.CommandResponse.{Completed, SubmitResponse}
import csw.params.commands.{CommandName, Sequence, Setup}
import csw.params.core.generics.KeyType.StringKey
import csw.params.core.generics.Parameter
import csw.params.core.models.Subsystem.NFIRAOS
import csw.params.core.models.{Id, Prefix}
import csw.params.events.{Event, EventKey, EventName, SystemEvent}
import csw.testkit.scaladsl.CSWService.{AlarmServer, EventServer}
import csw.testkit.scaladsl.ScalaTestFrameworkTestKit
import csw.time.core.models.UTCTime
import esw.ocs.api.BaseTestSuite
import esw.ocs.api.models.StepStatus.Finished.Success
import esw.ocs.api.models.{Step, StepList}
import esw.ocs.api.protocol._
import esw.ocs.app.wiring.SequencerWiring
import esw.ocs.impl.internal.Timeouts
import esw.ocs.impl.messages.SequencerMessages._

import scala.concurrent.Future
import scala.concurrent.duration.DurationDouble

class ScriptIntegrationTest extends ScalaTestFrameworkTestKit(EventServer, AlarmServer) with BaseTestSuite {

  import frameworkTestKit.mat

  implicit val actorSystem: ActorSystem[SpawnProtocol.Command] = frameworkTestKit.actorSystem

  private implicit val askTimeout: Timeout = Timeouts.DefaultTimeout

  override implicit def patienceConfig: PatienceConfig = PatienceConfig(10.seconds)

  // TestScript.kt
  private val ocsPackageId     = "esw"
  private val ocsObservingMode = "darknight"
  private val tcsPackageId     = "tcs"
  private val tcsObservingMode = "darknight"

  // TestScript4.kts
  private val irmsPackageId     = "irms"
  private val irmsObservingMode = "darknight"

  private var locationService: LocationService     = _
  private var ocsWiring: SequencerWiring           = _
  private var ocsSequencer: ActorRef[SequencerMsg] = _
  private var tcsWiring: SequencerWiring           = _
  private var tcsSequencer: ActorRef[SequencerMsg] = _
  private var irmsWiring: SequencerWiring          = _

  override def beforeAll(): Unit = {
    super.beforeAll()
    frameworkTestKit.spawnStandalone(ConfigFactory.load("standalone.conf"))
    val system = LoggingSystemFactory.start("abort-test", "", "", actorSystem)
    system.setAkkaLevel(Level.INFO)
    system.setDefaultLogLevel(Level.INFO)
    system.setSlf4jLevel(Level.INFO)
  }

  override def beforeEach(): Unit = {
    locationService = HttpLocationServiceFactory.makeLocalClient
    tcsWiring = new SequencerWiring(tcsPackageId, tcsObservingMode, None)
    tcsWiring.sequencerServer.start()
    tcsSequencer = tcsWiring.sequencerRef

    //start IRMS sequencer as OCS sends AbortSequence to IRMS downstream sequencer
    irmsWiring = new SequencerWiring(irmsPackageId, irmsObservingMode, None)
    irmsWiring.sequencerServer.start()

    ocsWiring = new SequencerWiring(ocsPackageId, ocsObservingMode, None)
    ocsSequencer = ocsWiring.sequencerServer.start().rightValue.uri.toActorRef.unsafeUpcast[SequencerMsg]
  }

  override def afterEach(): Unit = {
    ocsWiring.sequencerServer.shutDown().futureValue
    tcsWiring.sequencerServer.shutDown().futureValue
  }

  "CswServices" must {
    "be able to send sequence to other Sequencer by resolving location through TestScript | ESW-88, ESW-145, ESW-190, ESW-195, ESW-119" in {
      val command             = Setup(Prefix("TCS.test"), CommandName("command-4"), None)
      val submitResponseProbe = TestProbe[SubmitResponse]
      val sequenceId          = Id()
      val sequence            = Sequence(sequenceId, Seq(command))

      val initialStepList: Future[Option[StepList]] = tcsSequencer ? GetSequence
      initialStepList.futureValue shouldBe None

      ocsSequencer ! SubmitSequenceAndWait(sequence, submitResponseProbe.ref)

      val commandId = Id("testCommandIdString123")
      // This has to match with sequence created in TestScript -> handleSetupCommand("command-4")
      val assertableCommand =
        Setup(commandId, Prefix("TCS.test"), CommandName("command-3"), None, Set.empty)
      val steps            = List(Step(assertableCommand).copy(status = Success(Completed(commandId))))
      val expectedStepList = StepList(Id("testSequenceIdString123"), steps)
      Thread.sleep(1000)

      val actualStepList: Future[Option[StepList]] = tcsSequencer ? GetSequence
      // response received by irisSequencer
      submitResponseProbe.expectMessage(Completed(sequenceId))
      actualStepList.futureValue.get should ===(expectedStepList)
    }

    "be able to forward diagnostic mode to downstream components | ESW-118" in {
      val eventService = new EventServiceFactory().make(HttpLocationServiceFactory.makeLocalClient)
      val eventKey     = EventKey(Prefix("tcs.filter.wheel"), EventName("diagnostic-data"))

      val testProbe    = TestProbe[Event]
      val subscription = eventService.defaultSubscriber.subscribeActorRef(Set(eventKey), testProbe.ref)
      subscription.ready().futureValue
      testProbe.expectMessageType[SystemEvent] // discard invalid event

      //diagnosticMode
      val diagnosticModeParam: Parameter[_] = StringKey.make("mode").set("diagnostic")

      val diagnosticModeResF: Future[DiagnosticModeResponse] = ocsSequencer ? (DiagnosticMode(UTCTime.now(), "engineering", _))
      diagnosticModeResF.futureValue should ===(Ok)

      val actualDiagEvent = testProbe.expectMessageType[SystemEvent]
      actualDiagEvent.paramSet.head shouldBe diagnosticModeParam

      //operationsMode
      val operationsModeParam = StringKey.make("mode").set("operations")

      val operationsModeResF: Future[OperationsModeResponse] = ocsSequencer ? OperationsMode
      operationsModeResF.futureValue should ===(Ok)

      val actualOpEvent = testProbe.expectMessageType[SystemEvent]
      actualOpEvent.paramSet.head shouldBe operationsModeParam
    }

    "be able to forward GoOnline/GoOffline message to downstream components | ESW-236" in {
      val eventService = new EventServiceFactory().make(HttpLocationServiceFactory.makeLocalClient)
      val onlineKey    = EventKey(Prefix("tcs.filter.wheel"), EventName("online"))
      val offlineKey   = EventKey(Prefix("tcs.filter.wheel"), EventName("offline"))

      val testProbe          = TestProbe[Event]
      val onlineSubscription = eventService.defaultSubscriber.subscribeActorRef(Set(onlineKey), testProbe.ref)
      onlineSubscription.ready().futureValue
      testProbe.expectMessageType[SystemEvent] // discard invalid event

      val offlineSubscription = eventService.defaultSubscriber.subscribeActorRef(Set(offlineKey), testProbe.ref)
      offlineSubscription.ready().futureValue
      testProbe.expectMessageType[SystemEvent] // discard invalid event

      //goOffline
      val goOfflineResF: Future[GoOfflineResponse] = ocsSequencer ? GoOffline
      goOfflineResF.futureValue should ===(Ok)

      val actualOfflineEvent = testProbe.expectMessageType[SystemEvent]
      actualOfflineEvent.eventKey should ===(offlineKey)

      //goOnline
      val goOnlineResF: Future[GoOnlineResponse] = ocsSequencer ? GoOnline
      goOnlineResF.futureValue should ===(Ok)

      val actualOnlineEvent = testProbe.expectMessageType[SystemEvent]
      actualOnlineEvent.eventKey should ===(onlineKey)
    }

    "be able to set severity of sequencer alarms | ESW-125" in {
      val config            = ConfigFactory.parseResources("alarm_key.conf")
      val alarmAdminService = new AlarmServiceFactory().makeAdminApi(locationService)
      alarmAdminService.initAlarms(config, reset = true).futureValue

      val alarmKey = AlarmKey(NFIRAOS, "trombone", "tromboneAxisHighLimitAlarm")
      val command  = Setup(Prefix("NFIRAOS.test"), CommandName("set-alarm-severity"), None)
      val sequence = Sequence(command)

      val sequenceRes: Future[SubmitResponse] = ocsSequencer ? (SubmitSequenceAndWait(sequence, _))

      sequenceRes.futureValue should ===(Completed(sequence.runId))
      alarmAdminService.getCurrentSeverity(alarmKey).futureValue should ===(AlarmSeverity.Major)
    }

    "be able to get a published event | ESW-120" in {
      val eventService = new EventServiceFactory().make(HttpLocationServiceFactory.makeLocalClient)
      val publishF     = eventService.defaultPublisher.publish(SystemEvent(Prefix("TCS"), EventName("get.event")))
      publishF.futureValue

      val command  = Setup(Prefix("TCS"), CommandName("get-event"), None)
      val id       = Id()
      val sequence = Sequence(id, Seq(command))

      val submitResponse: Future[SubmitResponse] = ocsSequencer ? (SubmitSequenceAndWait(sequence, _))
      submitResponse.futureValue should ===(Completed(id))

      val successKey        = EventKey("TCS.get.success")
      val getPublishedEvent = eventService.defaultSubscriber.get(successKey).futureValue

      getPublishedEvent.isInvalid should ===(false)
    }

    "be able to send abortSequence to downstream sequencers and call abortHandler | ESW-137, ESW-155" in {
      val eventService = new EventServiceFactory().make(HttpLocationServiceFactory.makeLocalClient)
      val eventKey     = EventKey(Prefix("tcs"), EventName("abort.success"))

      val testProbe    = TestProbe[Event]
      val subscription = eventService.defaultSubscriber.subscribeActorRef(Set(eventKey), testProbe.ref)
      subscription.ready().futureValue
      testProbe.expectMessageType[SystemEvent] // discard invalid event

      // Submit sequence to OCS as AbortSequence is accepted only in InProgress State
      val command1            = Setup(Prefix("IRMS.test"), CommandName("command-irms"), None)
      val command2            = Setup(Prefix("IRIS.test"), CommandName("command-1"), None)
      val command3            = Setup(Prefix("TCS.test"), CommandName("command-2"), None)
      val submitResponseProbe = TestProbe[SubmitResponse]
      val sequence            = Sequence(Id(), Seq(command1, command2, command3))

      ocsSequencer ! SubmitSequenceAndWait(sequence, submitResponseProbe.ref)

      val abortSequenceResponseF: Future[OkOrUnhandledResponse] = ocsSequencer ? AbortSequence
      abortSequenceResponseF.futureValue should ===(Ok)

      //Expect Pending steps in OCS sequence are aborted
      eventually {
        val maybeStepListF: Future[Option[StepList]] = ocsSequencer ? GetSequence
        maybeStepListF.futureValue.get.nextPending shouldBe None
        val event = testProbe.receiveMessage()
        event.eventId shouldNot be(-1)
      }
    }

    "be able to send stop to downstream sequencers and call stopHandler | ESW-138, ESW-156" in {
      val eventService = new EventServiceFactory().make(HttpLocationServiceFactory.makeLocalClient)
      val eventKey     = EventKey(Prefix("tcs"), EventName("stop.success"))

      val testProbe    = TestProbe[Event]
      val subscription = eventService.defaultSubscriber.subscribeActorRef(Set(eventKey), testProbe.ref)
      subscription.ready().futureValue
      testProbe.expectMessageType[SystemEvent] // discard invalid event

      // Submit sequence to OCS as Stop is accepted only in InProgress State
      val command1            = Setup(Prefix("IRMS.test"), CommandName("command-irms"), None)
      val command2            = Setup(Prefix("IRIS.test"), CommandName("command-1"), None)
      val command3            = Setup(Prefix("TCS.test"), CommandName("command-2"), None)
      val submitResponseProbe = TestProbe[SubmitResponse]
      val sequenceId          = Id()
      val sequence            = Sequence(sequenceId, Seq(command1, command2, command3))

      ocsSequencer ! SubmitSequenceAndWait(sequence, submitResponseProbe.ref)

      val stopResponseF: Future[OkOrUnhandledResponse] = ocsSequencer ? Stop
      stopResponseF.futureValue should ===(Ok)

      eventually {
        val event = testProbe.receiveMessage()
        event.eventId shouldNot be(-1)
      }
    }

    "be able to send commands to downstream assembly | ESW-121" in {
      val eventService = new EventServiceFactory().make(HttpLocationServiceFactory.makeLocalClient)
      val eventKey     = EventKey(Prefix("tcs.filter.wheel"), EventName("setup-command-from-script"))

      val command    = Setup(Prefix("IRIS.test"), CommandName("command-for-assembly"), None)
      val sequenceId = Id()
      val sequence   = Sequence(sequenceId, Seq(command))

      val testProbe    = TestProbe[Event]
      val subscription = eventService.defaultSubscriber.subscribeActorRef(Set(eventKey), testProbe.ref)
      subscription.ready().futureValue
      testProbe.expectMessageType[SystemEvent] // discard invalid event

      val submitSequenceResponseF: Future[SubmitResponse] = ocsSequencer ? (SubmitSequenceAndWait(sequence, _))
      submitSequenceResponseF.futureValue should ===(Completed(sequenceId))

      val actualSetupEvent: SystemEvent = testProbe.expectMessageType[SystemEvent]
      actualSetupEvent.eventKey should ===(eventKey)
    }
  }
}
