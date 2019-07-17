package esw.integration.test.ocs.framework

import akka.actor
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.scaladsl.adapter.TypedActorSystemOps
import akka.http.scaladsl.Http
import akka.stream.typed.scaladsl.ActorMaterializer
import csw.location.api.scaladsl.LocationService
import csw.location.client.scaladsl.HttpLocationServiceFactory
import csw.location.model.scaladsl.Connection.AkkaConnection
import csw.location.model.scaladsl.{ComponentId, ComponentType}
import csw.testkit.LocationTestKit
import esw.ocs.framework.internal.SequencerWiring
import esw.template.http.server.BaseTestSuite
import esw.template.http.server.TestFutureExtensions.RichFuture

import scala.concurrent.duration.DurationDouble

class SequencerWiringTest extends BaseTestSuite {
  val testkit = LocationTestKit()

  implicit val system: ActorSystem[_]                = ActorSystem(Behaviors.empty, "test")
  implicit val untypedActorSystem: actor.ActorSystem = system.toUntyped
  implicit val mat: ActorMaterializer                = ActorMaterializer()
  private val testLocationService: LocationService   = HttpLocationServiceFactory.makeLocalClient

  override def beforeAll(): Unit = {
    testkit.startLocationServer()
  }

  override def afterAll(): Unit = {
    Http().shutdownAllConnectionPools().await
    testkit.shutdownLocationServer()
    system.terminate()
    system.whenTerminated.await
  }

  "SequencerWiring" must {
    "should start sequencer and register with location service | ESW-103" in {
      val sequencerId   = "testSequencerId1"
      val observingMode = "testObservingMode1"
      val sequencerName = s"$sequencerId@$observingMode"
      val wiring        = new SequencerWiring(sequencerId, observingMode)

      val triedLocation = wiring.start()

      val connection        = AkkaConnection(ComponentId(sequencerName, ComponentType.Sequencer))
      val sequencerLocation = testLocationService.resolve(connection, 5.seconds).await.get

      triedLocation.toOption.get.connection shouldBe connection
      sequencerLocation.connection.componentId.name shouldBe sequencerName

      // cleanup
      wiring.shutDown()
    }

    "should shutdown running Sequencer | ESW-103" in {
      val sequencerId   = "testSequencerId1"
      val observingMode = "testObservingMode1"
      val sequencerName = s"$sequencerId@$observingMode"
      val wiring        = new SequencerWiring(sequencerId, observingMode)

      wiring.start()

      val connection        = AkkaConnection(ComponentId(sequencerName, ComponentType.Sequencer))
      val sequencerLocation = testLocationService.resolve(connection, 5.seconds).await.get
      // test sequencer is registered with location service
      sequencerLocation.connection.componentId.name shouldBe sequencerName

      wiring.shutDown()

      testLocationService.resolve(connection, 5.seconds).await shouldBe None
    }
  }
}
