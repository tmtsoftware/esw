package esw.ocs

import java.net.URI

import akka.actor.typed.{ActorRef, ActorSystem, SpawnProtocol}
import akka.util.Timeout
import csw.command.client.messages.sequencer.SequencerMsg
import csw.event.api.scaladsl.{EventPublisher, EventService, EventSubscriber}
import csw.location.api.extensions.URIExtension._
import csw.location.api.scaladsl.LocationService
import csw.location.models.Connection.{AkkaConnection, HttpConnection}
import csw.location.models.{AkkaLocation, ComponentId, ComponentType}
import csw.testkit.scaladsl.CSWService.EventServer
import csw.testkit.scaladsl.{CSWService, ScalaTestFrameworkTestKit}
import esw.ocs.api.client.{SequencerAdminClient, SequencerCommandClient}
import esw.ocs.api.protocol.LoadScriptError
import esw.ocs.app.wiring.SequencerWiring
import esw.ocs.impl.messages.SequenceComponentMsg
import esw.ocs.impl.{SequencerAdminClientFactory, SequencerCommandClientFactory}
import msocket.impl.Encoding.JsonText

import scala.collection.mutable
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.DurationLong

abstract class EswTestKit(services: CSWService*) extends ScalaTestFrameworkTestKit(EventServer) {

//  import frameworkTestKit.frameworkWiring._
  lazy val locationService: LocationService                    = frameworkTestKit.frameworkWiring.locationService
  implicit lazy val system: ActorSystem[SpawnProtocol.Command] = frameworkTestKit.actorSystem
  implicit lazy val ec: ExecutionContext                       = frameworkTestKit.frameworkWiring.actorRuntime.ec
  implicit lazy val askTimeout: Timeout                        = Timeout(10.seconds)

  private lazy val eventService: EventService = frameworkTestKit.frameworkWiring.eventServiceFactory.make(locationService)
  lazy val eventSubscriber: EventSubscriber   = eventService.defaultSubscriber
  lazy val eventPublisher: EventPublisher     = eventService.defaultPublisher

  private val sequencerWirings: mutable.Buffer[SequencerWiring] = mutable.Buffer.empty

  override implicit def patienceConfig: PatienceConfig = PatienceConfig(10.seconds)

  override def beforeAll(): Unit = {
    super.beforeAll()
  }

  override def afterAll(): Unit = {
    shutdownAllSequencers()
    super.afterAll()
  }

  def shutdownAllSequencers(): Unit = {
    sequencerWirings.foreach(_.sequencerServer.shutDown())
    sequencerWirings.clear()
  }

  def spawnSequencer(
      packageId: String,
      observingMode: String,
      sequenceComponentName: Option[String] = None
  ): Either[LoadScriptError, AkkaLocation] = {
    val wiring = new SequencerWiring(packageId, observingMode, sequenceComponentName)
    sequencerWirings += wiring
    wiring.sequencerServer.start()
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
