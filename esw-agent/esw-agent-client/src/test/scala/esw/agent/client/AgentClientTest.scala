package esw.agent.client

import java.net.URI

import akka.actor.{ActorSystem, typed}
import akka.testkit.TestKit
import csw.location.api.scaladsl.LocationService
import csw.prefix.models.Prefix
import org.scalatestplus.mockito.MockitoSugar
import akka.actor.typed.scaladsl.adapter._
import csw.location.models.{AkkaLocation, ComponentId}
import csw.location.models.ComponentType.Machine
import csw.location.models.Connection.AkkaConnection

import scala.concurrent.duration.DurationLong
import org.mockito.Mockito.when
import org.scalatest.concurrent.ScalaFutures.convertScalaFuture
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

import scala.concurrent.Future

class AgentClientTest extends TestKit(ActorSystem("test1")) with WordSpecLike with Matchers with BeforeAndAfterAll {
  implicit val actorSystem: typed.ActorSystem[_] = system.toTyped
  "make" should {
    "resolve the given prefix and return a new instance of AgentClient  | ESW-237" in {
      val locationService: LocationService = MockitoSugar.mock[LocationService]
      val prefix                           = Prefix("esw.test1")
      val akkaConnection                   = AkkaConnection(ComponentId(prefix, Machine))
      val agentLocation = AkkaLocation(
        akkaConnection,
        URI.create("akka://abc")
      )
      when(locationService.resolve(akkaConnection, 5.seconds)).thenReturn(
        Future.successful(
          Some(
            agentLocation
          )
        )
      )
      AgentClient.make(prefix, locationService).futureValue
    }
    "return a failed future when location service cant resolve agent  | ESW-237" in {
      val locationService: LocationService = MockitoSugar.mock[LocationService]
      val prefix                           = Prefix("esw.test1")
      val akkaConnection                   = AkkaConnection(ComponentId(prefix, Machine))
      when(locationService.resolve(akkaConnection, 5.seconds)).thenReturn(
        Future.successful(None)
      )
      val exception = intercept[RuntimeException](AgentClient.make(prefix, locationService).futureValue)
      exception.getCause.getMessage should ===(s"could not resolve $prefix")
    }
    "return a failed future when location service call fails  | ESW-237" in {
      val locationService: LocationService = MockitoSugar.mock[LocationService]
      val prefix                           = Prefix("esw.test1")
      val akkaConnection                   = AkkaConnection(ComponentId(prefix, Machine))
      when(locationService.resolve(akkaConnection, 5.seconds)).thenReturn(
        Future.failed(new RuntimeException("boom"))
      )
      val exception = intercept[RuntimeException](AgentClient.make(prefix, locationService).futureValue)
      exception.getCause.getMessage should ===(s"boom")
    }
  }

  override def afterAll(): Unit = shutdown()
}
