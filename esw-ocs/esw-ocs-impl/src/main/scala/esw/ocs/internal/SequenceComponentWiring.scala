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
import esw.ocs.utils.LocationServiceUtils

import scala.concurrent.{ExecutionContext, Future}

// $COVERAGE-OFF$
private[ocs] class SequenceComponentWiring(prefixStr: String) {
  private val registrationRetryCount = 10
  private val prefix: Prefix         = Prefix(prefixStr)

  lazy val actorRuntime = new ActorRuntime(prefixStr)
  import actorRuntime._

  lazy val sequenceComponentRef: ActorRef[SequenceComponentMsg] =
    (typedSystem ? Spawn(SequenceComponentBehavior.behavior, prefixStr)).block

  private lazy val locationService: LocationService = HttpLocationServiceFactory.makeLocalClient

  private lazy val locationServiceUtils: LocationServiceUtils = new LocationServiceUtils(locationService)

  def start(): Either[RegistrationError, AkkaLocation] = registerSequenceComponent().block

  private def registerSequenceComponent()(implicit ec: ExecutionContext): Future[Either[RegistrationError, AkkaLocation]] = {
    val subsystem = prefix.subsystem

    locationServiceUtils
      .listBy(subsystem, ComponentType.SequenceComponent)
      .flatMap { sequenceComponents =>
        val uniqueId              = s"${sequenceComponents.length + 1}"
        val sequenceComponentName = s"${subsystem}_$uniqueId"
        val registration =
          AkkaRegistration(
            AkkaConnection(ComponentId(sequenceComponentName, ComponentType.SequenceComponent)),
            prefix,
            sequenceComponentRef.toURI
          )
        locationServiceUtils.registerWithRetry(registration, registrationRetryCount)(coordinatedShutdown)
      }
  }
}
// $COVERAGE-ON$
