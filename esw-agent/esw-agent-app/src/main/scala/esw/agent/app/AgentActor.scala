package esw.agent.app

import esw.agent.api.AgentCommand.SpawnCommand.SpawnSequenceComponent
import esw.agent.api.AgentCommand.{KillAllProcesses, ProcessRegistered, ProcessRegistrationFailed}
import esw.agent.api.Response.{Failed, Spawned}
import AgentActor.AgentState
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import csw.location.api.scaladsl.LocationService
import csw.location.models.ComponentId
import csw.location.models.ComponentType.SequenceComponent
import csw.location.models.Connection.AkkaConnection
import csw.logging.api.scaladsl.Logger
import esw.agent.api.AgentCommand
import esw.agent.app.utils.ProcessExecutor

import scala.util.Success

class AgentActor(
    locationService: LocationService,
    processExecutor: ProcessExecutor,
    agentSettings: AgentSettings,
    logger: Logger
) {

  import logger._

  def behavior(state: AgentState): Behavior[AgentCommand] = Behaviors.receive { (ctx, command) =>
    command match {
      case command @ SpawnSequenceComponent(replyTo, prefix) =>
        debug(s"spawning sequence component", map = Map("prefix" -> prefix))
        processExecutor.runCommand(command) match {
          case Left(err) => replyTo ! err
          case Right(pid) =>
            val akkaLocF = locationService.resolve(
              AkkaConnection(ComponentId(prefix, SequenceComponent)),
              agentSettings.durationToWaitForComponentRegistration
            )
            ctx.pipeToSelf(akkaLocF) {
              case Success(Some(_)) => ProcessRegistered(pid, replyTo)
              case _                => ProcessRegistrationFailed(pid, replyTo)
            }

        }
        Behaviors.same

      case ProcessRegistered(pid, replyTo) =>
        debug("spawned process is registered with location service", Map("pid" -> pid))
        replyTo ! Spawned
        behavior(state.finishRegistration(pid))

      case ProcessRegistrationFailed(pid, replyTo) =>
        error(
          "could not get registration confirmation from spawned process within given time. killing the process",
          Map("pid" -> pid)
        )
        replyTo ! Failed("could not get registration confirmation from spawned process within given time")
        processExecutor.killProcess(pid)
        behavior(state.failRegistration(pid))

      case KillAllProcesses =>
        debug("killing all processes")
        (state.registeredProcesses ++ state.registeringProcesses).foreach(processExecutor.killProcess)
        behavior(AgentState.empty)
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
