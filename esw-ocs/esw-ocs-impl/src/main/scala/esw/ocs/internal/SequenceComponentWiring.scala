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

// $COVERAGE-OFF$
private[ocs] class SequenceComponentWiring(prefix: Prefix) {
  private val registrationRetryCount = 10

  lazy val actorRuntime = new ActorRuntime(prefix.prefix)
  import actorRuntime._

  private lazy val locationService: LocationService = HttpLocationServiceFactory.makeLocalClient

  private lazy val locationServiceUtils: LocationServiceUtils = new LocationServiceUtils(locationService)

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
    locationServiceUtils.registerWithRetry(registration(), registrationRetryCount).block

}
// $COVERAGE-ON$
