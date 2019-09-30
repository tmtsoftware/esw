package esw.ocs.app.wiring

import akka.actor.typed.ActorRef
import akka.actor.typed.SpawnProtocol.Spawn
import akka.actor.typed.scaladsl.AskPattern.Askable
import akka.util.Timeout
import csw.location.models.AkkaLocation
import csw.params.core.models.Prefix
import esw.http.core.wiring.{ActorRuntime, CswWiring}
import esw.ocs.api.protocol.RegistrationError
import esw.ocs.impl.core.SequenceComponentBehavior
import esw.ocs.impl.internal.{SequenceComponentRegistration, SequencerServerFactory, Timeouts}
import esw.ocs.impl.messages.SequenceComponentMsg
import esw.ocs.impl.syntax.FutureSyntax.FutureOps

import scala.concurrent.Future

// $COVERAGE-OFF$
private[ocs] class SequenceComponentWiring(prefix: Prefix, sequencerServerFactory: SequencerServerFactory) {
  private val registrationRetryCount = 10

  lazy val cswWiring = new CswWiring(prefix.prefix)
  import cswWiring._
  import cswWiring.actorRuntime._
  lazy val actorRuntime: ActorRuntime = cswWiring.actorRuntime

  implicit lazy val timeout: Timeout = Timeouts.DefaultTimeout

  def sequenceComponentFactory(sequenceComponentName: String): Future[ActorRef[SequenceComponentMsg]] = {
    logger.info(s"Starting sequence component with name: $sequenceComponentName")
    typedSystem ? Spawn(
      SequenceComponentBehavior.behavior(sequenceComponentName, logger, sequencerServerFactory),
      sequenceComponentName
    )
  }

  private lazy val sequenceComponentRegistration =
    new SequenceComponentRegistration(prefix, locationService, sequenceComponentFactory)

  def start(): Either[RegistrationError, AkkaLocation] =
    sequenceComponentRegistration.registerWithRetry(registrationRetryCount).block

}
// $COVERAGE-ON$
