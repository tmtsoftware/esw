package esw.ocs.internal

import akka.actor.typed.ActorRef
import akka.actor.typed.SpawnProtocol.Spawn
import akka.actor.typed.scaladsl.AskPattern.Askable
import csw.location.api.extensions.ActorExtension.RichActor
import csw.location.api.scaladsl.LocationService
import csw.location.client.scaladsl.HttpLocationServiceFactory
import csw.location.models.Connection.AkkaConnection
import csw.location.models.{AkkaLocation, AkkaRegistration, ComponentId, ComponentType}
import csw.params.core.models.Prefix
import esw.ocs.api.models.messages.SequenceComponentMsg
import esw.ocs.api.models.messages.error.RegistrationError
import esw.ocs.core.SequenceComponentBehavior
import esw.ocs.syntax.FutureSyntax.FutureOps
import esw.ocs.utils.RegistrationUtils

private[ocs] class SequenceComponentWiring(name: String) {
  lazy val actorRuntime = new ActorRuntime(name)
  import actorRuntime._

  lazy val sequenceComponentRef: ActorRef[SequenceComponentMsg] =
    (typedSystem ? Spawn(SequenceComponentBehavior.behavior, name)).block

  private lazy val locationService: LocationService = HttpLocationServiceFactory.makeLocalClient

  //fixme: should this come from conf file?
  private lazy val prefix = Prefix("sequence-component")

  def start(): Either[RegistrationError, AkkaLocation] = {
    val registration =
      AkkaRegistration(AkkaConnection(ComponentId(name, ComponentType.Service)), prefix, sequenceComponentRef.toURI)

    RegistrationUtils.register(locationService, registration)(coordinatedShutdown).block
  }

}
