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
import csw.logging.api.scaladsl.Logger
import csw.network.utils.SocketUtils
import esw.http.core.commons.ServiceLogger
import esw.http.core.wiring
import esw.http.core.wiring.{HttpService, Settings}
import esw.ocs.api.responses.RegistrationError
import esw.ocs.app.route.{PostHandlerImpl, SequencerAdminRoutes}
import esw.ocs.impl.SequencerAdminImpl
import esw.ocs.impl.core._
import esw.ocs.impl.dsl.utils.ScriptLoader
import esw.ocs.impl.dsl.{CswServices, Script}
import esw.ocs.impl.internal.{SequencerServer, Timeouts}
import esw.ocs.impl.messages.SequencerMessages.{EswSequencerMessage, Shutdown}
import esw.ocs.impl.syntax.FutureSyntax.FutureOps

import scala.concurrent.{Await, Future}

private[ocs] class SequencerWiring(val sequencerId: String, val observingMode: String, sequenceComponentName: Option[String]) {
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
  private lazy val script: Script          = ScriptLoader.load(scriptClass, cswServices)

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

  private lazy val settings       = new Settings(Some(SocketUtils.getFreePort), Some(s"$sequencerName@http"), config)
  private lazy val logger: Logger = new ServiceLogger(settings.httpConnection).getLogger
  private lazy val httpService =
    new HttpService(logger, locationService, routes.route, settings, new wiring.ActorRuntime(typedSystem))

  lazy val sequencerBehavior =
    new SequencerBehavior(componentId, script, locationService, commandResponseManager)(typedSystem, timeout)

  lazy val sequencerServer: SequencerServer = new SequencerServer {
    override def start(): Either[RegistrationError, AkkaLocation] = {
      new Engine().start(sequenceOperatorFactory(), script)

      Await.result(httpService.registeredLazyBinding, Timeouts.DefaultTimeout)

      val registration = AkkaRegistration(AkkaConnection(componentId), prefix, sequencerRef.toURI)
      Await.result(cswServices.register(registration)(typedSystem), Timeouts.DefaultTimeout)
    }

    override def shutDown(): Future[Done] = {
      httpService.shutdown(UnknownReason)
      (sequencerRef ? Shutdown).map(_ => Done)
    }
  }

}
