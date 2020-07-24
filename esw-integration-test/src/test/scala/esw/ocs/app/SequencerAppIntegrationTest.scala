package esw.ocs.app

import akka.actor.testkit.typed.scaladsl.TestProbe
import akka.actor.typed.ActorRef
import akka.actor.typed.scaladsl.AskPattern._
import akka.util.Timeout
import csw.command.client.SequencerCommandServiceImpl
import csw.location.api.extensions.URIExtension.RichURI
import csw.location.api.models.Connection.AkkaConnection
import csw.location.api.models.{AkkaLocation, ComponentId, ComponentType}
import csw.params.commands.CommandResponse.Completed
import csw.params.commands.{CommandName, Sequence, Setup}
import csw.prefix.models.Subsystem.{CSW, ESW}
import csw.prefix.models.{Prefix, Subsystem}
import esw.ocs.api.actor.messages.SequenceComponentMsg
import esw.ocs.api.actor.messages.SequenceComponentMsg.{LoadScript, UnloadScript}
import esw.ocs.api.models.ObsMode
import esw.ocs.api.protocol.ScriptError
import esw.ocs.api.protocol.SequenceComponentResponse.{Ok, OkOrUnhandled, ScriptResponseOrUnhandled, SequencerLocation}
import esw.ocs.testkit.EswTestKit

import scala.concurrent.Future
import scala.concurrent.duration.DurationInt

class SequencerAppIntegrationTest extends EswTestKit {

  override def afterEach(): Unit = locationService.unregisterAll()

  "SequenceComponent command" must {
    "start sequence component with provided subsystem and prefix and register it with location service | ESW-102, ESW-136, ESW-103, ESW-147, ESW-151, ESW-214" in {
      val name: String            = "primary"
      val expectedSequencerPrefix = Prefix(ESW, "darknight")
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
      val probe      = TestProbe[ScriptResponseOrUnhandled]()
      seqCompRef ! LoadScript(ESW, ObsMode("darknight"), probe.ref)

      // verify that loaded sequencer is started and able to process sequence command
      val response          = probe.expectMessageType[SequencerLocation]
      val sequencerLocation = response.location

      //verify sequencerName has SequenceComponentName
      val actualSequencerPrefix: Prefix = sequencerLocation.prefix
      actualSequencerPrefix shouldEqual expectedSequencerPrefix

      // verify Sequencer is started and registered with location service with expected prefix
      val sequencerLocationCheck: AkkaLocation = resolveSequencerLocation(expectedSequencerPrefix)
      sequencerLocationCheck shouldEqual sequencerLocation

      val commandService = new SequencerCommandServiceImpl(sequencerLocation)
      val setup          = Setup(Prefix("wfos.home.datum"), CommandName("command-1"), None)
      val sequence       = Sequence(setup)
      commandService.submitAndWait(sequence).futureValue shouldBe a[Completed]

      // UnloadScript
      val probe2 = TestProbe[OkOrUnhandled]()
      seqCompRef ! UnloadScript(probe2.ref)
      probe2.expectMessage(Ok)
    }

    "start sequence component and register with automatically generated random uniqueIDs if prefix is not provided| ESW-144, ESW-279" in {
      val subsystem = "ESW"
      SequencerApp.main(Array("seqcomp", "-s", subsystem))

      val sequenceComponentLocation = locationService.list(ComponentType.SequenceComponent).futureValue.head

      //assert that componentName and prefix contain subsystem provided
      sequenceComponentLocation.prefix.toString.contains("ESW.ESW_") shouldEqual true
    }

    "start sequence component concurrently and register with automatically generated random uniqueIDs if prefix is not provided| ESW-144, ESW-279" in {
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
        //assert that componentName and prefix contain subsystem provided
        location.prefix.toString.contains("ESW.ESW_") shouldEqual true
      }
    }

