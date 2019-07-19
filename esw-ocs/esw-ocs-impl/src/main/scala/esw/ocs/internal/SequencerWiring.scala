package esw.ocs.internal

import akka.actor.typed.ActorRef
import akka.actor.typed.SpawnProtocol.Spawn
import akka.actor.typed.scaladsl.AskPattern.Askable
import com.typesafe.config.ConfigFactory
import csw.command.client.messages.CommandResponseManagerMessage
import csw.command.client.{CRMCacheProperties, CommandResponseManager, CommandResponseManagerActor}
import csw.location.api.extensions.ActorExtension.RichActor
import csw.location.api.scaladsl.LocationService
import csw.location.client.scaladsl.HttpLocationServiceFactory
import csw.location.model.scaladsl.Connection.AkkaConnection
import csw.location.model.scaladsl.{AkkaLocation, AkkaRegistration, ComponentId, ComponentType}
import esw.ocs.api.models.messages.SequencerMsg
import esw.ocs.api.models.messages.error.LoadScriptError
import esw.ocs.core._
import esw.ocs.dsl.utils.ScriptLoader
import esw.ocs.dsl.{CswServices, Script}
import esw.ocs.macros.StrandEc
import esw.ocs.syntax.FutureSyntax.FutureOps

import scala.util.control.NonFatal

private[ocs] class SequencerWiring(val sequencerId: String, val observingMode: String) {
  private lazy val config          = ConfigFactory.load()
  private lazy val sequencerConfig = SequencerConfig.from(config, sequencerId, observingMode)
  import sequencerConfig._
  private lazy val actorRuntime = new ActorRuntime(sequencerName)
  import actorRuntime._

  private lazy val engine      = new Engine()
  private lazy val componentId = ComponentId(sequencerName, ComponentType.Sequencer)

  private lazy val crmRef: ActorRef[CommandResponseManagerMessage] =
    (typedSystem ? Spawn(CommandResponseManagerActor.behavior(CRMCacheProperties(), loggerFactory), "crm")).block
  private lazy val commandResponseManager: CommandResponseManager = new CommandResponseManager(crmRef)

  private[esw] lazy val sequencerRef: ActorRef[SequencerMsg] =
    (typedSystem ? Spawn(SequencerBehavior.behavior(sequencer, script), sequencerName)).block

  //Pass lambda to break circular dependency shown below.
  //SequencerRef -> Script -> cswServices -> SequencerOperator -> SequencerRef
  private lazy val sequenceOperatorFactory = () => new SequenceOperator(sequencerRef)

  private lazy val cswServices    = new CswServices(sequenceOperatorFactory, commandResponseManager)
  private lazy val script: Script = ScriptLoader.load(scriptClass, cswServices)
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
    val registration = AkkaRegistration(AkkaConnection(componentId), prefix, sequencerRef.toURI)

    engine.start(sequenceOperatorFactory(), script)

    locationService
      .register(registration)
      .map(x => Right(x.location.asInstanceOf[AkkaLocation]))
      .recover {
        case NonFatal(e) => Left(LoadScriptError(e.getMessage))
      }
      .block
  }
}
