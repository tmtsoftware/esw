package esw.ocs.cli

import akka.actor.typed.ActorSystem
import csw.location.api.scaladsl.LocationService
import csw.location.client.scaladsl.HttpLocationServiceFactory
import csw.location.model.scaladsl.Connection.AkkaConnection
import csw.location.model.scaladsl.{ComponentId, ComponentType}
import csw.testkit.scaladsl.ScalaTestFrameworkTestKit
import esw.ocs.BaseTestSuite
import esw.ocs.cli.SequencerAppCommand.{SequenceComponent, Sequencer}

import scala.concurrent.duration.DurationInt

class SequencerAppTest extends ScalaTestFrameworkTestKit with BaseTestSuite {
  import frameworkTestKit._
  implicit val typedSystem: ActorSystem[_]         = actorSystem
  private val testLocationService: LocationService = HttpLocationServiceFactory.makeLocalClient

  "SequenceComponent command" must {
    "start sequence component with provided name and register it with location service | ESW-103, ESW-147" in {
      val seqComName = "testSequencerComponent"
      SequencerApp.run(SequenceComponent(seqComName))

      val connection        = AkkaConnection(ComponentId(seqComName, ComponentType.Service))
      val sequencerLocation = testLocationService.resolve(connection, 5.seconds).futureValue.get

      sequencerLocation.connection shouldBe connection
    }
  }

  "Sequencer command" must {
    "start sequencer with provided id, mode and register it with location service | ESW-103, ESW-147" in {
      val sequencerId   = "testSequencerId1"
      val observingMode = "testObservingMode1"
      val sequencerName = s"$sequencerId@$observingMode"
      SequencerApp.run(Sequencer(sequencerId, observingMode))

      val connection        = AkkaConnection(ComponentId(sequencerName, ComponentType.Sequencer))
      val sequencerLocation = testLocationService.resolve(connection, 5.seconds).futureValue.value

      sequencerLocation.connection.componentId.name shouldBe sequencerName
    }
  }
}
