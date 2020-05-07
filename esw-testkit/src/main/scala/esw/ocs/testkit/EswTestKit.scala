package esw.ocs.testkit

import java.nio.file.{Paths, Path => NIOPath}

import akka.actor.CoordinatedShutdown.UnknownReason
import akka.actor.testkit.typed.scaladsl.TestProbe
import akka.actor.typed.{ActorRef, ActorSystem, SpawnProtocol}
import akka.util.Timeout
import csw.command.client.extensions.AkkaLocationExt.RichAkkaLocation
import csw.command.client.messages.sequencer.SequencerMsg
import csw.event.api.scaladsl.{EventPublisher, EventService, EventSubscriber}
import csw.location.api.extensions.URIExtension._
import csw.location.api.models.Connection.AkkaConnection
import csw.location.api.models.{AkkaLocation, ComponentId, ComponentType}
import csw.location.api.scaladsl.LocationService
import csw.params.events.{Event, EventKey, SystemEvent}
import csw.prefix.models.{Prefix, Subsystem}
import csw.testkit.scaladsl.ScalaTestFrameworkTestKit
import esw.agent.app.{AgentApp, AgentSettings, AgentWiring}
import esw.ocs.api.SequencerApi
import esw.ocs.api.actor.client.{SequenceComponentImpl, SequencerApiFactory, SequencerImpl}
import esw.ocs.api.actor.messages.SequenceComponentMsg
import esw.ocs.api.protocol.ScriptError
import esw.ocs.app.wiring.{SequenceComponentWiring, SequencerWiring}
import esw.ocs.testkit.Service.{Gateway, MachineAgent}
import esw.ocs.testkit.simulation.SimulationSequencerWiring

import scala.collection.mutable
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.DurationLong
import scala.util.Random

abstract class EswTestKit(val gatewayTestKit: GatewayTestKit, services: Service*)
    extends ScalaTestFrameworkTestKit(Service.convertToCsw(services): _*)
    with LocationUtils {

  def this(services: Service*) = this(GatewayTestKit(), services: _*)

  implicit lazy val system: ActorSystem[SpawnProtocol.Command] = frameworkTestKit.actorSystem
  implicit lazy val ec: ExecutionContext                       = frameworkTestKit.frameworkWiring.actorRuntime.ec
  implicit lazy val askTimeout: Timeout                        = Timeout(10.seconds)
  private lazy val eventService: EventService                  = frameworkTestKit.frameworkWiring.eventServiceFactory.make(locationService)
  lazy val eventSubscriber: EventSubscriber                    = eventService.defaultSubscriber
  lazy val eventPublisher: EventPublisher                      = eventService.defaultPublisher

  def locationService: LocationService = frameworkTestKit.frameworkWiring.locationService

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
    if (services.contains(Gateway)) gatewayTestKit.spawnGateway()
    if (services.contains(MachineAgent)) spawnAgent(agentSettings)
  }

  override def afterAll(): Unit = {
    shutdownAllSequencers()
    gatewayTestKit.shutdownGateway()
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
    val wiring = AgentWiring.make(agentPrefix, agentSettings, system)
    agentWiring = Some(wiring)
    AgentApp.start(agentPrefix, wiring)
  }

  def shutdownAgent(): Unit = agentWiring.foreach(_.actorRuntime.shutdown(UnknownReason))

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
    val wiring = SequenceComponentWiring.make(subsystem, name, new SequencerWiring(_, _, _).sequencerServer, system)
    wiring.start().map { seqCompLocation =>
      sequenceComponentLocations += seqCompLocation
      seqCompLocation
    }
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

  def resolveSequenceComponent(prefix: Prefix): ActorRef[SequenceComponentMsg] =
    resolveSequenceComponentLocation(prefix).uri.toActorRef
      .unsafeUpcast[SequenceComponentMsg]

  def spawnSequenceComponentInSimulation(subsystem: Subsystem, name: Option[String]): Either[ScriptError, AkkaLocation] = {
    val wiring = SequenceComponentWiring.make(subsystem, name, new SimulationSequencerWiring(_, _, _).sequencerServer, system)
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

  def createTestProbe(eventKeys: Set[EventKey]): TestProbe[Event] = {
    val testProbe    = TestProbe[Event]
    val subscription = eventSubscriber.subscribeActorRef(eventKeys, testProbe.ref)
    subscription.ready().futureValue
    eventKeys.foreach(_ => testProbe.expectMessageType[SystemEvent]) // discard invalid event
    testProbe
  }
}
