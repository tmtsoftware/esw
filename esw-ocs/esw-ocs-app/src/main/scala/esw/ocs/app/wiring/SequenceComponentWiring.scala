package esw.ocs.app.wiring

import akka.actor.typed.ActorRef
import akka.actor.typed.SpawnProtocol.Spawn
import akka.actor.typed.scaladsl.AskPattern.Askable
import csw.framework.internal.wiring.ActorRuntime
import csw.location.models.AkkaLocation
import csw.params.core.models.Prefix
import esw.ocs.api.protocol.RegistrationError
import esw.ocs.impl.core.SequenceComponentBehavior
import esw.ocs.impl.internal.{SequenceComponentRegistration, SequencerServerFactory}
import esw.ocs.impl.messages.SequenceComponentMsg
import esw.ocs.impl.syntax.FutureSyntax.FutureOps

import scala.concurrent.Future

// $COVERAGE-OFF$
private[ocs] class SequenceComponentWiring(prefix: Prefix, sequencerServerFactory: SequencerServerFactory) {
  private val registrationRetryCount = 10

  lazy val cswServicesWiring = new CswServicesWiring(prefix.prefix)
  import cswServicesWiring._
  import frameworkWiring._
  import frameworkWiring.actorRuntime._

  lazy val actorRuntime: ActorRuntime = frameworkWiring.actorRuntime

  def sequenceComponentFactory(sequenceComponentName: String): Future[ActorRef[SequenceComponentMsg]] = {
    log.info(s"Starting sequence component with name: $sequenceComponentName")
    typedSystem ? Spawn(
      SequenceComponentBehavior.behavior(sequenceComponentName, log, sequencerServerFactory),
      sequenceComponentName
    )
  }

  private lazy val sequenceComponentRegistration =
    new SequenceComponentRegistration(prefix, locationService, sequenceComponentFactory)

  def start(): Either[RegistrationError, AkkaLocation] =
    sequenceComponentRegistration.registerWithRetry(registrationRetryCount).block

}
// $COVERAGE-ON$
