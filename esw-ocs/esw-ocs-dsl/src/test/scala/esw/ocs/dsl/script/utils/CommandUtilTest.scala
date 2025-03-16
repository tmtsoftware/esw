package esw.ocs.dsl.script.utils

import org.apache.pekko.actor.testkit.typed.scaladsl.TestProbe
import org.apache.pekko.actor.typed.{ActorSystem, SpawnProtocol}
import csw.command.client.messages.ComponentMessage
import csw.location.api.extensions.ActorExtension.*
import csw.location.api.models.Connection.PekkoConnection
import csw.location.api.models.{PekkoLocation, ComponentId, ComponentType, Metadata}
import csw.prefix.models.Prefix
import csw.prefix.models.Subsystem.ESW
import esw.commons.utils.location.EswLocationError.LocationNotFound
import esw.commons.utils.location.LocationServiceUtil
import esw.constants.CommonTimeouts
import esw.testcommons.BaseTestSuite
import org.mockito.Mockito.{reset, verify, when}

import java.util.concurrent.TimeUnit
import scala.concurrent.{ExecutionException, Future}
class CommandUtilTest extends BaseTestSuite {
  implicit val testSystem: ActorSystem[SpawnProtocol.Command] = ActorSystem(SpawnProtocol(), "testSystem")

  private val locationServiceUtil = mock[LocationServiceUtil]
  private val prefix              = Prefix(ESW, "trombone")
  private val componentType       = ComponentType.Assembly
  private val connection          = PekkoConnection(ComponentId(prefix, componentType))
  private val testRef             = TestProbe[ComponentMessage]().ref
  private val location            = PekkoLocation(connection, testRef.toURI, Metadata.empty)

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    reset(locationServiceUtil)
  }

  private val commandUtil = new CommandUtil(locationServiceUtil)
  "jResolvePekkoLocation" must {
    "return completion stage for pekko location" in {
      when(locationServiceUtil.resolve(connection, CommonTimeouts.ResolveLocation))
        .thenReturn(Future.successful(Right(location)))

      val completableLocation = commandUtil.jResolvePekkoLocation(prefix, componentType)

      completableLocation.toCompletableFuture.get(10, TimeUnit.SECONDS) shouldBe location
      verify(locationServiceUtil).resolve(connection, CommonTimeouts.ResolveLocation)
    }

    "throw exception if location service returns error" in {
      when(locationServiceUtil.resolve(connection, CommonTimeouts.ResolveLocation))
        .thenReturn(Future.successful(Left(LocationNotFound("Error while resolving location"))))

      val message = intercept[ExecutionException](
        commandUtil.jResolvePekkoLocation(prefix, componentType).toCompletableFuture.get(10, TimeUnit.SECONDS)
      ).getLocalizedMessage
      "esw.commons.utils.location.EswLocationError$LocationNotFound: Error while resolving location" shouldBe message
      verify(locationServiceUtil).resolve(connection, CommonTimeouts.ResolveLocation)
    }
  }

  "jResolveComponentRef" must {
    "return completion stage for component ref" in {
      when(locationServiceUtil.resolve(connection, CommonTimeouts.ResolveLocation))
        .thenReturn(Future.successful(Right(location)))

      val completableComponentRef = commandUtil.jResolveComponentRef(prefix, componentType)

      completableComponentRef.toCompletableFuture.get(10, TimeUnit.SECONDS) shouldBe testRef
      verify(locationServiceUtil).resolve(connection, CommonTimeouts.ResolveLocation)
    }

    "throw exception if location service returns error" in {
      when(locationServiceUtil.resolve(connection, CommonTimeouts.ResolveLocation))
        .thenReturn(Future.successful(Left(LocationNotFound("Error while resolving location"))))

      val message = intercept[ExecutionException](
        commandUtil.jResolveComponentRef(prefix, componentType).toCompletableFuture.get(10, TimeUnit.SECONDS)
      ).getLocalizedMessage
      "esw.commons.utils.location.EswLocationError$LocationNotFound: Error while resolving location" shouldBe message
      verify(locationServiceUtil).resolve(connection, CommonTimeouts.ResolveLocation)
    }
  }
}
