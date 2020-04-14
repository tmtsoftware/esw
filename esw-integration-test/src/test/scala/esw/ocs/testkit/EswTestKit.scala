package esw.ocs.testkit

import java.nio.file.{Paths, Path => NIOPath}

import akka.actor
import akka.actor.CoordinatedShutdown.UnknownReason
import akka.actor.typed.{ActorRef, ActorSystem, SpawnProtocol}
import akka.http.scaladsl.Http.ServerBinding
import akka.http.scaladsl.model.Uri
import akka.http.scaladsl.model.Uri.Path
import akka.util.Timeout
import com.typesafe.config.ConfigFactory
import csw.command.client.extensions.AkkaLocationExt.RichAkkaLocation
import csw.command.client.messages.sequencer.SequencerMsg
import csw.event.api.scaladsl.{EventPublisher, EventService, EventSubscriber}
import csw.location.api.extensions.URIExtension._
import csw.location.api.models.Connection.{AkkaConnection, HttpConnection}
import csw.location.api.models.{AkkaLocation, ComponentId, ComponentType, HttpLocation}
import csw.location.api.scaladsl.LocationService
import csw.network.utils.SocketUtils
import csw.prefix.models.{Prefix, Subsystem}
import csw.testkit.scaladsl.ScalaTestFrameworkTestKit
import esw.agent.app.{AgentApp, AgentSettings, AgentWiring}
import esw.commons.BaseTestSuite
import esw.gateway.api.codecs.GatewayCodecs
import esw.gateway.api.protocol.{PostRequest, WebsocketRequest}
import esw.gateway.server.GatewayWiring
import esw.ocs.api.SequencerApi
import esw.ocs.api.actor.client.{SequenceComponentImpl, SequencerApiFactory, SequencerImpl}
import esw.ocs.api.actor.messages.SequenceComponentMsg
import esw.ocs.api.protocol.ScriptError
import esw.ocs.app.wiring.{SequenceComponentWiring, SequencerWiring}
import esw.ocs.simulation.SimulationSequencerWiring
import esw.ocs.testkit.Service.{Gateway, MachineAgent}
import msocket.api.ContentType
import msocket.impl.post.HttpPostTransport
import msocket.impl.ws.WebsocketTransport

import scala.collection.mutable
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.DurationLong
import scala.util.Random

