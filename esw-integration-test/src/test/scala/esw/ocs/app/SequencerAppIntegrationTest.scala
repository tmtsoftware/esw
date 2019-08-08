package esw.ocs.app

import akka.Done
import akka.actor.Scheduler
import akka.actor.testkit.typed.scaladsl.TestProbe
import akka.actor.typed.ActorSystem
import akka.util.Timeout
import csw.command.client.internal.SequencerCommandServiceImpl
import csw.location.api.extensions.URIExtension.RichURI
import csw.location.api.scaladsl.LocationService
import csw.location.client.scaladsl.HttpLocationServiceFactory
import csw.location.models.Connection.AkkaConnection
import csw.location.models.{ComponentId, ComponentType}
import csw.params.commands.CommandResponse.Completed
import csw.params.commands.{CommandName, Sequence, Setup}
import csw.params.core.models.Prefix
import csw.testkit.scaladsl.ScalaTestFrameworkTestKit
import esw.ocs.api.BaseTestSuite
import esw.ocs.api.models.messages.SequenceComponentMsg
import esw.ocs.api.models.messages.SequenceComponentMsg.{LoadScript, UnloadScript}
import esw.ocs.api.models.messages.SequenceComponentResponses.LoadScriptResponse
import esw.ocs.app.SequencerAppCommand.{SequenceComponent, Sequencer}
import esw.ocs.exceptions.ScriptLoadingException.ScriptNotFound

import scala.concurrent.Future
import scala.concurrent.duration.DurationInt

class SequencerAppIntegrationTest extends ScalaTestFrameworkTestKit with BaseTestSuite {
  import frameworkTestKit._
  implicit val typedSystem: ActorSystem[_]         = actorSystem
  implicit val scheduler: Scheduler                = typedSystem.scheduler
  implicit val timeout: Timeout                    = Timeout(25.seconds)
  private val testLocationService: LocationService = HttpLocationServiceFactory.makeLocalClient

  override implicit def patienceConfig: PatienceConfig = PatienceConfig(15.seconds, 10.milli)

  "SequenceComponent command" must {
    "start sequence component with provided prefix and register it with location service | ESW-102, ESW-103, ESW-147, ESW-151, ESW-214" in {
      val prefixStr             = "esw.test.prefix"
      val prefix: Prefix        = Prefix(prefixStr)
      val uniqueId              = "1"
      val sequenceComponentName = s"${prefix.subsystem}_$uniqueId"

      // start Sequence Component
      SequencerApp.run(SequenceComponent(prefix), enableLogging = false)

      // verify Sequence component is started and registered with location service
      val connection           = AkkaConnection(ComponentId(sequenceComponentName, ComponentType.SequenceComponent))
      val sequenceCompLocation = testLocationService.resolve(connection, 5.seconds).futureValue.get
      sequenceCompLocation.connection shouldBe connection

      // LoadScript
      val seqCompRef = sequenceCompLocation.uri.toActorRef.unsafeUpcast[SequenceComponentMsg]
      val probe      = TestProbe[LoadScriptResponse]
      seqCompRef ! LoadScript("testSequencerId1", "testObservingMode1", probe.ref)

      // verify that loaded sequencer is started and able to process sequence command
      val response          = probe.expectMessageType[LoadScriptResponse]
      val sequencerLocation = response.response.rightValue

      //verify sequencerName has SequenceComponentName
      val actualSequencerName: String = sequencerLocation.connection.componentId.name
      actualSequencerName shouldEqual s"$sequenceComponentName@testSequencerId1@testObservingMode1"

      val commandService = new SequencerCommandServiceImpl(sequencerLocation)
      val setup          = Setup(Prefix("wfos.home.datum"), CommandName("command-1"), None)
      val sequence       = Sequence(setup)
      commandService.submit(sequence).futureValue shouldBe Completed(sequence.runId)

      // UnloadScript
      val probe2 = TestProbe[Done]
      seqCompRef ! UnloadScript(probe2.ref)
      probe2.expectMessage(Done)
    }

    "start sequence component and register with automatically generated, incremental uniqueIDs, ESW-144" in {
      val prefixStr      = "esw.test.prefix"
      val prefix: Prefix = Prefix(prefixStr)

      Future { SequencerApp.run(SequenceComponent(prefix), false) }
      Future { SequencerApp.run(SequenceComponent(prefix), false) }
      Future { SequencerApp.run(SequenceComponent(prefix), false) }

      val uniqueId1              = "1"
      val sequenceComponentName1 = s"${prefix.subsystem}_$uniqueId1"
      val connection1            = AkkaConnection(ComponentId(sequenceComponentName1, ComponentType.SequenceComponent))
      val sequenceCompLocation1  = testLocationService.resolve(connection1, 5.seconds).futureValue.get
      sequenceCompLocation1.connection shouldBe connection1

      val uniqueId2              = "2"
      val sequenceComponentName2 = s"${prefix.subsystem}_$uniqueId2"
      val connection2            = AkkaConnection(ComponentId(sequenceComponentName2, ComponentType.SequenceComponent))
      val sequenceCompLocation2  = testLocationService.resolve(connection2, 5.seconds).futureValue.get
      sequenceCompLocation2.connection shouldBe connection2

      val uniqueId3              = "3"
      val sequenceComponentName3 = s"${prefix.subsystem}_$uniqueId3"
      val connection3            = AkkaConnection(ComponentId(sequenceComponentName3, ComponentType.SequenceComponent))
      val sequenceCompLocation3  = testLocationService.resolve(connection3, 5.seconds).futureValue.get
      sequenceCompLocation3.connection shouldBe connection3
    }
  }

  "Sequencer command" must {
    "start sequencer with provided id, mode and register it with location service | ESW-102, ESW-103, ESW-147, ESW-151" in {
      val sequencerId   = "testSequencerId1"
      val observingMode = "testObservingMode1"
      val sequencerName = s"$sequencerId@$observingMode"

      // start Sequencer
      SequencerApp.run(Sequencer(sequencerId, observingMode), enableLogging = false)

      // verify that sequencer is started and able to process sequence command
      val connection        = AkkaConnection(ComponentId(sequencerName, ComponentType.Sequencer))
      val sequencerLocation = testLocationService.resolve(connection, 5.seconds).futureValue.value

      sequencerLocation.connection.componentId.name shouldBe sequencerName

      val commandService = new SequencerCommandServiceImpl(sequencerLocation)
      val setup          = Setup(Prefix("wfos.home.datum"), CommandName("command-1"), None)
      val sequence       = Sequence(setup)
      commandService.submit(sequence).futureValue shouldBe Completed(sequence.runId)
    }

    "throw exception if provided script configuration is invalid | ESW-102" in {
      val sequencerId   = "testSequencerId3"
      val observingMode = "testObservingMode3"

      intercept[ScriptNotFound] {
        SequencerApp.run(Sequencer(sequencerId, observingMode), enableLogging = false)
      }
    }
  }
}
