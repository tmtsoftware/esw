package esw.ocs.framework

import akka.actor.typed.ActorRef
import akka.actor.typed.SpawnProtocol.Spawn
import akka.actor.typed.scaladsl.AskPattern.Askable
import csw.command.client.messages.CommandResponseManagerMessage
import csw.command.client.{CRMCacheProperties, CommandResponseManager, CommandResponseManagerActor}
import csw.location.api.extensions.ActorExtension.RichActor
import csw.location.api.scaladsl.LocationService
import csw.location.client.scaladsl.HttpLocationServiceFactory
import csw.location.model.scaladsl.Connection.AkkaConnection
import csw.location.model.scaladsl.{AkkaLocation, AkkaRegistration, ComponentId, ComponentType}
import esw.ocs.async.macros.StrandEc
import esw.ocs.framework.api.models.messages.{LoadScriptError, SequencerMsg}
import esw.ocs.framework.core._
import esw.ocs.framework.core.internal.{ScriptLoader, SequencerConfig}
import esw.ocs.framework.dsl.{CswServices, Script}
import esw.ocs.framework.syntax.FutureSyntax.FutureOps

//todo: make package-private to esw as private
class SequencerWiring(val sequencerId: String, val observingMode: String) {

  private lazy val settings: SequencerConfig = new SequencerConfig(sequencerId, observingMode)
  private lazy val actorRuntime              = new ActorRuntime(settings.name)
  import actorRuntime._

  private lazy val engine      = new Engine()
  private lazy val componentId = ComponentId(settings.name, ComponentType.Sequencer)

  private lazy val crmRef: ActorRef[CommandResponseManagerMessage] =
    (typedSystem ? Spawn(CommandResponseManagerActor.behavior(CRMCacheProperties(), loggerFactory), "crm")).block
  private lazy val commandResponseManager: CommandResponseManager = new CommandResponseManager(crmRef)

  private[esw] lazy val sequencerRef: ActorRef[SequencerMsg] =
    (typedSystem ? Spawn(SequencerBehavior.behavior(sequencer, script), settings.name)).block

  //Pass lambda to break circular dependency shown below.
  //SequencerRef -> Script -> cswServices -> SequencerOperator -> SequencerRef
  private lazy val sequenceOperatorFactory = () => new SequenceOperator(sequencerRef)

  private lazy val cswServices    = new CswServices(sequenceOperatorFactory, commandResponseManager)
  private lazy val script: Script = new ScriptLoader(sequencerId, observingMode).load(cswServices)
  private[esw] lazy val sequencer = new Sequencer(commandResponseManager)(StrandEc(), timeout)

  private[esw] lazy val sequenceEditorClient = new SequenceEditorClient(sequencerRef)

  private lazy val locationService: LocationService = HttpLocationServiceFactory.makeLocalClient

  // fixme: no need to block
  def shutDown(): Unit = {
    locationService.unregister(AkkaConnection(componentId)).block
    sequenceEditorClient.shutdown().block
    typedSystem.terminate()
  }

  // fixme: do not block and return Future[AkkaLocation]?
  //  onComplete gives handle to Try
  def start(): Either[LoadScriptError, AkkaLocation] = {
    val registration = AkkaRegistration(AkkaConnection(componentId), settings.prefix, sequencerRef.toURI)
    log.info(s"Registering ${componentId.name} with Location Service using registration: [${registration.toString}]")

    engine.start(sequenceOperatorFactory(), script)

    locationService
      .register(registration)
      .map(x => Right(x.location.asInstanceOf[AkkaLocation]))
      .recover {
        case ex: Throwable => Left(LoadScriptError(ex))
      }
      .block
  }
}
