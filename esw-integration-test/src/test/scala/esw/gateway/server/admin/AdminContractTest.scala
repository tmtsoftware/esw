package esw.gateway.server.admin

import java.net.InetAddress

import akka.Done
import akka.actor.testkit.typed.TestKitSettings
import akka.actor.testkit.typed.scaladsl.TestProbe
import akka.actor.typed.{ActorRef, ActorSystem, SpawnProtocol}
import akka.http.scaladsl.Http
import akka.http.scaladsl.client.RequestBuilding
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, StatusCodes}
import com.typesafe.config.ConfigFactory
import csw.command.client.CommandServiceFactory
import csw.command.client.messages.CommandMessage.Oneway
import csw.command.client.messages.ContainerCommonMessage.GetComponents
import csw.command.client.messages.ContainerMessage
import csw.command.client.models.framework.{Component, Components, ContainerLifecycleState, SupervisorLifecycleState}
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
import csw.params.core.states.CurrentState
import csw.prefix.models.{Prefix, Subsystem}
import csw.testkit.FrameworkTestKit
import esw.gateway.api.clients.AdminClient
import esw.gateway.api.codecs.GatewayCodecs
import esw.gateway.api.protocol.InvalidComponent
import esw.gateway.server.TestAppender
import esw.gateway.server.admin.FrameworkAssertions._
import esw.ocs.testkit.EswTestKit
import esw.ocs.testkit.Service.AAS
import play.api.libs.json.{JsObject, Json}

import scala.collection.mutable
import scala.concurrent.Await
import scala.concurrent.duration.DurationDouble

class AdminContractTest extends EswTestKit(AAS) with GatewayCodecs {
  protected val logBuffer: mutable.Buffer[JsObject] = mutable.Buffer.empty[JsObject]
  protected val testAppender                        = new TestAppender(x => logBuffer += Json.parse(x.toString).as[JsObject])

  private implicit val typedSystem: ActorSystem[SpawnProtocol.Command] = frameworkTestKit.frameworkWiring.actorSystem

  protected val hostName: String = InetAddress.getLocalHost.getHostName

  implicit val testKitSettings: TestKitSettings = TestKitSettings(typedSystem)

  private val laserConnection                                          = AkkaConnection(ComponentId(Prefix(Subsystem.TCS, "Laser"), Assembly))
  private val motionControllerConnection                               = AkkaConnection(ComponentId(Prefix(Subsystem.TCS, "Motion_Controller"), HCD))
  private val galilConnection                                          = AkkaConnection(ComponentId(Prefix(Subsystem.TCS, "Galil"), Assembly))
  private val probe                                                    = TestProbe[OnewayResponse]()
  private val startLoggingCmd                                          = CommandName("StartLogging")
  private val prefix                                                   = Prefix("iris.command")
  private var containerActorSystem: ActorSystem[SpawnProtocol.Command] = _
  private var laserComponent: Component                                = _
  private var galilComponent: Component                                = _
  private var loggingSystem: LoggingSystem                             = _

  override def beforeAll(): Unit = {
    super.beforeAll()
    spawnGateway(authEnabled = true)
    loggingSystem = LoggingSystemFactory.start("logging", "version", hostName, typedSystem)
    loggingSystem.setAppenders(List(testAppender))

    logBuffer.clear()

    containerActorSystem = ActorSystemFactory.remote(SpawnProtocol(), "container-system")

    // this will start container on random port and join seed and form a cluster
    val containerRef = startContainerAndWaitForRunning("laser_container.conf")(typedSystem)
    val components   = extractComponentsFromContainer(containerRef)

    laserComponent = components(Prefix("TCS.Laser"))
    galilComponent = components(Prefix("TCS.Galil"))
  }

  override def afterAll(): Unit = {
    containerActorSystem.terminate()
    Await.result(containerActorSystem.whenTerminated, 5.seconds)
    super.afterAll()
  }

  override protected def afterEach(): Unit = logBuffer.clear()

