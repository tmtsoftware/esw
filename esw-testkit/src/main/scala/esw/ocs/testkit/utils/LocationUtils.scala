package esw.ocs.testkit.utils

import org.apache.pekko.actor.typed.{ActorRef, ActorSystem, SpawnProtocol}
import csw.command.client.messages.sequencer.SequencerMsg
import csw.location.api.extensions.URIExtension.RichURI
import csw.location.api.models.Connection.{PekkoConnection, HttpConnection}
import csw.location.api.models.{PekkoLocation, ComponentId, ComponentType, HttpLocation}
import csw.location.api.scaladsl.LocationService
import csw.prefix.models.{Prefix, Subsystem}
import esw.constants.CommonTimeouts
import esw.ocs.api.actor.messages.SequenceComponentMsg
import esw.ocs.api.models.{ObsMode, Variation}

import scala.concurrent.{ExecutionContext, Future}

trait LocationUtils extends BaseTestSuite {

  def locationService: LocationService
  implicit def actorSystem: ActorSystem[SpawnProtocol.Command]
  implicit def ec: ExecutionContext = actorSystem.executionContext

  def resolveHTTPLocationAsync(prefix: Prefix, componentType: ComponentType): Future[HttpLocation] =
    locationService.resolve(HttpConnection(ComponentId(prefix, componentType)), CommonTimeouts.ResolveLocation).map(_.value)

  def resolveHTTPLocation(prefix: Prefix, componentType: ComponentType): HttpLocation =
    resolveHTTPLocationAsync(prefix, componentType).futureValue

  def resolveSequencerLocation(prefix: Prefix): PekkoLocation = resolvePekkoLocation(prefix, ComponentType.Sequencer)

  def resolveSequencerLocation(subsystem: Subsystem, obsMode: ObsMode): PekkoLocation =
    resolveSequencerLocation(Prefix(subsystem, obsMode.name))

  def resolveSequencer(subsystem: Subsystem, obsMode: ObsMode): ActorRef[SequencerMsg] =
    resolveSequencerLocation(subsystem, obsMode).uri.toActorRef.unsafeUpcast[SequencerMsg]

  def resolveSequencer(subsystem: Subsystem, obsMode: ObsMode, variation: Variation): ActorRef[SequencerMsg] =
    resolveSequencerLocation(Variation.prefix(subsystem, obsMode, Some(variation))).uri.toActorRef.unsafeUpcast[SequencerMsg]

  def resolveSequenceComponentLocation(prefix: Prefix): PekkoLocation =
    resolvePekkoLocation(prefix, ComponentType.SequenceComponent)

  def resolvePekkoLocation(prefix: Prefix, componentType: ComponentType): PekkoLocation =
    locationService
      .resolve(PekkoConnection(ComponentId(prefix, componentType)), CommonTimeouts.ResolveLocation)
      .futureValue
      .value

  def resolveSequenceComponent(prefix: Prefix): ActorRef[SequenceComponentMsg] =
    resolveSequenceComponentLocation(prefix).uri.toActorRef.unsafeUpcast[SequenceComponentMsg]

}