abstract class EswTestKit(services: Service*)
    extends ScalaTestFrameworkTestKit(Service.convertToCsw(services): _*)
    with BaseTestSuite
    with GatewayCodecs {
  implicit lazy val system: ActorSystem[SpawnProtocol.Command] = frameworkTestKit.actorSystem
  implicit lazy val ec: ExecutionContext                       = frameworkTestKit.frameworkWiring.actorRuntime.ec
  implicit lazy val askTimeout: Timeout                        = Timeout(10.seconds)
  lazy val locationService: LocationService                    = frameworkTestKit.frameworkWiring.locationService
  lazy val untypedSystem: actor.ActorSystem                    = frameworkTestKit.frameworkWiring.actorRuntime.classicSystem
  private lazy val eventService: EventService                  = frameworkTestKit.frameworkWiring.eventServiceFactory.make(locationService)
  lazy val eventSubscriber: EventSubscriber                    = eventService.defaultSubscriber
  lazy val eventPublisher: EventPublisher                      = eventService.defaultPublisher

  // gateway
  lazy val gatewayPort: Int                 = SocketUtils.getFreePort
  lazy val gatewayWiring: GatewayWiring     = new GatewayWiring(Some(gatewayPort))
  var gatewayBinding: Option[ServerBinding] = None
  var gatewayLocation: Option[HttpLocation] = None
  // ESW-98
  private lazy val gatewayPrefix = Prefix(
    ConfigFactory
      .load()
      .getConfig("http-server")
      .getString("prefix")
  )
  lazy val gatewayPostClient = gatewayHTTPClient(gatewayPrefix)
  lazy val gatewayWsClient   = gatewayWebSocketClient(gatewayPrefix)

  private val sequenceComponentLocations: mutable.Buffer[AkkaLocation] = mutable.Buffer.empty

  // agent
  lazy val resourcesDirPath: NIOPath = Paths.get(getClass.getResource("/").getPath)
  lazy val agentSettings: AgentSettings = AgentSettings(
    resourcesDirPath.toString,
    durationToWaitForComponentRegistration = 5.seconds,
    durationToWaitForGracefulProcessTermination = 2.seconds
  )
  lazy val agentPrefix: Prefix                 = Prefix(s"esw.machine_${Random.nextInt().abs}")
  private var agentWiring: Option[AgentWiring] = None

  override implicit def patienceConfig: PatienceConfig = PatienceConfig(10.seconds)

  override def beforeAll(): Unit = {
    super.beforeAll()
    if (services.contains(Gateway)) spawnGateway()
    if (services.contains(MachineAgent)) spawnAgent(agentSettings)
  }

  override def afterAll(): Unit = {
    shutdownAllSequencers()
    shutdownGateway()
    shutdownAgent()
    super.afterAll()
  }

  def clearAll(): Unit =
    sequenceComponentLocations.clear()

  def shutdownAllSequencers(): Unit = {
    sequenceComponentLocations.foreach(new SequenceComponentImpl(_).unloadScript())
    clearAll()
  }

  def spawnAgent(agentSettings: AgentSettings): Unit = {
    agentWiring = Some(AgentApp.start(agentPrefix, agentSettings))
  }

  def shutdownAgent(): Unit = agentWiring.foreach(_.actorRuntime.shutdown(UnknownReason))

  def spawnGateway(): HttpLocation = {
    val (binding, registration) = gatewayWiring.httpService.registeredLazyBinding.futureValue
    gatewayBinding = Some(binding)
    gatewayLocation = Some(registration.location.asInstanceOf[HttpLocation])
    gatewayLocation.get
  }

  private def shutdownGateway(): Unit =
    if (gatewayBinding.nonEmpty)
      gatewayWiring.httpService.shutdown(UnknownReason).futureValue

  def spawnSequencerRef(subsystem: Subsystem, observingMode: String): ActorRef[SequencerMsg] =
    spawnSequencer(subsystem, observingMode).rightValue.sequencerRef

  def spawnSequencerProxy(subsystem: Subsystem, observingMode: String) =
    new SequencerImpl(spawnSequencerRef(subsystem, observingMode))

  def spawnSequencer(subsystem: Subsystem, observingMode: String): Either[ScriptError, AkkaLocation] = {
    val sequenceComponent = spawnSequenceComponent(subsystem, None)
    val locationE = sequenceComponent.flatMap { seqCompLocation =>
      new SequenceComponentImpl(seqCompLocation)
        .loadScript(subsystem, observingMode)
        .futureValue
        .response
    }
    locationE.left.foreach(println) // this is to print the exception in case script loading fails
    locationE
  }

  def spawnSequenceComponent(subsystem: Subsystem, name: Option[String]): Either[ScriptError, AkkaLocation] = {
    val wiring = new SequenceComponentWiring(subsystem, name, new SequencerWiring(_, _, _).sequencerServer)
    wiring.start().map { seqCompLocation =>
      sequenceComponentLocations += seqCompLocation
      seqCompLocation
    }
  }

  // ESW-98
  private def gatewayHTTPClient(prefix: Prefix) = {
    val httpLocation = resolveHTTPLocation(prefix, ComponentType.Service)
    val httpUri      = Uri(httpLocation.uri.toString).withPath(Path("/post-endpoint")).toString()
    val httpClient =
      new HttpPostTransport[PostRequest](httpUri, ContentType.Json, () => None)
    httpClient
  }

  private def gatewayWebSocketClient(prefix: Prefix) = {
    val httpLocation = resolveHTTPLocation(prefix, ComponentType.Service)
    val webSocketUri = Uri(httpLocation.uri.toString).withScheme("ws").withPath(Path("/websocket-endpoint")).toString()
    val webSocketClient =
      new WebsocketTransport[WebsocketRequest](webSocketUri, ContentType.Json)
    webSocketClient
  }

  def sequencerClient(subsystem: Subsystem, observingMode: String): SequencerApi = {
    val httpLocation = resolveHTTPLocation(Prefix(subsystem, observingMode), ComponentType.Sequencer)
    SequencerApiFactory.make(httpLocation)
  }

  def resolveSequencerLocation(prefix: Prefix): AkkaLocation =
    resolveAkkaLocation(prefix, ComponentType.Sequencer)

  def resolveSequencerLocation(subsystem: Subsystem, observingMode: String): AkkaLocation =
    resolveSequencerLocation(Prefix(subsystem, observingMode))

  def resolveSequencer(subsystem: Subsystem, observingMode: String): ActorRef[SequencerMsg] =
    resolveSequencerLocation(subsystem, observingMode).uri.toActorRef
      .unsafeUpcast[SequencerMsg]

  def resolveSequenceComponentLocation(prefix: Prefix): AkkaLocation =
    resolveAkkaLocation(prefix, ComponentType.SequenceComponent)

  private def resolveAkkaLocation(prefix: Prefix, componentType: ComponentType) =
    locationService
      .resolve(AkkaConnection(ComponentId(prefix, componentType)), 5.seconds)
      .futureValue
      .value

  private def resolveHTTPLocation(prefix: Prefix, componentType: ComponentType) =
    locationService
      .resolve(HttpConnection(ComponentId(prefix, componentType)), 5.seconds)
      .futureValue
      .value

  def resolveSequenceComponent(prefix: Prefix): ActorRef[SequenceComponentMsg] =
    resolveSequenceComponentLocation(prefix).uri.toActorRef
      .unsafeUpcast[SequenceComponentMsg]

  def spawnSequenceComponentInSimulation(subsystem: Subsystem, name: Option[String]): Either[ScriptError, AkkaLocation] = {
    val wiring = new SequenceComponentWiring(subsystem, name, new SimulationSequencerWiring(_, _, _).sequencerServer)
    wiring.start().map { seqCompLocation =>
      sequenceComponentLocations += seqCompLocation
      seqCompLocation
    }
  }

  def spawnSequencerInSimulation(subsystem: Subsystem, observingMode: String): Either[ScriptError, AkkaLocation] = {
    val sc = spawnSequenceComponentInSimulation(subsystem, None)

    val locationE = sc.flatMap { seqCompLocation =>
      new SequenceComponentImpl(seqCompLocation)
        .loadScript(subsystem, observingMode)
        .futureValue
        .response
    }
    locationE.left.foreach(println) // this is to print the exception in case script loading fails
    locationE
  }
}