  "AdminApi" must {

    "get the current component log meta data | ESW-254, CSW-78, CSW-81" in {
      val adminClient  = new AdminClient(gatewayPostClient)
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
      val gatewayPostClient = gatewayHTTPClient(tokenWithTcsUserRole)
      val adminClient       = new AdminClient(gatewayPostClient)

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

    "return appropriate error when component is not resolved for akka connection while getting log metadata | ESW-254, CSW-81, ESW-279, ESW-372" in {
      val adminClient         = new AdminClient(gatewayPostClient)
      val nonRegisteredCompId = ComponentId(Prefix(Subsystem.TCS, "abc"), ComponentType.HCD)

      val serviceError = intercept[InvalidComponent] {
        Await.result(adminClient.getLogMetadata(nonRegisteredCompId), 5.seconds)
      }

      serviceError should ===(InvalidComponent("Could not find component : ComponentId(TCS.abc,HCD)"))
    }

    "return appropriate error when component is not resolved for akka connection while setting log level | ESW-254, CSW-81, ESW-279, ESW-372" in {
      val gatewayPostClient   = gatewayHTTPClient(tokenWithEswUserRole)
      val adminClient         = new AdminClient(gatewayPostClient)
      val nonRegisteredCompId = ComponentId(Prefix(Subsystem.TCS, "abc"), ComponentType.HCD)

      val serviceError = intercept[InvalidComponent] {
        Await.result(adminClient.setLogLevel(nonRegisteredCompId, ERROR), 5.seconds)
      }

      serviceError should ===(InvalidComponent("Could not find component : ComponentId(TCS.abc,HCD)"))
    }

    "return appropriate exception when logging level is incorrect | ESW-254, CSW-81, ESW-279" in {
      val str = """{"SetLogLevel":{"componentId":{"prefix":"TCS.Laser","componentType":"assembly"},"level":"INVALID"}}"""
      val request = RequestBuilding
        .Post(
          s"http://${Networks().hostname}:$gatewayPort/post-endpoint",
          entity = HttpEntity(ContentTypes.`application/json`, str)
        )
      val response = Await.result(Http().singleRequest(request), 5.seconds)
      response.status shouldBe StatusCodes.BadRequest
    }

    "be able to send components into offline and online state | ESW-378" in {
      val gatewayPostClient                               = gatewayHTTPClient(tokenWithEswUserRole)
      val adminClient                                     = new AdminClient(gatewayPostClient)
      val actorSystem: ActorSystem[SpawnProtocol.Command] = ActorSystemFactory.remote(SpawnProtocol(), "sample-container-system")
      // start container
      startContainerAndWaitForRunning("sample_container.conf")(actorSystem)

      import esw.gateway.server.admin.SampleContainerState._

      //test AdminApi.goOffline Api for container
      adminClient.goOffline(eswContainerCompId).futureValue shouldBe Done

      adminClient.getContainerLifecycleState(eswContainerPrefix).futureValue shouldBe ContainerLifecycleState.Running
      adminClient.getComponentLifecycleState(eswAssemblyCompId).futureValue shouldBe SupervisorLifecycleState.RunningOffline
      adminClient.getComponentLifecycleState(eswGalilHcdCompId).futureValue shouldBe SupervisorLifecycleState.RunningOffline
      //**************************************************************************************\\

      //test AdminApi.goOnline Api for container
      adminClient.goOnline(eswContainerCompId).futureValue shouldBe Done

      adminClient.getContainerLifecycleState(eswContainerPrefix).futureValue shouldBe ContainerLifecycleState.Running
      adminClient.getComponentLifecycleState(eswAssemblyCompId).futureValue shouldBe SupervisorLifecycleState.Running
      adminClient.getComponentLifecycleState(eswGalilHcdCompId).futureValue shouldBe SupervisorLifecycleState.Running
      //**************************************************************************************\\

      //test AdminApi.goOffline Api for component(HCD or Assembly)
      adminClient.goOffline(eswAssemblyCompId).futureValue shouldBe Done

      adminClient.getComponentLifecycleState(eswAssemblyCompId).futureValue shouldBe SupervisorLifecycleState.RunningOffline
      adminClient.getComponentLifecycleState(eswGalilHcdCompId).futureValue shouldBe SupervisorLifecycleState.Running
      //**************************************************************************************\\

      //test AdminApi.goOnline Api for component(HCD or Assembly)
      adminClient.goOnline(eswAssemblyCompId).futureValue shouldBe Done

      adminClient.getComponentLifecycleState(eswAssemblyCompId).futureValue shouldBe SupervisorLifecycleState.Running
      adminClient.getComponentLifecycleState(eswGalilHcdCompId).futureValue shouldBe SupervisorLifecycleState.Running
      //**************************************************************************************\\

      adminClient.shutdown(eswContainerCompId).futureValue shouldBe Done

      actorSystem.terminate()
      actorSystem.whenTerminated.futureValue

      eventually {
        locationService.resolve(AkkaConnection(eswContainerCompId), 5.seconds).futureValue shouldBe None
      }
    }

    "be able to restart and shutdown the given component | ESW-378" in {
      val gatewayPostClient = gatewayHTTPClient(tokenWithEswUserRole)
      val adminClient       = new AdminClient(gatewayPostClient)

      val actorSystem: ActorSystem[SpawnProtocol.Command] = ActorSystemFactory.remote(SpawnProtocol(), "sample-container-system")

      import esw.gateway.server.admin.SampleContainerState._
      startContainerAndWaitForRunning("sample_container.conf")(actorSystem)

      val filterAssemblyLocation = locationService.resolve(AkkaConnection(eswAssemblyCompId), 5.seconds).futureValue.get
      val galilHcdLocation       = locationService.resolve(AkkaConnection(eswGalilHcdCompId), 5.seconds).futureValue.get

      val assemblyCommandService = CommandServiceFactory.make(filterAssemblyLocation)(actorSystem)
      val galilHcdCommandService = CommandServiceFactory.make(galilHcdLocation)(actorSystem)

      val assemblyProbe = TestProbe[CurrentState]("assembly-state-probe")(actorSystem)
      val galilHcdProbe = TestProbe[CurrentState]("galil-Hcd-state-probe")(actorSystem)

      val assemblyStateNames = Set(initializingFilterAssemblyStateName, shutdownFilterAssemblyStateName)
      val galilStateNames    = Set(initializingGalilStateName, shutdownGalilStateName)
      assemblyCommandService.subscribeCurrentState(assemblyStateNames, assemblyProbe.ref ! _)
      galilHcdCommandService.subscribeCurrentState(galilStateNames, galilHcdProbe.ref ! _)

      //test AdminApi.restart Api for container
      val restartResForContainer = adminClient.restart(eswContainerCompId)

      galilHcdProbe.expectMessage(galilShutdownCurrentState)
      assemblyProbe.expectMessage(assemblyShutdownCurrentState)
      galilHcdProbe.expectMessage(galilInitializeCurrentState)
      assemblyProbe.expectMessage(assemblyInitializeCurrentState)

      restartResForContainer.futureValue shouldBe Done
      //**************************************************************************************\\

      //test AdminApi.restart Api for component(HCD or Assembly)
      val restartResForHcd = adminClient.restart(eswGalilHcdCompId)

      galilHcdProbe.expectMessage(galilShutdownCurrentState)
      galilHcdProbe.expectMessage(galilInitializeCurrentState)
      assemblyProbe.expectNoMessage()

      restartResForHcd.futureValue shouldBe Done
      //**************************************************************************************\\

      //test AdminApi.shutdown Api for component(HCD or Assembly)
      val shutdownResForAssembly = adminClient.shutdown(eswAssemblyCompId)
      assemblyProbe.expectMessage(assemblyShutdownCurrentState)

      shutdownResForAssembly.futureValue shouldBe Done
      //**************************************************************************************\\

      //test AdminApi.shutdown Api for container
      val shutdownResForContainer = adminClient.shutdown(eswContainerCompId)

      galilHcdProbe.expectMessage(galilShutdownCurrentState)
      assemblyProbe.expectNoMessage()

      shutdownResForContainer.futureValue shouldBe Done
      //**************************************************************************************\\

      actorSystem.terminate()
      actorSystem.whenTerminated.futureValue

      eventually {
        locationService.resolve(AkkaConnection(eswContainerCompId), 5.seconds).futureValue shouldBe None
      }
    }
  }

  private def startContainerAndWaitForRunning(
      confName: String
  )(actorSystem: ActorSystem[SpawnProtocol.Command]): ActorRef[ContainerMessage] = {
    val config       = ConfigFactory.load(confName)
    val containerRef = FrameworkTestKit(actorSystem).spawnContainer(config)

    val containerStateProbe = TestProbe[ContainerLifecycleState]()(actorSystem)
    assertThatContainerIsRunning(containerRef, containerStateProbe, 5.seconds)
    containerRef
  }

  private def extractComponentsFromContainer(containerRef: ActorRef[ContainerMessage]): Map[Prefix, Component] = {
    val probe = TestProbe[Components]()
    containerRef ! GetComponents(probe.ref)
    val components = probe.expectMessageType[Components].components

    components.foldLeft(Map.empty[Prefix, Component])((res, comp) => {
      res.updated(comp.info.prefix, comp)
    })
  }

}
