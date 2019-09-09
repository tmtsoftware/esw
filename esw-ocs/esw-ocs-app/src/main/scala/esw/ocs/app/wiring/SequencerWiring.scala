package esw.ocs.app.wiring

import akka.Done
import akka.actor.CoordinatedShutdown.UnknownReason
import akka.actor.typed.ActorRef
import akka.actor.typed.SpawnProtocol.Spawn
import akka.actor.typed.scaladsl.AskPattern.Askable
import com.typesafe.config.{Config, ConfigFactory}
import csw.framework.internal.wiring.ActorRuntime
import csw.location.api.extensions.ActorExtension.RichActor
import csw.location.models.Connection.AkkaConnection
import csw.location.models.{AkkaLocation, AkkaRegistration, ComponentId, ComponentType}
import csw.network.utils.SocketUtils
import esw.http.core.wiring.{HttpService, ServerWiring}
import esw.ocs.api.models.responses.RegistrationError
import esw.ocs.app.route.{PostHandlerImpl, SequencerAdminRoutes}
import esw.ocs.client.messages.SequencerMessages.{EswSequencerMessage, Shutdown}
import esw.ocs.core._
import esw.ocs.dsl.utils.ScriptLoader
import esw.ocs.dsl.{CswServices, ScriptDsl}
import esw.ocs.impl.SequencerAdminImpl
import esw.ocs.internal.{Timeouts, Wiring}
import esw.ocs.syntax.FutureSyntax.FutureOps

import scala.concurrent.{Await, Future}

private[ocs] class SequencerWiring(val sequencerId: String, val observingMode: String, sequenceComponentName: Option[String])
    extends Wiring {
  private lazy val config: Config       = ConfigFactory.load()
  private[esw] lazy val sequencerConfig = SequencerConfig.from(config, sequencerId, observingMode, sequenceComponentName)
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
  private lazy val script: ScriptDsl       = ScriptLoader.loadKotlinScript(scriptClass, cswServices)

  lazy val cswServices = new CswServices(
    sequenceOperatorFactory,
    commandResponseManager,
    typedSystem,
    locationService,
    eventService,
    timeServiceSchedulerFactory
  )

  private lazy val sequencerAdmin = new SequencerAdminImpl(sequencerRef)
  private lazy val postHandler    = new PostHandlerImpl(sequencerAdmin)
  private lazy val routes         = new SequencerAdminRoutes(postHandler)

  private lazy val serverWiring = new ServerWiring(Some(SocketUtils.getFreePort), Some(s"$sequencerName@http"))
  import serverWiring._

  private lazy val httpService =
    new HttpService(cswCtx.logger, locationService, routes.route, settings, serverWiring.actorRuntime)

  lazy val sequencerBehavior =
    new SequencerBehavior(componentId, script, locationService, commandResponseManager)(typedSystem, timeout)

  def shutDown(): Future[Done] = {
    httpService.shutdown(UnknownReason)
    (sequencerRef ? Shutdown).map(_ => Done)
  }

  def start(): Either[RegistrationError, AkkaLocation] = {
    new Engine().start(sequenceOperatorFactory(), script)

    Await.result(httpService.registeredLazyBinding, Timeouts.DefaultTimeout)

    val registration = AkkaRegistration(AkkaConnection(componentId), prefix, sequencerRef.toURI)
    Await.result(cswServices.register(registration)(typedSystem), Timeouts.DefaultTimeout)
  }
}
