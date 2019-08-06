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
private[ocs] class SequenceComponentWiring(prefixStr: String) {
  private val registrationRetryCount = 10
  private val prefix: Prefix         = Prefix(prefixStr)

  lazy val actorRuntime = new ActorRuntime(prefixStr)
  import actorRuntime._

  lazy val sequenceComponentRef: ActorRef[SequenceComponentMsg] =
    (typedSystem ? Spawn(SequenceComponentBehavior.behavior, prefixStr)).block

  private lazy val locationService: LocationService = HttpLocationServiceFactory.makeLocalClient

  private lazy val locationServiceUtils: LocationServiceUtils = new LocationServiceUtils(locationService)

  def registration(): AkkaRegistration = {
    val subsystem = prefix.subsystem
    locationServiceUtils
      .listBy(subsystem, ComponentType.SequenceComponent)
      .map { sequenceComponents =>
        val uniqueId              = s"${sequenceComponents.length + 1}"
        val sequenceComponentName = s"${subsystem}_$uniqueId"
        AkkaRegistration(
          AkkaConnection(ComponentId(sequenceComponentName, ComponentType.SequenceComponent)),
          prefix,
          sequenceComponentRef.toURI
        )
      }
      .block
  }

  def start(): Either[RegistrationError, AkkaLocation] =
    locationServiceUtils.registerWithRetry(registration(), registrationRetryCount).block

}
// $COVERAGE-ON$
