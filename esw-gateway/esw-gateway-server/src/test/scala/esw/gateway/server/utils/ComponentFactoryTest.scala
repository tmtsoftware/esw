package esw.gateway.server.utils

import java.net.URI
import org.apache.pekko.actor.typed.{ActorSystem, SpawnProtocol}
import csw.location.api.models.Connection.{PekkoConnection, HttpConnection}
import csw.location.api.models.{PekkoLocation, ComponentId, HttpLocation, Metadata}
import csw.location.api.scaladsl.LocationService
import org.mockito.Mockito.{verify, when}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.concurrent.Eventually
import org.scalatest.concurrent.ScalaFutures.*
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar

import scala.concurrent.Future
import scala.concurrent.duration.DurationInt

class ComponentFactoryTest extends AnyWordSpec with MockitoSugar with Matchers with BeforeAndAfterAll with Eventually {
  private implicit val actorSystem: ActorSystem[SpawnProtocol.Command] = ActorSystem(SpawnProtocol(), "test")

  private val locationService       = mock[LocationService]
  private val pekkoComponentId      = mock[ComponentId]
  private val httpComponentId       = mock[ComponentId]
  private val pekkoConnection       = PekkoConnection(pekkoComponentId)
  private val httpConnection        = HttpConnection(httpComponentId)
  private val commandServiceFactory = mock[ICommandServiceFactory]

  private val pekkoLocation = PekkoLocation(pekkoConnection, new URI("actor-path"), Metadata.empty)
  private val httpLocation  = HttpLocation(httpConnection, new URI("actor-path"), Metadata.empty)

  when(locationService.resolve(pekkoConnection, 3.seconds)).thenReturn(Future.successful(Some(pekkoLocation)))

  when(locationService.resolve(PekkoConnection(httpComponentId), 3.seconds)).thenReturn(Future.successful(None))
  when(locationService.resolve(httpConnection, 3.seconds)).thenReturn(Future.successful(Some(httpLocation)))

  override implicit def patienceConfig: PatienceConfig = PatienceConfig(5.seconds)

  override protected def afterAll(): Unit = {
    actorSystem.terminate()
    actorSystem.whenTerminated.futureValue
  }

  "resolveLocation" must {
    "resolve pekko components using location service | ESW-91" in {
      val componentFactory = new ComponentFactory(locationService, commandServiceFactory)
      componentFactory
        .resolveLocation(pekkoComponentId)(_ shouldBe pekkoLocation)
        .futureValue
    }

    "fallback to http location when pekko component not registered | ESW-91, ESW-258" in {
      val componentFactory = new ComponentFactory(locationService, commandServiceFactory)
      componentFactory
        .resolveLocation(httpComponentId)(_ shouldBe httpLocation)
        .futureValue
    }
  }

  "commandService" must {
    "make instance of command service | ESW-91" in {
      val componentFactory = new ComponentFactory(locationService, commandServiceFactory)
      componentFactory.commandService(pekkoComponentId)
      eventually(verify(commandServiceFactory).make(pekkoLocation))
    }
  }
}
