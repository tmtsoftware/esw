package esw.template.http.server.csw.utils

import java.net.URI

import akka.actor.testkit.typed.scaladsl.TestProbe
import akka.actor.typed.{ActorRef, ActorSystem, SpawnProtocol}
import csw.command.client.ICommandServiceFactory
import csw.command.client.messages.ComponentMessage
import csw.location.api.models.Connection.AkkaConnection
import csw.location.api.models.{AkkaLocation, ComponentId, ComponentType}
import csw.location.api.scaladsl.LocationService
import csw.params.core.models.Prefix
import esw.template.http.server.BaseTestSuit
import esw.template.http.server.wiring.ActorRuntime

import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, Future}

class ComponentFactoryTest extends BaseTestSuit {
  val actorSystem: ActorSystem[SpawnProtocol] = ActorSystem(SpawnProtocol.behavior, "test")
  val actorRuntime                            = new ActorRuntime(actorSystem)
  import actorRuntime._

  override protected def afterAll(): Unit = {
    actorSystem.terminate
    Await.result(actorSystem.whenTerminated, 10.seconds)
    super.afterAll()
  }

  "ComponentFactory" must {
    "resolve components using location service | ESW-91" in {
      val locationService                      = mock[LocationService]
      val componentName                        = "testComponent"
      val componentType                        = mock[ComponentType]
      val connection                           = AkkaConnection(ComponentId(componentName, componentType))
      val commandServiceFactory                = mock[ICommandServiceFactory]
      val actorRef: ActorRef[ComponentMessage] = TestProbe[ComponentMessage].ref

      val location         = AkkaLocation(connection, mock[Prefix], new URI("actor-path"), actorRef)
      val expectedLocation = Future.successful(Some(location))

      when(locationService.resolve(connection, 5.seconds)).thenReturn(expectedLocation)

      val componentFactory = new ComponentFactory(locationService, commandServiceFactory)
      componentFactory.resolve(componentName, componentType) { actualLocation =>
        actualLocation shouldBe location
      }
    }

    "make command service for assembly | ESW-91" in {
      val locationService                      = mock[LocationService]
      val commandServiceFactory                = mock[ICommandServiceFactory]
      val componentFactory                     = new ComponentFactory(locationService, commandServiceFactory)
      val componentName                        = "testComponent"
      val componentType                        = ComponentType.Assembly
      val connection                           = AkkaConnection(ComponentId(componentName, componentType))
      val actorRef: ActorRef[ComponentMessage] = TestProbe[ComponentMessage].ref

      val location         = AkkaLocation(connection, mock[Prefix], new URI("actor-path"), actorRef)
      val expectedLocation = Future.successful(Some(location))

      when(locationService.resolve(connection, 5.seconds)).thenReturn(expectedLocation)

      componentFactory.commandService(componentName, componentType)
      verify(commandServiceFactory).make(location)
    }
  }
}
