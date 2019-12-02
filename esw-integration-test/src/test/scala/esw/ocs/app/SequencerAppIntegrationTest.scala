package esw.ocs.app

import akka.Done
import akka.actor.testkit.typed.scaladsl.TestProbe
import akka.actor.typed.ActorRef
import akka.actor.typed.scaladsl.AskPattern._
import akka.util.Timeout
import csw.command.client.internal.SequencerCommandServiceImpl
import csw.location.api.extensions.URIExtension.RichURI
import csw.location.models.Connection.AkkaConnection
import csw.location.models.{AkkaLocation, ComponentId, ComponentType}
import csw.params.commands.CommandResponse.Completed
import csw.params.commands.{CommandName, Sequence, Setup}
import csw.params.core.models.Subsystem.ESW
import csw.params.core.models.{Prefix, Subsystem}
import esw.ocs.api.protocol.{ScriptError, ScriptResponse}
import esw.ocs.impl.messages.SequenceComponentMsg
import esw.ocs.impl.messages.SequenceComponentMsg.{LoadScript, UnloadScript}
import esw.ocs.testkit.EswTestKit

import scala.concurrent.Future
import scala.concurrent.duration.DurationInt

class SequencerAppIntegrationTest extends EswTestKit {

  override def afterEach(): Unit = locationService.unregisterAll()

  "SequenceComponent command" must {
    "start sequence component with provided subsystem and prefix and register it with location service | ESW-102, ESW-136, ESW-103, ESW-147, ESW-151, ESW-214" in {
      val name: String            = "primary"
      val expectedSequencerPrefix = Prefix("ESW.ESW.primary@esw@darknight")
      val sequenceComponentPrefix = Prefix(Subsystem.ESW, name)

      // start Sequence Component
      SequencerApp.main(Array("seqcomp", "-s", "esw", "-n", name))

      // verify Sequence component is started and registered with location service
      val sequenceCompLocation: AkkaLocation = resolveSequenceComponentLocation(sequenceComponentPrefix)

      sequenceCompLocation.connection shouldEqual AkkaConnection(
        ComponentId(sequenceComponentPrefix, ComponentType.SequenceComponent)
      )
      sequenceCompLocation.prefix shouldEqual Prefix("ESW.primary")

      // LoadScript
      val seqCompRef = sequenceCompLocation.uri.toActorRef.unsafeUpcast[SequenceComponentMsg]
      val probe      = TestProbe[ScriptResponse]
      seqCompRef ! LoadScript("esw", "darknight", probe.ref)

      // verify that loaded sequencer is started and able to process sequence command
      val response          = probe.expectMessageType[ScriptResponse]
      val sequencerLocation = response.response.rightValue

      //verify sequencerName has SequenceComponentName
      val actualSequencerName: String = sequencerLocation.prefix.componentName
      actualSequencerName shouldEqual expectedSequencerPrefix

      // verify Sequencer is started and registered with location service with expected prefix
      val sequencerLocationCheck: AkkaLocation = resolveSequencerLocation(expectedSequencerPrefix)
      sequencerLocationCheck shouldEqual sequencerLocation

      val commandService = new SequencerCommandServiceImpl(sequencerLocation)
      val setup          = Setup(Prefix("wfos.home.datum"), CommandName("command-1"), None)
      val sequence       = Sequence(setup)
      commandService.submitAndWait(sequence).futureValue shouldBe a[Completed]

      // UnloadScript
      val probe2 = TestProbe[Done]
      seqCompRef ! UnloadScript(probe2.ref)
      probe2.expectMessage(Done)
    }

    "start sequence component and register with automatically generated random uniqueIDs if prefix is not provided| ESW-144" in {
      val subsystem = "ESW"
      SequencerApp.main(Array("seqcomp", "-s", subsystem))

      val sequenceComponentLocation = locationService.list(ComponentType.SequenceComponent).futureValue.head

      //assert that componentName and prefix contain subsystem provided
      sequenceComponentLocation.prefix.value.contains("ESW.ESW_") shouldEqual true
    }

    "start sequence component concurrently and register with automatically generated random uniqueIDs if prefix is not provided| ESW-144" in {
      val subsystem = "ESW"

      //register sequence component with same subsystem concurrently
      Future { SequencerApp.main(Array("seqcomp", "-s", subsystem)) }
      Future { SequencerApp.main(Array("seqcomp", "-s", subsystem)) }
      Future { SequencerApp.main(Array("seqcomp", "-s", subsystem)) }

      //Wait till futures registering sequence component complete
      Thread.sleep(5000)

      val sequenceComponentLocations = locationService.list(ComponentType.SequenceComponent).futureValue

      //assert if all 3 sequence components are registered
      locationService.list(ComponentType.SequenceComponent).futureValue.length shouldEqual 3
      sequenceComponentLocations.foreach { location =>
        {
          //assert that componentName and prefix contain subsystem provided
          location.prefix.value.contains("ESW.ESW_") shouldEqual true
        }
      }
    }

    "return ScriptError when script configuration is not provided| ESW-102, ESW-136" in {
      val subsystem               = "ESW"
      val name                    = "primary"
      val invalidPackageId        = "invalid_package"
      val observingMode           = "darknight"
      val sequenceComponentPrefix = Prefix(ESW, name)

      // start Sequence Component
      SequencerApp.main(Array("seqcomp", "-s", subsystem, "-n", name))

      // verify Sequence component is started and registered with location service
      val sequenceCompLocation: AkkaLocation = resolveSequenceComponentLocation(sequenceComponentPrefix)

      sequenceCompLocation.connection shouldEqual AkkaConnection(ComponentId(Prefix(ESW, name), ComponentType.SequenceComponent))

      val timeout = Timeout(10.seconds)
      // LoadScript
      val seqCompRef: ActorRef[SequenceComponentMsg] = sequenceCompLocation.uri.toActorRef.unsafeUpcast[SequenceComponentMsg]
      val loadScriptResponse: Future[ScriptResponse] =
        seqCompRef.ask((ref: ActorRef[ScriptResponse]) => LoadScript(invalidPackageId, observingMode, ref))(
          timeout,
          schedulerFromActorSystem
        )

      val response: Either[ScriptError, AkkaLocation] = loadScriptResponse.futureValue.response

      response match {
        case Left(v) =>
          v shouldEqual ScriptError(s"Script configuration missing for $invalidPackageId with $observingMode")
        case Right(_) => throw new RuntimeException("test failed as this test expects ScriptError")
      }
    }
  }

