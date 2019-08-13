package esw.ocs.internal

import akka.Done
import akka.actor.typed.ActorRef
import akka.actor.typed.SpawnProtocol.Spawn
import akka.actor.typed.scaladsl.AskPattern.Askable
import com.typesafe.config.ConfigFactory
import csw.command.client.messages.CommandResponseManagerMessage
import csw.command.client.{CRMCacheProperties, CommandResponseManager, CommandResponseManagerActor}
import csw.location.api.extensions.ActorExtension.RichActor
import csw.location.api.scaladsl.LocationService
import csw.location.client.scaladsl.HttpLocationServiceFactory
import csw.location.models.Connection.AkkaConnection
import csw.location.models.{AkkaLocation, AkkaRegistration, ComponentId, ComponentType}
import esw.ocs.api.models.SequencerState
import esw.ocs.api.models.messages.RegistrationError
import esw.ocs.api.models.messages.SequencerMessages.EswSequencerMessage
import esw.ocs.core._
import esw.ocs.dsl.utils.ScriptLoader
import esw.ocs.dsl.{CswServices, Script}
import esw.ocs.macros.StrandEc
import esw.ocs.syntax.FutureSyntax.FutureOps
import esw.utils.csw.LocationServiceUtils

import scala.concurrent.Future
// $COVERAGE-OFF$
private[ocs] class SequencerWiring(val sequencerId: String, val observingMode: String, sequenceComponentName: Option[String]) {
  private lazy val config          = ConfigFactory.load()
  private lazy val sequencerConfig = SequencerConfig.from(config, sequencerId, observingMode, sequenceComponentName)
  import sequencerConfig._
  lazy val actorRuntime = new ActorRuntime(sequencerName)
  import actorRuntime._

  private lazy val engine      = new Engine()
  private lazy val componentId = ComponentId(sequencerName, ComponentType.Sequencer)

  private lazy val crmRef: ActorRef[CommandResponseManagerMessage] =
    (typedSystem ? Spawn(CommandResponseManagerActor.behavior(CRMCacheProperties(), loggerFactory), "crm")).block
  private lazy val commandResponseManager: CommandResponseManager   = new CommandResponseManager(crmRef)
  private val sequencerCommandService: SequencerCommandServiceUtils = new SequencerCommandServiceUtils

  lazy val sequencerBehavior = new SequencerBehavior(componentId, script, locationService, commandResponseManager)

  lazy val sequencerRef: ActorRef[EswSequencerMessage] =
    (typedSystem ? Spawn(sequencerBehavior.idle(SequencerState.initial), sequencerName)).block

  //Pass lambda to break circular dependency shown below.
  //SequencerRef -> Script -> cswServices -> SequencerOperator -> SequencerRef
  private lazy val sequenceOperatorFactory = () => new SequenceOperator(sequencerRef)

  private lazy val cswServices =
    new CswServices(sequenceOperatorFactory, commandResponseManager, sequencerCommandService, locationServiceUtils)
  private lazy val script: Script = ScriptLoader.load(scriptClass, cswServices)
  lazy val strandEc               = StrandEc()

  lazy val sequencerSupervisorClient = new SequencerSupervisorClient(sequencerRef)

  private lazy val locationService: LocationService           = HttpLocationServiceFactory.makeLocalClient
  private lazy val locationServiceUtils: LocationServiceUtils = new LocationServiceUtils(locationService)

  def shutDown(): Future[Done] = sequencerSupervisorClient.shutdown().map(_ => Done)

  def start(): Either[RegistrationError, AkkaLocation] = {
    engine.start(sequenceOperatorFactory(), script)

    val registration = AkkaRegistration(AkkaConnection(componentId), prefix, sequencerRef.toURI)
    locationServiceUtils.register(registration)(typedSystem).block
  }
}
// $COVERAGE-ON$
