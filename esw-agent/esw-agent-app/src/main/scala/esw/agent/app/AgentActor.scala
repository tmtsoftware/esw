package esw.agent.app

import akka.actor.typed.ActorRef
import akka.actor.typed.scaladsl.Behaviors
import csw.location.api.scaladsl.LocationService
import csw.location.models.ComponentId
import csw.logging.api.scaladsl.Logger
import esw.agent.api.AgentCommand._
import esw.agent.api.{AgentCommand, Response}
import esw.agent.app.AgentActor.AgentState
import esw.agent.app.ProcessActor.{ProcessActorMessage, SpawnProcess}
import esw.agent.app.utils.ProcessExecutor

class AgentActor(
    locationService: LocationService,
    processExecutor: ProcessExecutor,
    agentSettings: AgentSettings,
    logger: Logger
) {

  import logger._

  def behavior(state: AgentState): Behaviors.Receive[AgentCommand] = Behaviors.receive[AgentCommand] { (ctx, command) =>
    command match {
      //already spawning
      case command: SpawnCommand if state.children.contains(command.componentId) =>
        val message = "spawning of component is already in progress"
        warn(message, Map("prefix" -> command.prefix))
        command.replyTo ! Response.Failed(message)
        Behaviors.same
      //happy path
      case command: SpawnCommand =>
        val childActor = new ProcessActor(
          locationService,
          processExecutor,
          agentSettings,
          logger,
          command.componentId,
          command.connectionType,
          command.replyTo,
          command.strings(agentSettings.binariesPath)
        )
        val child = ctx.spawn(childActor.init, command.prefix.value)
        ctx.watchWith(child, Finished(command))
        child ! SpawnProcess
        behavior(state.add(command.componentId, child))
      //work done by child actor and child actor died
      case Finished(spawnCommand) =>
        behavior(state.remove(spawnCommand.componentId))
    }
  }
}

object AgentActor {

  case class AgentState(children: Map[ComponentId, ActorRef[ProcessActorMessage]]) {
    def add(componentId: ComponentId, actorRef: ActorRef[ProcessActorMessage]): AgentState =
      copy(children = children + (componentId -> actorRef))
    def remove(componentId: ComponentId): AgentState = copy(children = children - componentId)
  }

  object AgentState {
    val empty: AgentState = AgentState(Map.empty)
  }
}
