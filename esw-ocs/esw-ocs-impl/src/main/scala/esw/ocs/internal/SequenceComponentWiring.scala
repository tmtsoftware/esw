package esw.ocs.internal

import csw.location.api.scaladsl.LocationService
import csw.location.client.scaladsl.HttpLocationServiceFactory
import csw.location.models.AkkaLocation
import csw.params.core.models.Prefix
import esw.ocs.api.models.messages.RegistrationError
import esw.ocs.syntax.FutureSyntax.FutureOps
import esw.utils.csw.LocationServiceUtils

// $COVERAGE-OFF$
private[ocs] class SequenceComponentWiring(prefix: Prefix) {
  private val registrationRetryCount = 10

  lazy val actorRuntime = new ActorRuntime(prefix.prefix)
  import actorRuntime._

  private lazy val locationService: LocationService = HttpLocationServiceFactory.makeLocalClient

  private lazy val locationServiceUtils: LocationServiceUtils = new LocationServiceUtils(locationService)
  private lazy val sequenceComponentRegistration =
    new SequenceComponentRegistration(prefix, locationService, locationServiceUtils)

  def start(): Either[RegistrationError, AkkaLocation] =
    sequenceComponentRegistration.registerWithRetry(registrationRetryCount).block

}
// $COVERAGE-ON$
