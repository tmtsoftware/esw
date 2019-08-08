package esw.ocs.internal

import akka.actor.CoordinatedShutdown
import akka.actor.typed.SpawnProtocol.Spawn
import akka.actor.typed.scaladsl.AskPattern.Askable
import akka.actor.typed.scaladsl.adapter.TypedActorSystemOps
import akka.actor.typed.{ActorRef, ActorSystem}
import csw.location.api.exceptions.OtherLocationIsRegistered
import csw.location.api.extensions.ActorExtension.RichActor
import csw.location.api.extensions.URIExtension._
import csw.location.api.scaladsl.{LocationService, RegistrationResult}
import csw.location.client.scaladsl.HttpLocationServiceFactory
import csw.location.models.Connection.AkkaConnection
import csw.location.models.{AkkaLocation, AkkaRegistration, ComponentId, ComponentType}
import csw.params.core.models.Prefix
import esw.ocs.api.models.messages.SequenceComponentMsg.Stop
import esw.ocs.api.models.messages.{RegistrationError, SequenceComponentMsg}
import esw.ocs.core.SequenceComponentBehavior
import esw.ocs.syntax.FutureSyntax.FutureOps
import esw.utils.csw.LocationServiceUtils

import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

// $COVERAGE-OFF$
private[ocs] class SequenceComponentWiring(prefix: Prefix) {
  private val registrationRetryCount = 10

  lazy val actorRuntime = new ActorRuntime(prefix.prefix)
  import actorRuntime._

  private lazy val locationService: LocationService = HttpLocationServiceFactory.makeLocalClient

  private lazy val locationServiceUtils: LocationServiceUtils = new LocationServiceUtils(locationService)

  private def addCoordinatedShutdownTask(
      coordinatedShutdown: CoordinatedShutdown,
      registrationResult: RegistrationResult
  ): Unit = {
    coordinatedShutdown.addTask(
      CoordinatedShutdown.PhaseBeforeServiceUnbind,
      s"unregistering-${registrationResult.location}"
    )(() => registrationResult.unregister())
  }

  private def registerWithRetry(akkaRegistration: => AkkaRegistration, retryCount: Int)(
      implicit actorSystem: ActorSystem[_]
  ): Future[Either[RegistrationError, AkkaLocation]] = {
    implicit val ec: ExecutionContext = actorSystem.executionContext
    locationService
      .register(akkaRegistration)
      .map { result =>
        addCoordinatedShutdownTask(CoordinatedShutdown(actorSystem.toUntyped), result)
        Right(result.location.asInstanceOf[AkkaLocation])
      }
      .recoverWith {
        case OtherLocationIsRegistered(_) if retryCount > 0 =>
          //kill actor ref if registration fails. Retry attempt will create new actor ref
          akkaRegistration.actorRefURI.toActorRef.unsafeUpcast[SequenceComponentMsg] ! Stop
          registerWithRetry(akkaRegistration, retryCount - 1)
        case NonFatal(e) => Future.successful(Left(RegistrationError(e.getMessage)))
      }
  }

  private def generateSequenceComponentName(): String = {
    val subsystem = prefix.subsystem
    locationServiceUtils
      .listBy(subsystem, ComponentType.SequenceComponent)
      .map { sequenceComponents =>
        val uniqueId = s"${sequenceComponents.length + 1}"
        s"${subsystem}_$uniqueId"
      }
      .block
  }

  private def registration(): AkkaRegistration = {
    val sequenceComponentName = generateSequenceComponentName()
    val sequenceComponentRef: ActorRef[SequenceComponentMsg] =
      (typedSystem ? Spawn(SequenceComponentBehavior.behavior(sequenceComponentName), sequenceComponentName)).block
    AkkaRegistration(
      AkkaConnection(ComponentId(sequenceComponentName, ComponentType.SequenceComponent)),
      prefix,
      sequenceComponentRef.toURI
    )
  }

  def start(): Either[RegistrationError, AkkaLocation] =
    registerWithRetry(registration(), registrationRetryCount).block

}
// $COVERAGE-ON$
