package esw.ocs.script

import java.nio.file.Path

import akka.actor.testkit.typed.scaladsl.TestProbe
import com.typesafe.config.ConfigFactory
import csw.alarm.client.AlarmServiceFactory
import csw.alarm.models.AlarmSeverity
import csw.alarm.models.Key.AlarmKey
import csw.config.api.scaladsl.ConfigService
import csw.config.api.{ConfigData, TokenFactory}
import csw.config.client.scaladsl.ConfigClientFactory
import csw.event.client.EventServiceFactory
import csw.location.client.scaladsl.HttpLocationServiceFactory
import csw.params.commands.CommandResponse.{Completed, Error}
import csw.params.commands.{CommandName, Sequence, Setup}
import csw.params.core.generics.KeyType.StringKey
import csw.params.core.generics.Parameter
import csw.params.core.models.Subsystem.{ESW, LGSF, NFIRAOS, TCS}
import csw.params.core.models.{Id, Prefix}
import csw.params.events.{Event, EventKey, EventName, SystemEvent}
import csw.testkit.ConfigTestKit
import csw.time.core.models.UTCTime
import esw.ocs.api.SequencerApi
import esw.ocs.api.models.StepStatus.Finished.Success
import esw.ocs.api.models.{Step, StepList}
import esw.ocs.api.protocol._
import esw.ocs.impl.SequencerActorProxy
import esw.ocs.testkit.EswTestKit
import esw.ocs.testkit.Service._

import scala.concurrent.Future

class ScriptIntegrationTest extends EswTestKit(EventServer, AlarmServer, ConfigServer) {

  // TestScript.kt
  private val ocsSubsystem     = ESW
  private val ocsObservingMode = "darknight"
  private val tcsSubsystem     = TCS
  private val tcsObservingMode = "darknight"

  // TestScript4.kts
  private val lgsfSubsystem                = LGSF
  private val lgsfObservingMode            = "darknight"
  private val configTestKit: ConfigTestKit = frameworkTestKit.configTestKit
  private var ocsSequencer: SequencerApi   = _
  private var tcsSequencer: SequencerApi   = _
  private var lgsfSequencer: SequencerApi  = _

  override def beforeAll(): Unit = {
    super.beforeAll()
    frameworkTestKit.spawnStandalone(ConfigFactory.load("standalone.conf"))
  }

  override def beforeEach(): Unit = {
    val tcsSequencerRef  = spawnSequencerRef(tcsSubsystem, tcsObservingMode)
    val lgsfSequencerRef = spawnSequencerRef(lgsfSubsystem, lgsfObservingMode) //start LGSF sequencer as OCS send commands to LGSF downstream sequencer
    val ocsSequencerRef  = spawnSequencerRef(ocsSubsystem, ocsObservingMode)
    ocsSequencer = new SequencerActorProxy(ocsSequencerRef)
    tcsSequencer = new SequencerActorProxy(tcsSequencerRef)
    lgsfSequencer = new SequencerActorProxy(lgsfSequencerRef)
  }

  override def afterEach(): Unit = shutdownAllSequencers()

  private def withIds(stepListMaybe: Future[Option[StepList]], ids: Id*): Future[Option[StepList]] = {
    stepListMaybe.map {
      _.map { x =>
        StepList(x.steps.zip(ids).map {
          case (step, id) => step.withId(id)
        })
      }
    }
  }

