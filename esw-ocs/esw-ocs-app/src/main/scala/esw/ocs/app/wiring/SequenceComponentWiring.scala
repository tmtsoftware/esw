package esw.ocs.app.wiring

import akka.actor.typed.ActorRef
import akka.actor.typed.SpawnProtocol.Spawn
import akka.actor.typed.scaladsl.AskPattern.Askable
import akka.util.Timeout
import csw.location.api.extensions.ActorExtension.RichActor
import csw.location.models.Connection.AkkaConnection
import csw.location.models.{AkkaLocation, AkkaRegistration, ComponentId, ComponentType}
import csw.params.core.models.Prefix
import esw.dsl.sequence_manager.LocationServiceUtil
import esw.http.core.wiring.{ActorRuntime, CswWiring}
import esw.ocs.api.protocol.RegistrationError
import esw.ocs.impl.core.SequenceComponentBehavior
import esw.ocs.impl.internal.{SequencerServerFactory, Timeouts}
import esw.ocs.impl.messages.SequenceComponentMsg
import esw.ocs.impl.syntax.FutureSyntax.FutureOps

// $COVERAGE-OFF$
private[ocs] class SequenceComponentWiring(prefix: Prefix, sequencerServerFactory: SequencerServerFactory) {

  lazy val cswWiring = new CswWiring(prefix.prefix)
  import cswWiring._
  import cswWiring.actorRuntime._
  lazy val actorRuntime: ActorRuntime    = cswWiring.actorRuntime
  private lazy val sequenceComponentName = prefix.prefix
  implicit lazy val timeout: Timeout     = Timeouts.DefaultTimeout

  private lazy val sequenceComponentRef: ActorRef[SequenceComponentMsg] = (typedSystem ? Spawn(
    SequenceComponentBehavior.behavior(sequenceComponentName, logger, sequencerServerFactory),
    sequenceComponentName
  )).block

  def start(): Either[RegistrationError, AkkaLocation] = {
    val registration = AkkaRegistration(
      AkkaConnection(ComponentId(sequenceComponentName, ComponentType.SequenceComponent)),
      prefix,
      sequenceComponentRef.toURI
    )

    new LocationServiceUtil(locationService).register(registration).block
  }

}
// $COVERAGE-ON$
