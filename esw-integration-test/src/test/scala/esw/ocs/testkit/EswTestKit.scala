package esw.ocs.testkit

import java.net.URI

import akka.actor
import akka.actor.typed.{ActorRef, ActorSystem, SpawnProtocol}
import akka.util.Timeout
import csw.command.client.messages.sequencer.SequencerMsg
import csw.event.api.scaladsl.{EventPublisher, EventService, EventSubscriber}
import csw.location.api.extensions.URIExtension._
import csw.location.api.scaladsl.LocationService
import csw.location.models.Connection.{AkkaConnection, HttpConnection}
import csw.location.models.{AkkaLocation, ComponentId, ComponentType}
import csw.params.core.models.Subsystem
import csw.testkit.scaladsl.{CSWService, ScalaTestFrameworkTestKit}
import esw.ocs.api.client.{SequencerAdminClient, SequencerCommandClient}
import esw.ocs.api.protocol.ScriptError
import esw.ocs.app.wiring.{SequenceComponentWiring, SequencerWiring}
import esw.ocs.impl.messages.SequenceComponentMsg
import esw.ocs.impl.{SequencerAdminClientFactory, SequencerCommandClientFactory}
import msocket.impl.Encoding.JsonText

import scala.collection.mutable
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.DurationLong

abstract class EswTestKit(services: CSWService*) extends ScalaTestFrameworkTestKit(services: _*) with BaseTestSuite {
  implicit lazy val system: ActorSystem[SpawnProtocol.Command] = frameworkTestKit.actorSystem
  implicit lazy val ec: ExecutionContext                       = frameworkTestKit.frameworkWiring.actorRuntime.ec
  implicit lazy val askTimeout: Timeout                        = Timeout(10.seconds)
  lazy val locationService: LocationService                    = frameworkTestKit.frameworkWiring.locationService
  lazy val untypedSystem: actor.ActorSystem                    = frameworkTestKit.frameworkWiring.actorRuntime.untypedSystem

  private lazy val eventService: EventService = frameworkTestKit.frameworkWiring.eventServiceFactory.make(locationService)
  lazy val eventSubscriber: EventSubscriber   = eventService.defaultSubscriber
  lazy val eventPublisher: EventPublisher     = eventService.defaultPublisher

  private val sequencerWirings: mutable.Buffer[SequencerWiring]            = mutable.Buffer.empty
  private val sequenceCompWirings: mutable.Buffer[SequenceComponentWiring] = mutable.Buffer.empty

  override implicit def patienceConfig: PatienceConfig = PatienceConfig(10.seconds)

  override def afterAll(): Unit = {
    shutdownAllSequencers()
    super.afterAll()
  }

  def clearAllWirings(): Unit = {
    sequencerWirings.clear()
    sequenceCompWirings.clear()
  }

  def shutdownAllSequencers(): Unit = {
    sequencerWirings.foreach(_.sequencerServer.shutDown())
    clearAllWirings()
  }

  def spawnSequencerRef(
      packageId: String,
      observingMode: String,
      sequenceComponentName: Option[String] = None
  ): ActorRef[SequencerMsg] =
    spawnSequencer(packageId, observingMode, sequenceComponentName).toOption.get.uri.toActorRef.unsafeUpcast[SequencerMsg]

  def spawnSequencer(
      packageId: String,
      observingMode: String,
      sequenceComponentName: Option[String] = None
  ): Either[ScriptError, AkkaLocation] = {
    val wiring = new SequencerWiring(packageId, observingMode, sequenceComponentName)
    sequencerWirings += wiring
    wiring.sequencerServer.start()
  }

  def spawnSequenceComponent(subsystem: Subsystem, name: Option[String]): Either[ScriptError, AkkaLocation] = {
    val wiring = new SequenceComponentWiring(subsystem, name, new SequencerWiring(_, _, _).sequencerServer)
    sequenceCompWirings += wiring
    wiring.start()
  }

  private def resolveSequencerHttp(packageId: String, observingMode: String): URI = {
    val componentId = ComponentId(s"$packageId@$observingMode@http", ComponentType.Service)
    locationService.resolve(HttpConnection(componentId), 5.seconds).futureValue.get.uri
  }

  def sequencerAdminClient(packageId: String, observingMode: String): SequencerAdminClient = {
    val uri     = resolveSequencerHttp(packageId, observingMode)
    val postUrl = s"${uri.toString}post-endpoint"
    SequencerAdminClientFactory.make(postUrl, JsonText, () => None)
  }

  def sequencerCommandClient(packageId: String, observingMode: String): SequencerCommandClient = {
    val uri     = resolveSequencerHttp(packageId, observingMode)
    val postUrl = s"${uri.toString}post-endpoint"
    val wsUrl   = s"ws://${uri.getHost}:${uri.getPort}/websocket-endpoint"
    SequencerCommandClientFactory.make(postUrl, wsUrl, JsonText, () => None)
  }

  def resolveSequencerLocation(sequencerName: String): AkkaLocation =
    locationService
      .resolve(AkkaConnection(ComponentId(sequencerName, ComponentType.Sequencer)), 5.seconds)
      .futureValue
      .value

  def resolveSequencerLocation(packageId: String, observingMode: String): AkkaLocation =
    resolveSequencerLocation(s"$packageId@$observingMode")

  def resolveSequencer(packageId: String, observingMode: String): ActorRef[SequencerMsg] =
    resolveSequencerLocation(packageId, observingMode).uri.toActorRef
      .unsafeUpcast[SequencerMsg]

  def resolveSequenceComponentLocation(name: String): AkkaLocation =
    locationService
      .resolve(AkkaConnection(ComponentId(name, ComponentType.SequenceComponent)), 5.seconds)
      .futureValue
      .value

  def resolveSequenceComponent(name: String): ActorRef[SequenceComponentMsg] =
    resolveSequenceComponentLocation(name).uri.toActorRef
      .unsafeUpcast[SequenceComponentMsg]
}
