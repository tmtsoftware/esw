package esw.http.core.utils

import java.net.URI

import akka.actor.typed.{ActorSystem, SpawnProtocol}
import csw.location.api.scaladsl.LocationService
import csw.location.models.Connection.{AkkaConnection, HttpConnection}
import csw.location.models.{AkkaLocation, ComponentId, HttpLocation}
import esw.http.core.BaseTestSuite
import esw.http.core.wiring.ActorRuntime
import org.mockito.Mockito.{verify, when}

import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, Future}

class ComponentFactoryTest extends BaseTestSuite {
  private val actorSystem: ActorSystem[SpawnProtocol.Command] = ActorSystem(SpawnProtocol(), "test")
  private val actorRuntime                                    = new ActorRuntime(actorSystem)
  import actorRuntime._

  private val locationService       = mock[LocationService]
  private val akkaComponentId       = mock[ComponentId]
  private val httpComponentId       = mock[ComponentId]
  private val akkaConnection        = AkkaConnection(akkaComponentId)
  private val httpConnection        = HttpConnection(httpComponentId)
  private val commandServiceFactory = mock[ICommandServiceFactory]

  private val akkaLocation = AkkaLocation(akkaConnection, new URI("actor-path"))
  private val httpLocation = HttpLocation(httpConnection, new URI("actor-path"))

  when(locationService.resolve(akkaConnection, 5.seconds)).thenReturn(Future.successful(Some(akkaLocation)))

  when(locationService.resolve(AkkaConnection(httpComponentId), 5.seconds)).thenReturn(Future.successful(None))
  when(locationService.resolve(httpConnection, 5.seconds)).thenReturn(Future.successful(Some(httpLocation)))

  override protected def afterAll(): Unit = {
    actorSystem.terminate
    Await.result(actorSystem.whenTerminated, 10.seconds)
  }

  "resolveLocation" must {
    "resolve akka components using location service | ESW-91" in {
      val componentFactory = new ComponentFactory(locationService, commandServiceFactory)
      componentFactory
        .resolveLocation(akkaComponentId)(_ shouldBe akkaLocation)
        .futureValue
    }

    "fallback to http location when akka component not registered | ESW-91, ESW-258" in {
      val componentFactory = new ComponentFactory(locationService, commandServiceFactory)
      componentFactory
        .resolveLocation(httpComponentId)(_ shouldBe httpLocation)
        .futureValue
    }
  }

  "commandService" must {
    "make instance of command service | ESW-91" in {
      val componentFactory = new ComponentFactory(locationService, commandServiceFactory)
      componentFactory.commandService(akkaComponentId)
      eventually(verify(commandServiceFactory).make(akkaLocation))
    }
  }
}
