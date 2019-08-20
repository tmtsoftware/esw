package esw.ocs.internal

import akka.actor.typed.ActorRef
import akka.actor.typed.SpawnProtocol.Spawn
import akka.actor.typed.scaladsl.AskPattern.Askable
import csw.command.client.messages.CommandResponseManagerMessage
import csw.command.client.{CRMCacheProperties, CommandResponseManager, CommandResponseManagerActor}
import csw.location.api.scaladsl.LocationService
import csw.location.client.scaladsl.HttpLocationServiceFactory
import esw.ocs.core.SequenceOperator
import esw.ocs.dsl.CswServices
import esw.ocs.syntax.FutureSyntax.FutureOps
import esw.utils.csw.LocationServiceUtils

// $COVERAGE-OFF$
private[internal] class CswServicesWiring(actorRuntime: ActorRuntime, sequenceOperatorFactory: () => SequenceOperator) {
  import actorRuntime._

  lazy val locationService: LocationService           = HttpLocationServiceFactory.makeLocalClient
  lazy val locationServiceUtils: LocationServiceUtils = new LocationServiceUtils(locationService)

  lazy val crmRef: ActorRef[CommandResponseManagerMessage] =
    (typedSystem ? Spawn(CommandResponseManagerActor.behavior(CRMCacheProperties(), loggerFactory), "crm")).block
  lazy val commandResponseManager: CommandResponseManager = new CommandResponseManager(crmRef)

  lazy val sequencerCommandService: SequencerCommandServiceUtils = new SequencerCommandServiceUtils
  lazy val cswServices =
    new CswServices(sequenceOperatorFactory, commandResponseManager, sequencerCommandService, locationServiceUtils)

}
// $COVERAGE-ON$
