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
import csw.location.models.{AkkaLocation, ComponentId, ComponentType}
import csw.params.commands.CommandResponse.Completed
import csw.params.commands.{CommandName, Sequence, Setup}
import csw.params.core.models.{Prefix, Subsystem}
import csw.testkit.scaladsl.ScalaTestFrameworkTestKit
import esw.dsl.script.exceptions.ScriptLoadingException.ScriptNotFound
import esw.ocs.api.BaseTestSuite
import esw.ocs.api.protocol.LoadScriptResponse
import esw.ocs.app.SequencerAppCommand.{SequenceComponent, Sequencer}
import esw.ocs.impl.messages.SequenceComponentMsg
import esw.ocs.impl.messages.SequenceComponentMsg.{LoadScript, UnloadScript}

import scala.concurrent.Future
import scala.concurrent.duration.DurationInt

class SequencerAppIntegrationTest extends ScalaTestFrameworkTestKit with BaseTestSuite {
  import frameworkTestKit._
  implicit val typedSystem: ActorSystem[_]         = actorSystem
  implicit val scheduler: Scheduler                = typedSystem.scheduler
  implicit val timeout: Timeout                    = Timeout(25.seconds)
  private val testLocationService: LocationService = HttpLocationServiceFactory.makeLocalClient

  override implicit def patienceConfig: PatienceConfig = PatienceConfig(15.seconds)

  override def afterEach(): Unit = {
    super.afterEach()
    testLocationService.unregisterAll()
  }

  "SequenceComponent command" must {
    "start sequence component with provided subsystem and name and register it with location service | ESW-102, ESW-103, ESW-147, ESW-151, ESW-214" in {
      val subsystem    = Subsystem.ESW
      val name: String = "primary"

      // start Sequence Component
      SequencerApp.run(SequenceComponent(subsystem, Some(name)), enableLogging = false)

      // verify Sequence component is started and registered with location service
      val sequenceCompLocation: AkkaLocation =
        testLocationService
          .resolve(AkkaConnection(ComponentId(s"${subsystem}.$name", ComponentType.SequenceComponent)), 5.seconds)
          .futureValue
          .get

      sequenceCompLocation.connection shouldEqual AkkaConnection(ComponentId("ESW.primary", ComponentType.SequenceComponent))
      sequenceCompLocation.prefix shouldEqual Prefix("ESW.primary")

      // LoadScript
      val seqCompRef = sequenceCompLocation.uri.toActorRef.unsafeUpcast[SequenceComponentMsg]
      val probe      = TestProbe[LoadScriptResponse]
      seqCompRef ! LoadScript("testSequencerId1", "testObservingMode1", probe.ref)

      // verify that loaded sequencer is started and able to process sequence command
      val response          = probe.expectMessageType[LoadScriptResponse]
      val sequencerLocation = response.response.rightValue

      //verify sequencerName has SequenceComponentName
      val actualSequencerName: String = sequencerLocation.connection.componentId.name
      actualSequencerName shouldEqual "ESW.primary@testSequencerId1@testObservingMode1"

      val commandService = new SequencerCommandServiceImpl(sequencerLocation)
      val setup          = Setup(Prefix("wfos.home.datum"), CommandName("command-1"), None)
      val sequence       = Sequence(setup)
      commandService.submitAndWait(sequence).futureValue shouldBe Completed(sequence.runId)

      // UnloadScript
      val probe2 = TestProbe[Done]
      seqCompRef ! UnloadScript(probe2.ref)
      probe2.expectMessage(Done)
    }

    "start sequence component and register with automatically generated random uniqueIDs if name is not provided| ESW-144" in {
      val subsystem = Subsystem.ESW

      SequencerApp.run(SequenceComponent(subsystem, None), enableLogging = false)

      val sequenceComponentLocation = testLocationService.list(ComponentType.SequenceComponent).futureValue.head

      //assert that componentName and prefix contain subsystem provided
      sequenceComponentLocation.connection.componentId.name.contains("ESW.ESW_") shouldEqual true
      sequenceComponentLocation.asInstanceOf[AkkaLocation].prefix.prefix.contains("ESW.ESW_") shouldEqual true
    }

    "start sequence component concurrently and register with automatically generated random uniqueIDs if name is not provided| ESW-144" in {
      val subsystem = Subsystem.ESW

      //register sequence component with same subsystem concurrently
      Future { SequencerApp.run(SequenceComponent(subsystem, None), enableLogging = false) }
      Future { SequencerApp.run(SequenceComponent(subsystem, None), enableLogging = false) }
      Future { SequencerApp.run(SequenceComponent(subsystem, None), enableLogging = false) }

      //Wait till futures registering sequence component complete
      Thread.sleep(5000)

      val sequenceComponentLocations = testLocationService.list(ComponentType.SequenceComponent).futureValue

      //assert if all 3 sequence components are registered
      testLocationService.list(ComponentType.SequenceComponent).futureValue.length shouldEqual 3
      sequenceComponentLocations.foreach { location =>
        {
          //assert that componentName and prefix contain subsystem provided
          location.connection.componentId.name.contains("ESW.ESW_") shouldEqual true
          location.asInstanceOf[AkkaLocation].prefix.prefix.contains("ESW.ESW_") shouldEqual true
        }
      }
    }
  }

  "Sequencer command" must {
    "start sequencer with provided id, mode and register it with location service | ESW-103, ESW-147, ESW-151" in {
      val packageId     = "testSequencerId1"
      val observingMode = "testObservingMode1"
      val sequencerName = s"$packageId@$observingMode"

      // start Sequencer
      SequencerApp.run(Sequencer(packageId, observingMode), enableLogging = false)

      // verify that sequencer is started and able to process sequence command
      val connection        = AkkaConnection(ComponentId(sequencerName, ComponentType.Sequencer))
      val sequencerLocation = testLocationService.resolve(connection, 5.seconds).futureValue.value

      sequencerLocation.connection.componentId.name shouldBe sequencerName

      val commandService = new SequencerCommandServiceImpl(sequencerLocation)
      val setup          = Setup(Prefix("wfos.home.datum"), CommandName("command-1"), None)
      val sequence       = Sequence(setup)
      commandService.submitAndWait(sequence).futureValue shouldBe Completed(sequence.runId)
    }

    "throw exception if provided script configuration is invalid | ESW-102" in {
      val packageId     = "testSequencerId3"
      val observingMode = "testObservingMode3"

      intercept[ScriptNotFound] {
        SequencerApp.run(Sequencer(packageId, observingMode), enableLogging = false)
      }
    }
  }
}