    "return ScriptError when script configuration is not provided| ESW-102, ESW-136" in {
      val subsystem               = "ESW"
      val name                    = "primary"
      val unexpectedSubsystem     = CSW
      val obsMode                 = ObsMode("darknight")
      val sequenceComponentPrefix = Prefix(ESW, name)

      // start Sequence Component
      SequencerApp.main(Array("seqcomp", "-s", subsystem, "-n", name))

      // verify Sequence component is started and registered with location service
      val sequenceCompLocation: AkkaLocation = resolveSequenceComponentLocation(sequenceComponentPrefix)

      sequenceCompLocation.connection shouldEqual AkkaConnection(ComponentId(Prefix(ESW, name), ComponentType.SequenceComponent))

      val timeout = Timeout(10.seconds)
      // LoadScript
      val seqCompRef: ActorRef[SequenceComponentMsg] = sequenceCompLocation.uri.toActorRef.unsafeUpcast[SequenceComponentMsg]
      val loadScriptResponse: ScriptResponseOrUnhandled =
        seqCompRef
          .ask((ref: ActorRef[ScriptResponseOrUnhandled]) => LoadScript(unexpectedSubsystem, obsMode, ref))(
            timeout,
            schedulerFromActorSystem
          )
          .futureValue

      loadScriptResponse match {
        case error: ScriptError.LoadingScriptFailed =>
          error shouldEqual ScriptError.LoadingScriptFailed(
            s"Script configuration missing for [${unexpectedSubsystem.name}] with [${obsMode.name}]"
          )
        case _ => throw new RuntimeException("test failed as this test expects ScriptError")
      }
    }

    "throw exception if location service gives error while registering sequence component" in {
      val name: String            = "primary"
      val sequenceComponentPrefix = Prefix(Subsystem.ESW, name)

      // start Sequence Component
      SequencerApp.main(Array("seqcomp", "-s", "esw", "-n", name))

      // verify Sequence component is started and registered with location service
      val sequenceCompLocation: AkkaLocation = resolveSequenceComponentLocation(sequenceComponentPrefix)
      sequenceCompLocation.prefix shouldEqual Prefix("ESW.primary")

      // assert that exception is thrown when start Sequence Component with same name
      intercept[RuntimeException](SequencerApp.main(Array("seqcomp", "-s", "esw", "-n", name)))
    }
  }

  "Sequencer command" must {
    "start sequencer with provided id, mode and register it with location service | ESW-102, ESW-136, ESW-103, ESW-147, ESW-151" in {
      val subsystem          = "ESW"
      val name               = "primary"
      val sequencerSubsystem = "esw"
      val obsMode            = "darknight"

      // start Sequencer"
      SequencerApp.main(Array("sequencer", "-s", subsystem, "-n", name, "-i", sequencerSubsystem, "-m", obsMode))

      // verify sequence component is started and can be resolved
      val sequenceComponentPrefix = Prefix(s"$subsystem.$name")
      resolveSequenceComponentLocation(sequenceComponentPrefix)

      // verify that sequencer is started and able to process sequence command
      val connection        = AkkaConnection(ComponentId(Prefix(ESW, obsMode), ComponentType.Sequencer))
      val sequencerLocation = locationService.resolve(connection, 5.seconds).futureValue.value

      val commandService = new SequencerCommandServiceImpl(sequencerLocation)
      val setup          = Setup(Prefix("wfos.home.datum"), CommandName("command-1"), None)
      val sequence       = Sequence(setup)
      commandService.submitAndWait(sequence).futureValue shouldBe a[Completed]
    }

    "start sequencer with provided mandatory subsystem, mode register it with location service | ESW-103, ESW-279" in {
      val subsystem = "ESW"
      val obsMode   = "darknight"

      // start Sequencer
      SequencerApp.main(Array("sequencer", "-s", subsystem, "-m", obsMode))

      val sequenceComponentLocation = locationService.list(ComponentType.SequenceComponent).futureValue.head

      //assert that componentName and prefix contain subsystem provided
      sequenceComponentLocation.prefix.toString.contains("ESW.ESW_") shouldEqual true

      // verify that sequencer is started and able to process sequence command
      resolveSequencerLocation(Prefix(ESW, obsMode))
    }

    "throw exception if ScriptError is returned | ESW-102, ESW-136, ESW-279" in {
      val subsystem           = "esw"
      val name                = "primary"
      val unexpectedSubsystem = "CSW"
      val obsMode             = "darknight"

      val exception = intercept[RuntimeException] {
        SequencerApp.main(Array("sequencer", "-s", subsystem, "-n", name, "-i", unexpectedSubsystem, "-m", obsMode))
      }

      exception.getMessage shouldEqual s"Failed to start with error: Script configuration missing for [$unexpectedSubsystem] with [$obsMode]"
    }

    "start sequencer in simulation mode | ESW-149" in {
      val subsystem = "esw"
      val name      = "primary"
      val obsMode   = "random"

      //there is no script for esw.random mode but sequencer should start with esw.random as a simulation script
      //starting sequencer in simulation mode
      SequencerApp.main(Array("sequencer", "-s", subsystem, "-n", name, "-m", obsMode, "--simulation"))

      // assert sequencer has started
      resolveSequencerLocation(Prefix(ESW, obsMode))
    }
  }
}
