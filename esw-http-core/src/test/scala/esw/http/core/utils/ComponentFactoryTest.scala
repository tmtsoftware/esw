package esw.http.core.utils

import java.net.URI

import akka.actor.typed.{ActorSystem, SpawnProtocol}
import csw.location.api.scaladsl.LocationService
import csw.location.models.{AkkaLocation, ComponentId, ComponentType}
import csw.location.models.Connection.AkkaConnection
import csw.params.core.models.{Prefix, Subsystem}
import esw.http.core.BaseTestSuite
import esw.http.core.wiring.ActorRuntime
import org.mockito.Mockito.{verify, when}

import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, Future}

class ComponentFactoryTest extends BaseTestSuite {
  val actorSystem: ActorSystem[SpawnProtocol.Command] = ActorSystem(SpawnProtocol(), "test")
  val actorRuntime                                    = new ActorRuntime(actorSystem)
  import actorRuntime._

  override protected def afterAll(): Unit = {
    actorSystem.terminate
    Await.result(actorSystem.whenTerminated, 10.seconds)
    super.afterAll()
  }

  "ComponentFactory" must {
    "resolve components using location service | ESW-91" in {
      val locationService       = mock[LocationService]
      val componentName         = "testComponent"
      val componentType         = mock[ComponentType]
      val prefix                = Prefix(Subsystem.ESW, componentName)
      val componentId           = ComponentId(prefix, componentType)
      val connection            = AkkaConnection(componentId)
      val commandServiceFactory = mock[ICommandServiceFactory]

      val location         = AkkaLocation(connection, new URI("actor-path"))
      val expectedLocation = Future.successful(Some(location))

      when(locationService.resolve(connection, 5.seconds)).thenReturn(expectedLocation)

      val componentFactory = new ComponentFactory(locationService, commandServiceFactory)
      componentFactory.resolve(componentId) { actualLocation =>
        actualLocation shouldBe location
      }
    }

    "make command service for assembly | ESW-91" in {
      val locationService       = mock[LocationService]
      val commandServiceFactory = mock[ICommandServiceFactory]
      val componentFactory      = new ComponentFactory(locationService, commandServiceFactory)
      val componentName         = "testComponent"
      val prefix                = Prefix(Subsystem.ESW, componentName)
      val componentType         = ComponentType.Assembly
      val componentId           = ComponentId(prefix, componentType)
      val connection            = AkkaConnection(componentId)

      val location         = AkkaLocation(connection, new URI("actor-path"))
      val expectedLocation = Future.successful(Some(location))

      when(locationService.resolve(connection, 5.seconds)).thenReturn(expectedLocation)

      componentFactory.commandService(componentId)
      eventually(verify(commandServiceFactory).make(location))
    }
  }
}
