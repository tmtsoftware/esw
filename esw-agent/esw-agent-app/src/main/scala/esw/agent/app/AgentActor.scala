package esw.agent.app

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import csw.location.api.scaladsl.LocationService
import csw.location.models.ComponentType.SequenceComponent
import csw.location.models.Connection.AkkaConnection
import csw.location.models.{ComponentId, Location, TypedConnection}
import csw.logging.api.scaladsl.Logger
import esw.agent.api.AgentCommand
import esw.agent.api.AgentCommand.SpawnCommand.SpawnSequenceComponent
import esw.agent.api.AgentCommand.{ProcessRegistered, ProcessRegistrationFailed}
import esw.agent.api.Response.{Failed, Spawned}
import esw.agent.app.AgentActor.AgentState
import esw.agent.app.utils.ProcessExecutor

import scala.concurrent.duration.{DurationLong, FiniteDuration}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

class AgentActor(
    locationService: LocationService,
    processExecutor: ProcessExecutor,
    agentSettings: AgentSettings,
    logger: Logger
) {

  import logger._

  private def isRegistered[T <: Location](
      typedConnection: TypedConnection[T],
      timeout: FiniteDuration = agentSettings.durationToWaitForComponentRegistration
  )(implicit executionContext: ExecutionContext): Future[Boolean] =
    locationService.resolve(typedConnection, timeout).map(_.nonEmpty)

  def behavior(state: AgentState): Behavior[AgentCommand] = Behaviors.receive { (ctx, command) =>
    command match {
      case command @ SpawnSequenceComponent(replyTo, prefix) =>
        import ctx.executionContext
        val connection = AkkaConnection(ComponentId(prefix, SequenceComponent))
        isRegistered(connection, 1.second)
          .onComplete {
            //registration already found. maybe component is already running on another machine
            case Success(true) =>
              val errorMessage = "can not spawn component when it is already registered"
              warn(errorMessage, Map("prefix" -> prefix))
              replyTo ! Failed(errorMessage)
            //registration not found. ready to launch component
            case Success(false) =>
              debug(s"spawning sequence component", map = Map("prefix" -> prefix))
              processExecutor.runCommand(command) match {
                case Left(err) => replyTo ! err
                case Right(pid) =>
                  ctx.pipeToSelf(isRegistered(connection)) {
                    case Success(true) => ProcessRegistered(pid, replyTo)
                    case _             => ProcessRegistrationFailed(pid, replyTo)
                  }

              }
            case Failure(exception) =>
              val errorMessage = "error occurred while resolving a component with location service"
              error(errorMessage, Map("prefix" -> prefix), exception)
              replyTo ! Failed(errorMessage)
          }
        Behaviors.same

      case ProcessRegistered(pid, replyTo) =>
        debug("spawned process is registered with location service", Map("pid" -> pid))
        replyTo ! Spawned
        behavior(state.finishRegistration(pid))

      case ProcessRegistrationFailed(pid, replyTo) =>
        val errorMessage = "could not get registration confirmation from spawned process within given time"
        error(errorMessage, Map("pid" -> pid))
        replyTo ! Failed(errorMessage)
        processExecutor.killProcess(pid)
        behavior(state.failRegistration(pid))
    }
  }
}

object AgentActor {

  case class AgentState(registeredProcesses: Set[Long], registeringProcesses: Set[Long]) {
    def withNewProcess(pid: Long): AgentState = copy(registeringProcesses = registeringProcesses + pid)

    def finishRegistration(pid: Long): AgentState = copy(
      registeredProcesses = registeringProcesses + pid,
      registeringProcesses = registeringProcesses - pid
    )

    def failRegistration(pid: Long): AgentState = copy(
      registeringProcesses = registeringProcesses - pid
    )
  }

  object AgentState {
    val empty: AgentState = AgentState(Set.empty, Set.empty)
  }
}