  "Sequencer command" must {
    "start sequencer with provided id, mode and register it with location service | ESW-102, ESW-136, ESW-103, ESW-147, ESW-151" in {
      val subsystem     = "ESW"
      val name          = "primary"
      val packageId     = "esw"
      val observingMode = "darknight"
      val sequencerName = "ESW.primary@esw@darknight"

      // start Sequencer"
      SequencerApp.main(Array("sequencer", "-s", subsystem, "-n", name, "-i", packageId, "-m", observingMode))

      // verify sequence component is started
      val sequenceComponentPrefix   = Prefix(s"$subsystem.$name")
      val sequenceComponentLocation = resolveSequenceComponentLocation(sequenceComponentPrefix)
      sequenceComponentLocation.connection.componentId.prefix.componentName shouldBe sequenceComponentPrefix

      // verify that sequencer is started and able to process sequence command
      val connection        = AkkaConnection(ComponentId(Prefix(ESW, sequencerName), ComponentType.Sequencer))
      val sequencerLocation = locationService.resolve(connection, 5.seconds).futureValue.value

      sequencerLocation.prefix.componentName shouldBe sequencerName

      val commandService = new SequencerCommandServiceImpl(sequencerLocation)
      val setup          = Setup(Prefix("wfos.home.datum"), CommandName("command-1"), None)
      val sequence       = Sequence(setup)
      commandService.submitAndWait(sequence).futureValue shouldBe a[Completed]
    }

    "start sequencer with provided mandatory subsystem, mode register it with location service | ESW-103" in {
      val subsystem     = "ESW"
      val observingMode = "darknight"

      // start Sequencer
      SequencerApp.main(Array("sequencer", "-s", subsystem, "-m", observingMode))

      val sequenceComponentLocation = locationService.list(ComponentType.SequenceComponent).futureValue.head

      //assert that componentName and prefix contain subsystem provided
      sequenceComponentLocation.prefix.value.contains("ESW.ESW_") shouldEqual true

      val sequenceComponentName = sequenceComponentLocation.prefix.componentName

      //sequencer prefix will have sequence component prefix and optional packageId is defaulted to subsystem
      val sequencerName = s"$sequenceComponentName@esw@darknight"
      // verify that sequencer is started and able to process sequence command
      val sequencerLocation = resolveSequencerLocation(Prefix(ESW, sequencerName))

      sequencerLocation.prefix.componentName shouldBe sequencerName
    }

    "throw exception if ScriptError is returned | ESW-102, ESW-136" in {
      val subsystem        = "ESW"
      val name             = "primary"
      val invalidPackageId = "invalid package"
      val observingMode    = "darknight"

      val exception = intercept[RuntimeException] {
        SequencerApp.main(Array("sequencer", "-s", subsystem, "-n", name, "-i", invalidPackageId, "-m", observingMode))
      }

      exception.getMessage shouldEqual s"Failed to start with error: ScriptError(Script configuration missing for $invalidPackageId with $observingMode)"
    }
  }
}