  "Sequencer Script" must {
    "be able to send sequence to other Sequencer by resolving location through TestScript | ESW-88, ESW-145, ESW-190, ESW-195, ESW-119, ESW-251" in {
      val command  = Setup(Prefix("esw.test"), CommandName("command-4"), None)
      val sequence = Sequence(Seq(command))

      tcsSequencer.getSequence.futureValue shouldBe None

      val submitResponseF = ocsSequencer.submitAndWait(sequence)

      // This has to match with sequence created in TestScript -> handleSetupCommand("command-4")
      val assertableCommand =
        Setup(Prefix("esw.test"), CommandName("command-3"), None, Set.empty)
      val step             = Step(assertableCommand).copy(status = Success)
      val steps            = List(step)
      val expectedStepList = StepList(steps)
      Thread.sleep(1000)

      // response received by irisSequencer
      submitResponseF.futureValue shouldBe a[Completed]
      withIds(tcsSequencer.getSequence, step.id).futureValue.get should ===(expectedStepList)
    }

    "be able to forward diagnostic mode to downstream components | ESW-118" in {
      val eventKey = EventKey(Prefix("tcs.filter.wheel"), EventName("diagnostic-data"))

      val testProbe    = TestProbe[Event]
      val subscription = eventSubscriber.subscribeActorRef(Set(eventKey), testProbe.ref)
      subscription.ready().futureValue
      testProbe.expectMessageType[SystemEvent] // discard invalid event

      //diagnosticMode
      val diagnosticModeParam: Parameter[_] = StringKey.make("mode").set("diagnostic")

      ocsSequencer.diagnosticMode(UTCTime.now(), "engineering").futureValue should ===(Ok)

      val actualDiagEvent = testProbe.expectMessageType[SystemEvent]
      actualDiagEvent.paramSet.head shouldBe diagnosticModeParam

      //operationsMode
      val operationsModeParam = StringKey.make("mode").set("operations")

      ocsSequencer.operationsMode().futureValue should ===(Ok)

      val actualOpEvent = testProbe.expectMessageType[SystemEvent]
      actualOpEvent.paramSet.head shouldBe operationsModeParam
    }

    "be able to forward GoOnline/GoOffline message to downstream components | ESW-236" in {
      val onlineKey  = EventKey(Prefix("tcs.filter.wheel"), EventName("online"))
      val offlineKey = EventKey(Prefix("tcs.filter.wheel"), EventName("offline"))

      val testProbe          = TestProbe[Event]
      val onlineSubscription = eventSubscriber.subscribeActorRef(Set(onlineKey), testProbe.ref)
      onlineSubscription.ready().futureValue
      testProbe.expectMessageType[SystemEvent] // discard invalid event

      val offlineSubscription = eventSubscriber.subscribeActorRef(Set(offlineKey), testProbe.ref)
      offlineSubscription.ready().futureValue
      testProbe.expectMessageType[SystemEvent] // discard invalid event

      //goOffline
      ocsSequencer.goOffline().futureValue should ===(Ok)

      val actualOfflineEvent = testProbe.expectMessageType[SystemEvent]
      actualOfflineEvent.eventKey should ===(offlineKey)

      //goOnline
      ocsSequencer.goOnline().futureValue should ===(Ok)

      val actualOnlineEvent = testProbe.expectMessageType[SystemEvent]
      actualOnlineEvent.eventKey should ===(onlineKey)
    }

    "be able to set severity of sequencer alarms and refresh it ESW-125" in {
      val config            = ConfigFactory.parseResources("alarm_key.conf")
      val alarmAdminService = new AlarmServiceFactory().makeAdminApi(locationService)
      alarmAdminService.initAlarms(config, reset = true).futureValue

      val alarmKey = AlarmKey(NFIRAOS, "trombone", "tromboneAxisHighLimitAlarm")
      val command  = Setup(Prefix("NFIRAOS.test"), CommandName("set-alarm-severity"), None)
      val sequence = Sequence(command)

      ocsSequencer.submitAndWait(sequence).futureValue shouldBe a[Completed]
      alarmAdminService.getCurrentSeverity(alarmKey).futureValue should ===(AlarmSeverity.Major)

      Thread.sleep(2500) // as per test config, alarm severity will expire if not refreshed.
      alarmAdminService.getCurrentSeverity(alarmKey).futureValue should ===(AlarmSeverity.Major)
    }

    "be able to get a published event | ESW-120" in {
      val eventService = new EventServiceFactory().make(HttpLocationServiceFactory.makeLocalClient)
      val publishF     = eventService.defaultPublisher.publish(SystemEvent(Prefix("esw.test"), EventName("get.event")))
      publishF.futureValue

      val command  = Setup(Prefix("esw.test"), CommandName("get-event"), None)
      val sequence = Sequence(Seq(command))

      ocsSequencer.submitAndWait(sequence).futureValue shouldBe a[Completed]

      val successKey        = EventKey("esw.test.get.success")
      val getPublishedEvent = eventSubscriber.get(successKey).futureValue

      getPublishedEvent.isInvalid should ===(false)
    }

    "be able to subscribe a event key | ESW-120" in {
      val eventService = new EventServiceFactory().make(HttpLocationServiceFactory.makeLocalClient)

      val command  = Setup(Prefix("esw.test"), CommandName("on-event"), None)
      val sequence = Sequence(Seq(command))

      ocsSequencer.submitAndWait(sequence).futureValue shouldBe a[Completed]

      val publishF = eventService.defaultPublisher.publish(SystemEvent(Prefix("esw.test"), EventName("get.event")))
      publishF.futureValue

      Thread.sleep(1000)

      val successKey        = EventKey("esw.test.onEvent.success")
      val getPublishedEvent = eventSubscriber.get(successKey).futureValue

      getPublishedEvent.isInvalid should ===(false)
    }

    "be able to send abortSequence to downstream sequencers and call abortHandler | ESW-137, ESW-155" in {
      val eventKey     = EventKey(Prefix("tcs.test"), EventName("abort.success"))
      val testProbe    = TestProbe[Event]
      val subscription = eventSubscriber.subscribeActorRef(Set(eventKey), testProbe.ref)
      subscription.ready().futureValue
      testProbe.expectMessageType[SystemEvent] // discard invalid event

      // Submit sequence to OCS as AbortSequence is accepted only in InProgress State
      val command1 = Setup(Prefix("LGSF.test"), CommandName("command-lgsf"), None)
      val command2 = Setup(Prefix("IRIS.test"), CommandName("command-1"), None)
      val command3 = Setup(Prefix("TCS.test"), CommandName("command-2"), None)
      val sequence = Sequence(Seq(command1, command2, command3))

      val response = ocsSequencer.submitAndWait(sequence)

      eventually(ocsSequencer.getSequence.futureValue.get.isInFlight shouldBe true)

      eventually(lgsfSequencer.getSequence.futureValue.get.isInFlight shouldBe true)

      ocsSequencer.abortSequence().futureValue should ===(Ok)

      // Expect Pending steps in OCS sequence are aborted
      eventually {
        ocsSequencer.getSequence.futureValue.get.nextPending shouldBe None
        // handleAbortSequence from ocs script sends abort sequence message to downstream lgsf sequencer
        // lgsf sequencer publish abort event on invocation of handle abort hook which is verified here
        val event = testProbe.receiveMessage()
        event.eventId shouldNot be(-1)
      }

      response.futureValue shouldBe a[Completed]
    }

    "be able to send stop to downstream sequencers and call stopHandler | ESW-138, ESW-156" in {
      val eventKey = EventKey(Prefix("tcs.test"), EventName("stop.success"))

      val testProbe    = TestProbe[Event]
      val subscription = eventSubscriber.subscribeActorRef(Set(eventKey), testProbe.ref)
      subscription.ready().futureValue
      testProbe.expectMessageType[SystemEvent] // discard invalid event

      // Submit sequence to OCS as Stop is accepted only in InProgress State
      val command1 = Setup(Prefix("LGSF.test"), CommandName("command-lgsf"), None)
      val command2 = Setup(Prefix("IRIS.test"), CommandName("command-1"), None)
      val command3 = Setup(Prefix("TCS.test"), CommandName("command-2"), None)
      val sequence = Sequence(Seq(command1, command2, command3))

      val response = ocsSequencer.submitAndWait(sequence)

      eventually(ocsSequencer.getSequence.futureValue.get.isInFlight shouldBe true)

      eventually(lgsfSequencer.getSequence.futureValue.get.isInFlight shouldBe true)

      ocsSequencer.stop().futureValue should ===(Ok)

      eventually {
        ocsSequencer.getSequence.futureValue.get.nextPending shouldBe None
        // handleStop from ocs script sends stop message to downstream lgsf sequencer
        // lgsf sequencer publish stop event on invocation of handle stop hook which is verified here
        val event = testProbe.receiveMessage()
        event.eventId shouldNot be(-1)
      }

      response.futureValue shouldBe a[Completed]

      eventually(ocsSequencer.getSequence.futureValue.get.isInFlight shouldBe false)
    }

    "be able to send commands to downstream assembly | ESW-121" in {
      val eventKey = EventKey(Prefix("tcs.filter.wheel"), EventName("setup-command-from-script"))

      val command  = Setup(Prefix("IRIS.test"), CommandName("command-for-assembly"), None)
      val sequence = Sequence(Seq(command))

      val testProbe    = TestProbe[Event]
      val subscription = eventSubscriber.subscribeActorRef(Set(eventKey), testProbe.ref)
      subscription.ready().futureValue
      testProbe.expectMessageType[SystemEvent] // discard invalid event

      ocsSequencer.submitAndWait(sequence).futureValue shouldBe a[Completed]

      val actualSetupEvent: SystemEvent = testProbe.expectMessageType[SystemEvent]
      actualSetupEvent.eventKey should ===(eventKey)
    }

    "be able to check existence of a config file and fetch config | ESW-123" in {
      val factory = mock[TokenFactory]
      when(factory.getToken).thenReturn("valid")

      val adminApi: ConfigService = ConfigClientFactory.adminApi(configTestKit.actorSystem, locationService, factory)
      configTestKit.initSvnRepo()
      val file = Path.of("/tmt/test/wfos.conf")
      val configValue1: String =
        """
          |component = wfos
          |""".stripMargin
      adminApi.create(file, ConfigData.fromString(configValue1), annex = false, "First commit").futureValue

      val existConfigCommand = Setup(Prefix("WFOS.test"), CommandName("check-config"), None)
      val getConfigCommand   = Setup(Prefix("WFOS.test"), CommandName("get-config-data"), None)
      val sequence           = Sequence(Seq(existConfigCommand, getConfigCommand))

      ocsSequencer.submitAndWait(sequence).futureValue shouldBe a[Completed]

      // verify existConfig api
      val existConfigKey   = EventKey(Prefix("WFOS.test"), EventName("check-config.success"))
      val existConfigEvent = eventSubscriber.get(existConfigKey).futureValue
      existConfigEvent.eventKey should ===(existConfigKey)

      // verify getConfig api
      val getConfigKey   = EventKey(Prefix("WFOS.test"), EventName("get-config.success"))
      val getConfigEvent = eventSubscriber.get(getConfigKey).futureValue
      getConfigEvent.eventKey should ===(getConfigKey)

      configTestKit.deleteServerFiles()
    }

    "be able to handle unexpected exception and finish the sequence | ESW-241" in {
      val command1 = Setup(Prefix("esw.test"), CommandName("check-exception-1"), None)
      val command2 = Setup(Prefix("esw.test"), CommandName("check-exception-2"), None)
      val sequence = Sequence(Seq(command1, command2))

      val response = ocsSequencer.submitAndWait(sequence).futureValue
      response shouldBe an[Error]
      response.asInstanceOf[Error].message should ===("java.lang.RuntimeException: boom")
    }
  }
}
