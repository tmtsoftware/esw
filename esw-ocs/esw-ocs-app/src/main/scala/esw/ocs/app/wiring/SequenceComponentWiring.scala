package esw.ocs.app.wiring

import akka.actor.typed.{ActorRef, Props}
import akka.actor.typed.SpawnProtocol.Spawn
import akka.actor.typed.scaladsl.AskPattern._
import akka.util.Timeout
import csw.location.models.AkkaLocation
import csw.logging.api.scaladsl.Logger
import csw.logging.client.scaladsl.LoggerFactory
import csw.params.core.models.Subsystem
import esw.http.core.wiring.{ActorRuntime, CswWiring}
import esw.ocs.api.protocol.LoadScriptError
import esw.ocs.impl.core.SequenceComponentBehavior
import esw.ocs.impl.internal.{SequenceComponentRegistration, SequencerServerFactory, Timeouts}
import esw.ocs.impl.messages.SequenceComponentMsg
import esw.ocs.impl.syntax.FutureSyntax.FutureOps

import scala.concurrent.Future

// $COVERAGE-OFF$
private[ocs] class SequenceComponentWiring(
    subsystem: Subsystem,
    name: Option[String],
    sequencerServerFactory: SequencerServerFactory
) {
  private val registrationRetryCount = 10

  lazy val cswWiring = new CswWiring()
  import cswWiring._
  import cswWiring.actorRuntime._
  lazy val actorRuntime: ActorRuntime = cswWiring.actorRuntime

  implicit lazy val timeout: Timeout = Timeouts.DefaultTimeout

  def sequenceComponentFactory(sequenceComponentName: String): Future[ActorRef[SequenceComponentMsg]] = {
    val loggerFactory                   = new LoggerFactory(sequenceComponentName)
    val sequenceComponentLogger: Logger = loggerFactory.getLogger

    sequenceComponentLogger.info(s"Starting sequence component with name: $sequenceComponentName")
    typedSystem ? { x =>
      Spawn(
        SequenceComponentBehavior.behavior(sequenceComponentName, sequenceComponentLogger, sequencerServerFactory),
        sequenceComponentName,
        Props.empty,
        x
      )
    }
  }

  private lazy val sequenceComponentRegistration =
    new SequenceComponentRegistration(subsystem, name, locationService, sequenceComponentFactory)

  def start(): Either[LoadScriptError, AkkaLocation] =
    sequenceComponentRegistration.registerSequenceComponent(registrationRetryCount).block

}
// $COVERAGE-ON$
