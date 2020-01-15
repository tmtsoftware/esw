package esw.agent.app

import akka.actor.typed.ActorRef
import akka.actor.typed.scaladsl.Behaviors
import csw.location.api.scaladsl.LocationService
import csw.location.models.ComponentId
import csw.logging.api.scaladsl.Logger
import esw.agent.api.AgentCommand._
import esw.agent.api.Response.Failed
import esw.agent.api.{AgentCommand, Response}
import esw.agent.app.AgentActor.AgentState
import esw.agent.app.ProcessActor.{ProcessActorMessage, SpawnComponent}
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
      //already spawning or registered
      case SpawnCommand(replyTo, componentId) if state.components.contains(componentId) =>
        val message = "given component is already in process"
        warn(message, Map("prefix" -> componentId.prefix))
        replyTo ! Response.Failed(message)
        Behaviors.same
      //happy path
      case command: SpawnCommand =>
        val processActor    = new ProcessActor(locationService, processExecutor, agentSettings, logger, command)
        val processActorRef = ctx.spawn(processActor.init, command.componentId.prefix.toString.toLowerCase)
        ctx.watchWith(processActorRef, Finished(command.componentId))
        processActorRef ! SpawnComponent
        behavior(state.add(command.componentId, processActorRef))
      case KillComponent(replyTo, componentId) =>
        state.components.get(componentId) match {
          case Some(processActor) =>
            processActor ! ProcessActor.Die(replyTo)
          case None =>
            val message = "given component id is not running on this agent"
            error(message, Map("prefix" -> componentId.prefix))
            replyTo ! Failed(message)
        }
        Behaviors.same
      //process has exited and child actor died
      case Finished(componentId) => behavior(state.remove(componentId))
    }
  }
}

object AgentActor {

  case class AgentState(components: Map[ComponentId, ActorRef[ProcessActorMessage]]) {
    def add(componentId: ComponentId, actorRef: ActorRef[ProcessActorMessage]): AgentState =
      copy(components = components + (componentId -> actorRef))
    def remove(componentId: ComponentId): AgentState = copy(components = components - componentId)
  }

  object AgentState {
    val empty: AgentState = AgentState(Map.empty)
  }
}
