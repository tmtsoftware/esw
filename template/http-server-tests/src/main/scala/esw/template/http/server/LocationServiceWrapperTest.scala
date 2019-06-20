package esw.template.http.server

import java.net.URI

import akka.actor.testkit.typed.scaladsl.TestProbe
import akka.actor.typed.{ActorRef, ActorSystem, SpawnProtocol}
import csw.command.client.messages.ComponentMessage
import csw.location.api.models.Connection.AkkaConnection
import csw.location.api.models._
import csw.location.api.scaladsl.LocationService
import csw.location.client.ActorSystemFactory
import csw.params.core.models.Prefix
import esw.template.http.server.commons.ActorRuntime
import org.mockito.MockitoSugar
import org.scalatest.{BeforeAndAfterAll, FunSuite, Matchers}

import scala.concurrent.duration.DurationDouble
import scala.concurrent.{Await, Future}

class LocationServiceWrapperTest extends FunSuite with MockitoSugar with Matchers with BeforeAndAfterAll {

  val actorSystem: ActorSystem[SpawnProtocol] = ActorSystemFactory.remote(SpawnProtocol.behavior, "test")
  val actorRuntime                            = new ActorRuntime(actorSystem)
  import actorRuntime._
  val actorRef: ActorRef[ComponentMessage] = TestProbe[ComponentMessage].ref

  test("ESW-91 | should register using location service") {
    val componentId                = mock[ComponentId]
    val prefix                     = mock[Prefix]
    val expectedRegistrationResult = Future.successful(mock[RegistrationResult])
    val akkaRegistration           = AkkaRegistration(AkkaConnection(componentId), prefix, actorRef)
    val locationService            = mock[LocationService]

    when(locationService.register(akkaRegistration)).thenReturn(expectedRegistrationResult)

    val locationServiceWrapper   = new LocationServiceWrapper(locationService)
    val actualRegistrationResult = locationServiceWrapper.register(prefix, componentId, actorRef)

    actualRegistrationResult shouldBe expectedRegistrationResult
  }

  test("ESW-91 | should resolve using location service") {

    val locationService = mock[LocationService]
    val componentName   = "testComponent"
    val componentType   = mock[ComponentType]
    val connection      = AkkaConnection(ComponentId(componentName, componentType))

    val location         = AkkaLocation(connection, mock[Prefix], new URI("actor-path"), actorRef)
    val expectedLocation = Future.successful(Some(location))

    when(locationService.resolve(connection, 5.seconds)).thenReturn(expectedLocation)

    val locationServiceWrapper = new LocationServiceWrapper(locationService)
    locationServiceWrapper.resolve(componentName, componentType) { actualLocation =>
      actualLocation shouldBe location
    }
  }

  override protected def afterAll(): Unit = {
    actorSystem.terminate
    Await.result(actorSystem.whenTerminated, 10.seconds)
    super.afterAll()
  }
}
