package esw.gateway.server.admin

import java.net.InetAddress

import akka.actor.testkit.typed.TestKitSettings
import akka.actor.testkit.typed.scaladsl.TestProbe
import akka.actor.typed.{ActorRef, ActorSystem, SpawnProtocol}
import akka.http.scaladsl.Http
import akka.http.scaladsl.client.RequestBuilding
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, StatusCodes}
import com.typesafe.config.ConfigFactory
import csw.command.client.messages.CommandMessage.Oneway
import csw.command.client.messages.ContainerCommonMessage.GetComponents
import csw.command.client.messages.ContainerMessage
import csw.command.client.models.framework.{Component, Components, ContainerLifecycleState}
import csw.location.api.models.ComponentType.{Assembly, HCD}
import csw.location.api.models.Connection.AkkaConnection
import csw.location.api.models.{ComponentId, ComponentType}
import csw.location.client.ActorSystemFactory
import csw.logging.client.internal.JsonExtensions._
import csw.logging.client.internal._
import csw.logging.client.scaladsl.LoggingSystemFactory
import csw.logging.models.Level.{ERROR, INFO, WARN}
import csw.logging.models.{Level, LogMetadata}
import csw.network.utils.Networks
import csw.params.commands.CommandResponse.OnewayResponse
import csw.params.commands.{CommandName, Setup}
import csw.prefix.models.{Prefix, Subsystem}
import esw.gateway.api.clients.AdminClient
import esw.gateway.api.codecs.GatewayCodecs
import esw.gateway.server.TestAppender
import esw.gateway.server.admin.FrameworkAssertions._
import esw.ocs.testkit.EswTestKit
import esw.ocs.testkit.Service.Gateway
import msocket.api.models.{GenericError, ServiceError}
import play.api.libs.json.{JsObject, Json}

import scala.collection.mutable
import scala.concurrent.Await
import scala.concurrent.duration.DurationDouble

class AdminGatewayTest extends EswTestKit(Gateway) with GatewayCodecs {

  protected val logBuffer: mutable.Buffer[JsObject] = mutable.Buffer.empty[JsObject]
  protected val testAppender                        = new TestAppender(x => logBuffer += Json.parse(x.toString).as[JsObject])

  private implicit val typedSystem: ActorSystem[SpawnProtocol.Command] = frameworkTestKit.frameworkWiring.actorSystem

  protected val hostName: String = InetAddress.getLocalHost.getHostName

  implicit val testKitSettings: TestKitSettings = TestKitSettings(typedSystem)

  private val laserConnection                                          = AkkaConnection(ComponentId(Prefix(Subsystem.TCS, "Laser"), Assembly))
  private val motionControllerConnection                               = AkkaConnection(ComponentId(Prefix(Subsystem.TCS, "Motion_Controller"), HCD))
  private val galilConnection                                          = AkkaConnection(ComponentId(Prefix(Subsystem.TCS, "Galil"), Assembly))
  private val probe                                                    = TestProbe[OnewayResponse]
  private val startLoggingCmd                                          = CommandName("StartLogging")
  private val prefix                                                   = Prefix("iris.command")
  private var containerActorSystem: ActorSystem[SpawnProtocol.Command] = _
  private var laserComponent: Component                                = _
  private var galilComponent: Component                                = _
  private var loggingSystem: LoggingSystem                             = _
  private var adminClient: AdminClient                                 = _

  override def beforeAll(): Unit = {
    super.beforeAll()
    loggingSystem = LoggingSystemFactory.start("logging", "version", hostName, typedSystem)
    loggingSystem.setAppenders(List(testAppender))

    logBuffer.clear()

    containerActorSystem = ActorSystemFactory.remote(SpawnProtocol(), "container-system")

    // this will start container on random port and join seed and form a cluster
    val containerRef = startContainerAndWaitForRunning()
    extractComponentsFromContainer(containerRef)
    adminClient = new AdminClient(gatewayPostClient)
  }

  override def afterAll(): Unit = {
    containerActorSystem.terminate()
    Await.result(containerActorSystem.whenTerminated, 5.seconds)
    super.afterAll()
  }

  def startContainerAndWaitForRunning(): ActorRef[ContainerMessage] = {
    val config       = ConfigFactory.load("laser_container.conf")
    val containerRef = spawnContainer(config)

    val containerStateProbe = TestProbe[ContainerLifecycleState]
    assertThatContainerIsRunning(containerRef, containerStateProbe, 5.seconds)
    containerRef
  }

  def extractComponentsFromContainer(containerRef: ActorRef[ContainerMessage]): Unit = {
    val probe = TestProbe[Components]
    containerRef ! GetComponents(probe.ref)
    val components = probe.expectMessageType[Components].components

    laserComponent = components.find(x => x.info.prefix.componentName.equals("Laser")).get
    galilComponent = components.find(x => x.info.prefix.componentName.equals("Galil")).get
  }

