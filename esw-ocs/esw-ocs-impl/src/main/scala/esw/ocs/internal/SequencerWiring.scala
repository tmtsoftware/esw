package esw.ocs.internal

import akka.Done
import akka.actor.typed.ActorRef
import akka.actor.typed.SpawnProtocol.Spawn
import akka.actor.typed.scaladsl.AskPattern.Askable
import com.typesafe.config.{Config, ConfigFactory}
import csw.framework.internal.wiring.ActorRuntime
import csw.location.api.extensions.ActorExtension.RichActor
import csw.location.models.Connection.AkkaConnection
import csw.location.models.{AkkaLocation, AkkaRegistration, ComponentId, ComponentType}
import esw.ocs.api.models.responses.RegistrationError
import esw.ocs.client.SequencerAdminClient
import esw.ocs.client.messages.SequencerMessages.{EswSequencerMessage, Shutdown}
import esw.ocs.core._
import esw.ocs.dsl.utils.ScriptLoader
import esw.ocs.dsl.{CswServices, Script}
import esw.ocs.syntax.FutureSyntax.FutureOps

import scala.concurrent.Future
// $COVERAGE-OFF$
private[ocs] class SequencerWiring(val sequencerId: String, val observingMode: String, sequenceComponentName: Option[String]) {
  private lazy val config: Config  = ConfigFactory.load()
  private lazy val sequencerConfig = SequencerConfig.from(config, sequencerId, observingMode, sequenceComponentName)
  import sequencerConfig._

  lazy val cswServicesWiring = new CswServicesWiring(sequencerName)
  import cswServicesWiring._
  import frameworkWiring._
  import frameworkWiring.actorRuntime._
  implicit lazy val actorRuntime: ActorRuntime = frameworkWiring.actorRuntime

  lazy val sequencerRef: ActorRef[EswSequencerMessage] = (typedSystem ? Spawn(sequencerBehavior.setup, sequencerName)).block

  //Pass lambda to break circular dependency shown below.
  //SequencerRef -> Script -> cswServices -> SequencerOperator -> SequencerRef
  private lazy val sequenceOperatorFactory = () => new SequenceOperator(sequencerRef)
  private lazy val componentId             = ComponentId(sequencerName, ComponentType.Sequencer)
  private lazy val script: Script          = ScriptLoader.load(scriptClass, cswServices)

  lazy val cswServices = new CswServices(
    sequenceOperatorFactory,
    commandResponseManager,
    sequencerCommandService,
    locationServiceDsl,
    eventServiceDsl,
    timeServiceDsl
  )

  lazy val sequencerBehavior =
    new SequencerBehavior(componentId, script, locationService, commandResponseManager)(typedSystem, timeout)
  lazy val sequencerAdmin = new SequencerAdminClient(sequencerRef)(typedSystem, timeout)

  def shutDown(): Future[Done] = (sequencerRef ? Shutdown).map(_ => Done)

  def start(): Either[RegistrationError, AkkaLocation] = {
    new Engine().start(sequenceOperatorFactory(), script)

    val registration = AkkaRegistration(AkkaConnection(componentId), prefix, sequencerRef.toURI)
    locationServiceDsl.register(registration)(typedSystem).block
  }
}
// $COVERAGE-ON$
