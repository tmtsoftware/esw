package esw.ocs.script

import java.nio.file.Path

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
import csw.params.commands.{CommandName, Observe, Sequence, Setup}
import csw.params.core.generics.KeyType.StringKey
import csw.params.core.generics.{KeyType, Parameter}
import csw.params.core.models.Id
import csw.params.events.{EventKey, EventName, ObserveEvent, SystemEvent}
import csw.prefix.models.Prefix
import csw.prefix.models.Subsystem.{ESW, LGSF, NFIRAOS, TCS}
import csw.testkit.ConfigTestKit
import csw.testkit.scaladsl.CSWService.{AlarmServer, ConfigServer, EventServer}
import csw.time.core.models.UTCTime
import esw.gateway.server.testdata.AssemblyBehaviourFactory
import esw.ocs.api.SequencerApi
import esw.ocs.api.models.StepStatus.Finished.Success
import esw.ocs.api.models.{ObsMode, Step, StepList}
import esw.ocs.api.protocol._
import esw.ocs.testkit.EswTestKit
import esw.ocs.testkit.Service._

import scala.concurrent.Future

class ScriptIntegrationTest extends EswTestKit(EventServer, AlarmServer, ConfigServer) {

  // TestScript.kt
  private val ocsSubsystem = ESW
  private val ocsObsMode   = ObsMode("darknight")
  private val tcsSubsystem = TCS
  private val tcsObsMode   = ObsMode("darknight")

  // TestScript4.kts
  private val lgsfSubsystem                = LGSF
  private val lgsfObsMode                  = ObsMode("darknight")
  private val configTestKit: ConfigTestKit = frameworkTestKit.configTestKit
  private var ocsSequencer: SequencerApi   = _
  private var tcsSequencer: SequencerApi   = _
  private var lgsfSequencer: SequencerApi  = _

  private val tolerance: Long = 1200

  override def beforeAll(): Unit = {
    super.beforeAll()
    spawnAssembly(Prefix("ESW.test"), new AssemblyBehaviourFactory()).futureValue
  }

