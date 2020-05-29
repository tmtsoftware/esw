package esw.ocs.dsl.script.utils

import java.util.concurrent.TimeUnit

import akka.actor.testkit.typed.scaladsl.TestProbe
import akka.actor.typed.{ActorSystem, SpawnProtocol}
import csw.command.client.messages.ComponentMessage
import csw.location.api.extensions.ActorExtension._
import csw.location.api.models.Connection.AkkaConnection
import csw.location.api.models.{AkkaLocation, ComponentId, ComponentType}
import csw.prefix.models.Prefix
import csw.prefix.models.Subsystem.ESW
import esw.commons.utils.location.EswLocationError.LocationNotFound
import esw.commons.utils.location.LocationServiceUtil
import esw.commons.{BaseTestSuite, Timeouts}

import scala.concurrent.{ExecutionException, Future}

class CommandUtilTest extends BaseTestSuite {
  implicit val testSystem: ActorSystem[SpawnProtocol.Command] = ActorSystem(SpawnProtocol(), "testSystem")

  private val locationServiceUtil = mock[LocationServiceUtil]
  private val prefix              = Prefix(ESW, "trombone")
  private val componentType       = ComponentType.Assembly
  private val connection          = AkkaConnection(ComponentId(prefix, componentType))
  private val testRef             = TestProbe[ComponentMessage]().ref
  private val location            = AkkaLocation(connection, testRef.toURI)

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    reset(locationServiceUtil)
  }

  private val commandUtil = new CommandUtil(locationServiceUtil)
  "jResolveAkkaLocation" must {
    "return completion stage for akka location" in {
      when(locationServiceUtil.resolve(connection, Timeouts.DefaultTimeout)).thenReturn(Future.successful(Right(location)))

      val completableLocation = commandUtil.jResolveAkkaLocation(prefix, componentType)

      completableLocation.toCompletableFuture.get(10, TimeUnit.SECONDS) shouldBe location
      verify(locationServiceUtil).resolve(connection, Timeouts.DefaultTimeout)
    }

    "throw exception if location service returns error" in {
      when(locationServiceUtil.resolve(connection, Timeouts.DefaultTimeout))
        .thenReturn(Future.successful(Left(LocationNotFound("Error while resolving location"))))

      val message = intercept[ExecutionException](
        commandUtil.jResolveAkkaLocation(prefix, componentType).toCompletableFuture.get(10, TimeUnit.SECONDS)
      ).getLocalizedMessage
      "esw.commons.utils.location.EswLocationError$LocationNotFound" shouldBe message
      verify(locationServiceUtil).resolve(connection, Timeouts.DefaultTimeout)
    }
  }

  "jResolveComponentRef" must {
    "return completion stage for component ref" in {
      when(locationServiceUtil.resolve(connection, Timeouts.DefaultTimeout)).thenReturn(Future.successful(Right(location)))

      val completableComponentRef = commandUtil.jResolveComponentRef(prefix, componentType)

      completableComponentRef.toCompletableFuture.get(10, TimeUnit.SECONDS) shouldBe testRef
      verify(locationServiceUtil).resolve(connection, Timeouts.DefaultTimeout)
    }

    "throw exception if location service returns error" in {
      when(locationServiceUtil.resolve(connection, Timeouts.DefaultTimeout))
        .thenReturn(Future.successful(Left(LocationNotFound("Error while resolving location"))))

      val message = intercept[ExecutionException](
        commandUtil.jResolveComponentRef(prefix, componentType).toCompletableFuture.get(10, TimeUnit.SECONDS)
      ).getLocalizedMessage
      "esw.commons.utils.location.EswLocationError$LocationNotFound" shouldBe message
      verify(locationServiceUtil).resolve(connection, Timeouts.DefaultTimeout)
    }
  }
}
