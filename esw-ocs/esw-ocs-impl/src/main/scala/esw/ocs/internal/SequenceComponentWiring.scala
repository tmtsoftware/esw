package esw.ocs.internal

import akka.actor.typed.ActorRef
import akka.actor.typed.SpawnProtocol.Spawn
import akka.actor.typed.scaladsl.AskPattern.Askable
import csw.framework.internal.wiring.ActorRuntime
import csw.location.models.AkkaLocation
import csw.params.core.models.Prefix
import esw.ocs.api.models.messages.{RegistrationError, SequenceComponentMsg}
import esw.ocs.core.SequenceComponentBehavior
import esw.ocs.syntax.FutureSyntax.FutureOps

// $COVERAGE-OFF$
private[ocs] class SequenceComponentWiring(prefix: Prefix) {
  private val registrationRetryCount = 10

  lazy val cswServicesWiring = new CswServicesWiring(prefix.prefix)
  import cswServicesWiring._
  import frameworkWiring._
  import frameworkWiring.actorRuntime._

  lazy val actorRuntime: ActorRuntime = frameworkWiring.actorRuntime

  def sequenceComponentFactory(sequenceComponentName: String): ActorRef[SequenceComponentMsg] = {
    log.info(s"Starting sequence component with name: $sequenceComponentName")
    (typedSystem ? Spawn(SequenceComponentBehavior.behavior(sequenceComponentName, log), sequenceComponentName)).block
  }

  private lazy val sequenceComponentRegistration =
    new SequenceComponentRegistration(prefix, locationService, sequenceComponentFactory)

  def start(): Either[RegistrationError, AkkaLocation] =
    sequenceComponentRegistration.registerWithRetry(registrationRetryCount).block

}
// $COVERAGE-ON$