  override def beforeEach(): Unit = {
    //start LGSF sequencer as OCS send commands to LGSF downstream sequencer
    ocsSequencer = spawnSequencerProxy(ocsSubsystem, ocsObsMode)
    tcsSequencer = spawnSequencerProxy(tcsSubsystem, tcsObsMode)
    lgsfSequencer = spawnSequencerProxy(lgsfSubsystem, lgsfObsMode)
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
    "be able to send sequence to other Sequencer by resolving location through TestScript | ESW-88, ESW-145, ESW-190, ESW-195, ESW-119, ESW-251, CSW-81" in {
      val command  = Setup(Prefix("esw.test"), CommandName("command-4"), None)
      val sequence = Sequence(command)

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

    "be able to forward diagnostic mode to downstream components | ESW-118, CSW-81" in {
      val eventKey = EventKey(Prefix("tcs.filter.wheel"), EventName("diagnostic-data"))

      val testProbe = createTestProbe(Set(eventKey))

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

    "be able to forward GoOnline/GoOffline message to downstream components | ESW-236, CSW-81" in {
      val onlineKey  = EventKey(Prefix("tcs.filter.wheel"), EventName("online"))
      val offlineKey = EventKey(Prefix("tcs.filter.wheel"), EventName("offline"))

      val testProbe = createTestProbe(Set(onlineKey, offlineKey))

      //goOffline
      ocsSequencer.goOffline().futureValue should ===(Ok)

      val actualOfflineEvent = testProbe.expectMessageType[SystemEvent]
      actualOfflineEvent.eventKey should ===(offlineKey)

      //goOnline
      ocsSequencer.goOnline().futureValue should ===(Ok)

      val actualOnlineEvent = testProbe.expectMessageType[SystemEvent]
      actualOnlineEvent.eventKey should ===(onlineKey)
    }

    "be able to set severity of sequencer alarms and refresh it | ESW-125, CSW-81, CSW-83" in {
      val config            = ConfigFactory.parseResources("alarm_key.conf")
      val alarmAdminService = new AlarmServiceFactory().makeAdminApi(locationService)
      alarmAdminService.initAlarms(config, reset = true).futureValue

      val alarmKey = AlarmKey(Prefix(NFIRAOS, "trombone"), "tromboneAxisHighLimitAlarm")
      val command  = Setup(Prefix("NFIRAOS.test"), CommandName("set-alarm-severity"), None)
      val sequence = Sequence(command)

      ocsSequencer.submitAndWait(sequence).futureValue shouldBe a[Completed]
      alarmAdminService.getCurrentSeverity(alarmKey).futureValue should ===(AlarmSeverity.Major)

      Thread.sleep(2500) // as per test config, alarm severity will expire if not refreshed.
      alarmAdminService.getCurrentSeverity(alarmKey).futureValue should ===(AlarmSeverity.Major)
    }

    "be able to get a published event | ESW-120, CSW-81" in {
      val eventService = new EventServiceFactory().make(HttpLocationServiceFactory.makeLocalClient)
      val publishF     = eventService.defaultPublisher.publish(SystemEvent(Prefix("esw.test"), EventName("get.event")))
      publishF.futureValue

      val command  = Setup(Prefix("esw.test"), CommandName("get-event"), None)
      val sequence = Sequence(command)

      ocsSequencer.submitAndWait(sequence).futureValue shouldBe a[Completed]

      val successKey        = EventKey("esw.test.get.success")
      val getPublishedEvent = eventSubscriber.get(successKey).futureValue

      getPublishedEvent.isInvalid should ===(false)
    }

    "be able to subscribe a event key | ESW-120, CSW-81" in {
      val eventService = new EventServiceFactory().make(HttpLocationServiceFactory.makeLocalClient)

      val command  = Setup(Prefix("esw.test"), CommandName("on-event"), None)
      val sequence = Sequence(command)

      ocsSequencer.submitAndWait(sequence).futureValue shouldBe a[Completed]

      val publishF = eventService.defaultPublisher.publish(SystemEvent(Prefix("esw.test"), EventName("get.event")))
      publishF.futureValue

      Thread.sleep(1000)

      val successKey        = EventKey("esw.test.onevent.success")
      val getPublishedEvent = eventSubscriber.get(successKey).futureValue

      getPublishedEvent.isInvalid should ===(false)
    }

    "be able to send abortSequence to downstream sequencers and call abortHandler | ESW-137, ESW-155, CSW-81" in {
      val eventKey  = EventKey(Prefix("tcs.test"), EventName("abort.success"))
      val testProbe = createTestProbe(Set(eventKey))

      // Submit sequence to OCS as AbortSequence is accepted only in InProgress State
      val command1 = Setup(Prefix("LGSF.test"), CommandName("command-lgsf"), None)
      val command2 = Setup(Prefix("IRIS.test"), CommandName("command-1"), None)
      val command3 = Setup(Prefix("TCS.test"), CommandName("command-2"), None)
      val sequence = Sequence(command1, command2, command3)

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

    "be able to send stop to downstream sequencers and call stopHandler | ESW-138, ESW-156, CSW-81" in {
      val eventKey = EventKey(Prefix("tcs.test"), EventName("stop.success"))

      val testProbe = createTestProbe(Set(eventKey))

      // Submit sequence to OCS as Stop is accepted only in InProgress State
      val command1 = Setup(Prefix("LGSF.test"), CommandName("command-lgsf"), None)
      val command2 = Setup(Prefix("IRIS.test"), CommandName("command-1"), None)
      val command3 = Setup(Prefix("TCS.test"), CommandName("command-2"), None)
      val sequence = Sequence(command1, command2, command3)

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

    "be able to send commands to downstream assembly | ESW-121, CSW-81" in {
      val eventKey              = EventKey(Prefix("tcs.filter.wheel"), EventName("setup-command-from-script"))
      val startedEventKey       = EventKey(Prefix("tcs.filter.wheel"), EventName("query-started-command-from-script"))
      val completedEventKey     = EventKey(Prefix("tcs.filter.wheel"), EventName("query-completed-command-from-script"))
      val currentState1EventKey = EventKey(Prefix("tcs.filter.wheel"), EventName("publish-stateName1"))
      val currentState2EventKey = EventKey(Prefix("tcs.filter.wheel"), EventName("publish-stateName2"))

      val command  = Setup(Prefix("IRIS.test"), CommandName("command-for-assembly"), None)
      val sequence = Sequence(command)

      val testProbe =
        createTestProbe(Set(eventKey, startedEventKey, completedEventKey, currentState1EventKey, currentState2EventKey))

      ocsSequencer.submitAndWait(sequence).futureValue shouldBe a[Completed]

      //assert probe for submit response of testAssembly(in testScript)
      val actualSetupEvent: SystemEvent = testProbe.expectMessageType[SystemEvent]
      actualSetupEvent.eventKey should ===(eventKey)

      //assert probe for query response of testAssembly(in testScript)
      val startedEvent = testProbe.expectMessageType[SystemEvent]
      startedEvent.eventKey should ===(startedEventKey)

      //assert probe for queryFinal response of testAssembly(in testScript)
      val completedEvent = testProbe.expectMessageType[SystemEvent]
      completedEvent.eventKey should ===(completedEventKey)

      //assert probe for subscribeCurrentState response for stateName1 of testAssembly(in testScript)
      val currentState1Event = testProbe.expectMessageType[SystemEvent]
      currentState1Event.eventKey should ===(currentState1EventKey)

      //assert probe for subscribeCurrentState response for stateName2 of testAssembly(in testScript)
      val currentState2Event = testProbe.expectMessageType[SystemEvent]
      currentState2Event.eventKey should ===(currentState2EventKey)
    }

    "be able to schedule tasks from now | ESW-122, CSW-81" in {
      val eventKey = EventKey(Prefix("esw.schedule.once"), EventName("offset"))

      val command  = Setup(Prefix("IRIS.test"), CommandName("schedule-once-from-now"), None)
      val sequence = Sequence(command)

      val testProbe = createTestProbe(Set(eventKey))

      ocsSequencer.submitAndWait(sequence).futureValue shouldBe a[Completed]

      eventually {
        val eventToBeAsserted: SystemEvent = testProbe.expectMessageType[SystemEvent]
        val offset: Long                   = eventToBeAsserted.get("offset", KeyType.LongKey).get.head
        offset shouldBe <=(tolerance)
        offset shouldBe >(1000L)
      }
    }

    "be able to schedule periodically tasks from now | ESW-122, CSW-81" in {
      val eventKey = EventKey(Prefix("esw.schedule.periodically"), EventName("offset"))

      val command  = Setup(Prefix("IRIS.test"), CommandName("schedule-periodically-from-now"), None)
      val sequence = Sequence(command)

      val testProbe = createTestProbe(Set(eventKey))

      ocsSequencer.submitAndWait(sequence).futureValue shouldBe a[Completed]

      eventually {
        val eventToBeAsserted: SystemEvent = testProbe.expectMessageType[SystemEvent]
        val offset: Long                   = eventToBeAsserted.get("offset", KeyType.LongKey).get.head
        offset shouldBe <=(tolerance)
        offset shouldBe >(1000L)
      }
      eventually {
        val eventToBeAsserted: SystemEvent = testProbe.expectMessageType[SystemEvent]
        val offset: Long                   = eventToBeAsserted.get("offset", KeyType.LongKey).get.head
        offset shouldBe <=(2 * tolerance)
        offset shouldBe >(2000L)
      }
    }

    "be able to check existence of a config file and fetch config | ESW-123, CSW-81" in {
      val factory = mock[TokenFactory]
      when(factory.getToken).thenReturn("validToken")

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
      val sequence           = Sequence(existConfigCommand, getConfigCommand)

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

    "be able to handle unexpected exception and finish the sequence | ESW-241, CSW-81, ESW-294" in {
      val failCmdName = CommandName("check-exception-1")
      val command1    = Setup(Prefix("esw.test"), failCmdName, None)
      val command2    = Setup(Prefix("esw.test"), CommandName("check-exception-2"), None)
      val sequence    = Sequence(command1, command2)

      val response = ocsSequencer.submitAndWait(sequence).futureValue
      response shouldBe an[Error]
      response.asInstanceOf[Error].message should fullyMatch regex s"StepId: .*, CommandName: ${failCmdName.name}, reason: boom"
    }

    "be able to send publish and subscribe to observe event published by Sequencer | ESW-81" in {
      val command           = Observe(Prefix("esw.test"), CommandName("observe-start"), None)
      val sequence          = Sequence(command)
      val expectedPrefix    = Prefix(ocsSubsystem, ocsObsMode.name)
      val expectedEventName = EventName("ObserveStart")
      val expectedEventKey  = EventKey(expectedPrefix, expectedEventName)
      val testProbe         = createTestProbe(Set(expectedEventKey))

      ocsSequencer.submitAndWait(sequence).futureValue shouldBe a[Completed]

      val actualObserveEvent = testProbe.expectMessageType[ObserveEvent]
      actualObserveEvent.eventName should ===(expectedEventName)
      actualObserveEvent.source should ===(expectedPrefix)
      actualObserveEvent.paramSet shouldBe Set(StringKey.make("obsId").set("2021A-011-153"))
    }

  }
}
