package esw.ocs.app.wiring

import akka.actor.typed.SpawnProtocol.Spawn
import akka.actor.typed.scaladsl.AskPattern._
import akka.actor.typed.{ActorRef, ActorSystem, Props, SpawnProtocol}
import akka.util.Timeout
import csw.location.api.models.AkkaLocation
import csw.location.api.scaladsl.LocationService
import csw.location.client.ActorSystemFactory
import csw.location.client.scaladsl.HttpLocationServiceFactory
import csw.logging.api.scaladsl.Logger
import csw.logging.client.scaladsl.LoggerFactory
import csw.prefix.models.{Prefix, Subsystem}
import esw.commons.utils.location.EswLocationError.RegistrationError
import esw.constants.CommonTimeouts
import esw.http.core.wiring.ActorRuntime
import esw.ocs.api.actor.messages.SequenceComponentMsg
import esw.ocs.impl.core.SequenceComponentBehavior
import esw.ocs.impl.internal.{SequenceComponentRegistration, SequencerServerFactory}

import scala.concurrent.{Await, Future}

// $COVERAGE-OFF$
private[esw] class SequenceComponentWiring(
    subsystem: Subsystem,
    name: Option[String],
    agentPrefix: Option[Prefix],
    sequencerServerFactory: SequencerServerFactory
) {
  private val registrationRetryCount = 10
  private[wiring] lazy val actorSystem: ActorSystem[SpawnProtocol.Command] =
    ActorSystemFactory.remote(SpawnProtocol(), "sequence-component-system")

  final lazy val actorRuntime: ActorRuntime = new ActorRuntime(actorSystem)
  import actorRuntime._
  lazy val locationService: LocationService = HttpLocationServiceFactory.makeLocalClient(actorSystem)

  implicit lazy val timeout: Timeout = CommonTimeouts.Wiring

  def sequenceComponentFactory(sequenceComponentPrefix: Prefix): Future[ActorRef[SequenceComponentMsg]] = {
    val loggerFactory                   = new LoggerFactory(sequenceComponentPrefix)
    val sequenceComponentLogger: Logger = loggerFactory.getLogger

    sequenceComponentLogger.info(s"Starting sequence component with name: $sequenceComponentPrefix")
    typedSystem ? { (replyTo: ActorRef[ActorRef[SequenceComponentMsg]]) =>
      Spawn(
        new SequenceComponentBehavior(
          sequenceComponentPrefix,
          sequenceComponentLogger,
          locationService,
          sequencerServerFactory
        ).idle,
        sequenceComponentPrefix.toString,
        Props.empty,
        replyTo
      )
    }
  }

  private lazy val sequenceComponentRegistration =
    new SequenceComponentRegistration(subsystem, name, agentPrefix, locationService, sequenceComponentFactory)

  def start(): Either[RegistrationError, AkkaLocation] =
    Await.result(sequenceComponentRegistration.registerSequenceComponent(registrationRetryCount), CommonTimeouts.Wiring)

}
private[ocs] object SequenceComponentWiring {
  def make(
      subsystem: Subsystem,
      name: Option[String],
      agentPrefix: Option[Prefix],
      sequencerServerFactory: SequencerServerFactory,
      _actorSystem: ActorSystem[SpawnProtocol.Command]
  ): SequenceComponentWiring =
    new SequenceComponentWiring(subsystem, name, agentPrefix, sequencerServerFactory) {
      override private[wiring] lazy val actorSystem = _actorSystem
    }
}
// $COVERAGE-ON$
