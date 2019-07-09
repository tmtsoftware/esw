package esw.ocs.framework

import akka.actor.Scheduler
import akka.actor.typed.SpawnProtocol.Spawn
import akka.actor.typed.scaladsl.AskPattern.Askable
import akka.actor.typed.{ActorRef, ActorSystem, SpawnProtocol}
import akka.stream.Materializer
import akka.stream.typed.scaladsl.ActorMaterializer
import akka.util.Timeout
import com.typesafe.config.Config
import csw.command.client.messages.CommandResponseManagerMessage
import csw.command.client.{CRMCacheProperties, CommandResponseManager, CommandResponseManagerActor}
import csw.location.api.models.Connection.AkkaConnection
import csw.location.api.models.{AkkaLocation, AkkaRegistration, ComponentId, ComponentType}
import csw.location.api.scaladsl.LocationService
import csw.location.client.ActorSystemFactory
import csw.location.client.scaladsl.HttpLocationServiceFactory
import csw.logging.api.scaladsl.Logger
import csw.logging.client.scaladsl.LoggerFactory
import csw.params.core.models.Prefix
import esw.ocs.async.macros.StrandEc
import esw.ocs.framework.api.models.messages.SequencerMsg
import esw.ocs.framework.core.internal.ScriptLoader
import esw.ocs.framework.core.{SequenceEditorClient, Sequencer, SequencerBehavior}
import esw.ocs.framework.dsl.{CswServices, Script}

import scala.concurrent.duration.DurationLong
import scala.concurrent.{Await, ExecutionContext}

class SequencerWiring(val sequencerId: String, val observingMode: String) {

  implicit lazy val typedSystem: ActorSystem[SpawnProtocol] =
    ActorSystemFactory.remote(SpawnProtocol.behavior, "sequencer-system")
  implicit lazy val mat: Materializer    = ActorMaterializer()
  implicit lazy val ec: ExecutionContext = typedSystem.executionContext
  implicit lazy val scheduler: Scheduler = typedSystem.scheduler
  implicit lazy val timeout: Timeout     = 5.seconds

  private lazy val sequencerName  = s"$sequencerId@$observingMode"
  private lazy val componentId    = ComponentId(sequencerName, ComponentType.Sequencer)
  private lazy val config: Config = typedSystem.settings.config
  private lazy val prefix         = Prefix(config.getString(s"scripts.$sequencerId.$observingMode.prefix"))

  private lazy val loggerFactory = new LoggerFactory(sequencerName)
  private lazy val log: Logger   = loggerFactory.getLogger

  private lazy val crmRef: ActorRef[CommandResponseManagerMessage] =
    Await.result(typedSystem ? Spawn(CommandResponseManagerActor.behavior(CRMCacheProperties(), loggerFactory), "crm"), 5.seconds)
  private lazy val commandResponseManager: CommandResponseManager = new CommandResponseManager(crmRef)

  private lazy val cswServices    = new CswServices
  private lazy val script: Script = new ScriptLoader(sequencerId, observingMode).load(cswServices)
  private lazy val sequencer      = new Sequencer(commandResponseManager)(StrandEc(), timeout)

  private lazy val sequencerRef: ActorRef[SequencerMsg] =
    Await.result(typedSystem ? Spawn(SequencerBehavior.behavior(sequencer, script), "sequencer"), 5.seconds)
  private lazy val sequenceEditorClient = new SequenceEditorClient(sequencerRef)

  private lazy val locationService: LocationService = HttpLocationServiceFactory.makeLocalClient

  def shutDown(): Unit = {
    Await.result(locationService.unregister(AkkaConnection(componentId)), 5.seconds)
    Await.result(sequenceEditorClient.shutdown(), 5.seconds)
    typedSystem.terminate()
  }

  def start(): AkkaLocation = {
    val registration = AkkaRegistration(AkkaConnection(componentId), prefix, sequencerRef)
    log.info(s"Registering ${componentId.name} with Location Service using registration: [${registration.toString}]")

    Await
      .result(locationService.register(registration), 5.seconds)
      .location
      .asInstanceOf[AkkaLocation]
  }
}
