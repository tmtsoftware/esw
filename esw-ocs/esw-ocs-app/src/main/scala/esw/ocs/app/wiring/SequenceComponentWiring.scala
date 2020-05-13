package esw.ocs.app.wiring

import akka.actor.typed.SpawnProtocol.Spawn
import akka.actor.typed.scaladsl.AskPattern._
import akka.actor.typed.{ActorRef, ActorSystem, Props, SpawnProtocol}
import akka.util.Timeout
import csw.location.api.models.AkkaLocation
import csw.location.client.ActorSystemFactory
import csw.logging.api.scaladsl.Logger
import csw.logging.client.scaladsl.LoggerFactory
import csw.prefix.models.{Prefix, Subsystem}
import esw.commons.Timeouts
import esw.http.core.wiring.{ActorRuntime, CswWiring}
import esw.ocs.api.actor.messages.SequenceComponentMsg
import esw.ocs.api.protocol.ScriptError
import esw.ocs.impl.core.SequenceComponentBehavior
import esw.ocs.impl.internal.{SequenceComponentRegistration, SequencerServerFactory}

import scala.concurrent.{Await, Future}

// $COVERAGE-OFF$
private[esw] class SequenceComponentWiring(
    subsystem: Subsystem,
    name: Option[String],
    sequencerServerFactory: SequencerServerFactory
) {
  private val registrationRetryCount = 10
  private[wiring] lazy val actorSystem: ActorSystem[SpawnProtocol.Command] =
    ActorSystemFactory.remote(SpawnProtocol(), "sequence-component-system")

  lazy val cswWiring = new CswWiring(actorSystem)
  import cswWiring._
  import cswWiring.actorRuntime._
  lazy val actorRuntime: ActorRuntime = cswWiring.actorRuntime

  implicit lazy val timeout: Timeout = Timeouts.DefaultTimeout

  def sequenceComponentFactory(sequenceComponentPrefix: Prefix): Future[ActorRef[SequenceComponentMsg]] = {
    val loggerFactory                   = new LoggerFactory(sequenceComponentPrefix)
    val sequenceComponentLogger: Logger = loggerFactory.getLogger

    sequenceComponentLogger.info(s"Starting sequence component with name: $sequenceComponentPrefix")
    typedSystem ? { replyTo =>
      Spawn(
        SequenceComponentBehavior.behavior(sequenceComponentPrefix, sequenceComponentLogger, sequencerServerFactory),
        sequenceComponentPrefix.toString,
        Props.empty,
        replyTo
      )
    }
  }

  private lazy val sequenceComponentRegistration =
    new SequenceComponentRegistration(subsystem, name, locationService, sequenceComponentFactory)

  def start(): Either[ScriptError, AkkaLocation] =
    Await.result(sequenceComponentRegistration.registerSequenceComponent(registrationRetryCount), Timeouts.DefaultTimeout)

}
private[ocs] object SequenceComponentWiring {
  def make(
      subsystem: Subsystem,
      name: Option[String],
      sequencerServerFactory: SequencerServerFactory,
      _actorSystem: ActorSystem[SpawnProtocol.Command]
  ): SequenceComponentWiring =
    new SequenceComponentWiring(subsystem, name, sequencerServerFactory) {
      override private[wiring] lazy val actorSystem = _actorSystem
    }
}
// $COVERAGE-ON$
