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
import esw.ocs.api.models.messages.{RegistrationError, SequenceComponentMsg}
import esw.ocs.core.SequenceComponentBehavior
import esw.ocs.syntax.FutureSyntax.FutureOps
import esw.ocs.utils.RegistrationUtils

// $COVERAGE-OFF$
private[ocs] class SequenceComponentWiring(prefixStr: String) {
  private val prefix: Prefix        = Prefix(prefixStr)
  private val sequenceComponentName = s"${prefix.subsystem}_$prefixStr"

  lazy val actorRuntime = new ActorRuntime(sequenceComponentName)
  import actorRuntime._

  lazy val sequenceComponentRef: ActorRef[SequenceComponentMsg] =
    (typedSystem ? Spawn(SequenceComponentBehavior.behavior, sequenceComponentName)).block

  private lazy val locationService: LocationService = HttpLocationServiceFactory.makeLocalClient

  def start(): Either[RegistrationError, AkkaLocation] = {
    val registration =
      AkkaRegistration(
        AkkaConnection(ComponentId(sequenceComponentName, ComponentType.SequenceComponent)),
        prefix,
        sequenceComponentRef.toURI
      )

    RegistrationUtils.register(locationService, registration)(coordinatedShutdown).block
  }
}
// $COVERAGE-ON$