  override protected def afterEach(): Unit = logBuffer.clear()

  "AdminApi" must {

    "get the current component log meta data | ESW-254, CSW-78, CSW-81" in {
      val logMetadata1 = adminClient.getLogMetadata(motionControllerConnection.componentId).futureValue

      val config     = ConfigFactory.load().getConfig("csw-logging")
      val logLevel   = Level(config.getString("logLevel"))
      val akkaLevel  = Level(config.getString("akkaLogLevel"))
      val slf4jLevel = Level(config.getString("slf4jLogLevel"))
      val componentLogLevel = Level(
        config
          .getConfig("component-log-levels")
          .getString(motionControllerConnection.prefix.toString)
      )
      logMetadata1 shouldBe LogMetadata(logLevel, akkaLevel, slf4jLevel, componentLogLevel)

      // updating default and akka log level
      loggingSystem.setDefaultLogLevel(ERROR)
      loggingSystem.setAkkaLevel(WARN)

      val logMetadata2 = adminClient.getLogMetadata(motionControllerConnection.componentId).futureValue

      logMetadata2 shouldBe LogMetadata(ERROR, WARN, slf4jLevel, componentLogLevel)

      // reset log levels to default
      loggingSystem.setDefaultLogLevel(logLevel)
      loggingSystem.setAkkaLevel(akkaLevel)
    }

    "set log level of the component dynamically through http end point | ESW-254, CSW-81, ESW-279" in {
      laserComponent.supervisor ! Oneway(Setup(prefix, startLoggingCmd, None), probe.ref)
      Thread.sleep(500)

      // default logging level for Laser component is info
      val groupByComponentNamesLog = logBuffer.groupBy { json =>
        if (json.contains("@componentName")) json.getString("@componentName")
      }
      val laserComponentLogs = groupByComponentNamesLog(laserComponent.info.prefix.componentName)

      laserComponentLogs.exists(log => log.getString("@severity").toLowerCase.equalsIgnoreCase("info")) shouldBe true
      laserComponentLogs.foreach { log =>
        val currentLogLevel = log.getString("@severity").toLowerCase
        Level(currentLogLevel) >= INFO shouldBe true
      }

      adminClient.setLogLevel(laserConnection.componentId, ERROR).futureValue

      Thread.sleep(100)
      logBuffer.clear()

      // laser and galil components, start logging messages at all log levels
      // and expected is that, laser component logs messages at and above Error level
      // and galil component  still logs messages at and above Info level

      laserComponent.supervisor ! Oneway(Setup(prefix, startLoggingCmd, None), probe.ref)
      galilComponent.supervisor ! Oneway(Setup(prefix, startLoggingCmd, None), probe.ref)
      Thread.sleep(100)

      val groupByAfterFilter       = logBuffer.groupBy(json => json.getString("@componentName"))
      val laserCompLogsAfterFilter = groupByAfterFilter(laserConnection.prefix.componentName)
      val galilCompLogsAfterFilter = groupByAfterFilter(galilConnection.prefix.componentName)

      laserCompLogsAfterFilter.exists(log => log.getString("@severity").toLowerCase.equalsIgnoreCase("error")) shouldBe true
      laserCompLogsAfterFilter.foreach { log =>
        val currentLogLevel = log.getString("@severity").toLowerCase
        Level(currentLogLevel) >= ERROR shouldBe true
      }

      // this makes sure that, changing log level of one component (laser component) from container does not affect other components (galil component) log level
      galilCompLogsAfterFilter.exists(log => log.getString("@severity").toLowerCase.equalsIgnoreCase("info")) shouldBe true

      galilCompLogsAfterFilter.foreach { log =>
        val currentLogLevel = log.getString("@severity").toLowerCase
        Level(currentLogLevel) >= INFO shouldBe true
      }
    }

    "return appropriate error when component is not resolved for akka connection | ESW-254, CSW-81, ESW-279" in {
      val serviceError = intercept[ServiceError] {
        Await.result(adminClient.getLogMetadata(ComponentId(Prefix(Subsystem.TCS, "abc"), ComponentType.HCD)), 5.seconds)
      }

      serviceError.generic_error should ===(
        GenericError("UnresolvedAkkaLocationException", "Could not resolve TCS.abc to a valid Akka location")
      )
    }

    "return appropriate exception when logging level is incorrect | ESW-254, CSW-81, ESW-279" in {
      val str = """{"SetLogLevel":{"componentId":{"prefix":"TCS.Laser","componentType":"assembly"},"level":"INVALID"}}"""
      val request = RequestBuilding
        .Post(
          s"http://${Networks().hostname}:$gatewayPort/post-endpoint",
          entity = HttpEntity(ContentTypes.`application/json`, str)
        )
      val response = Await.result(Http().singleRequest(request), 5.seconds)
      response.status should ===(StatusCodes.BadRequest)
    }
  }
}
