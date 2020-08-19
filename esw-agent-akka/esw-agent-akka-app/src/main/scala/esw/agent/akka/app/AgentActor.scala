package esw.agent.akka.app

import akka.actor.typed.scaladsl.Behaviors
import esw.agent.akka.app.process.ProcessManager
import esw.agent.akka.client.AgentCommand
import esw.agent.akka.client.AgentCommand._
import esw.agent.service.api.models._
import esw.commons.extensions.FutureEitherExt.FutureEitherOps

class AgentActor(processManager: ProcessManager) {

  private[agent] def behavior: Behaviors.Receive[AgentCommand] =
    Behaviors.receive[AgentCommand] { (ctx, command) =>
      import ctx.executionContext
      command match {
        case cmd: SpawnCommand                => processManager.spawn(cmd).mapToAdt(_ => Spawned, Failed).map(cmd.replyTo ! _)
        case KillComponent(replyTo, location) => processManager.kill(location).map(replyTo ! _)
      }
      Behaviors.same
    }
}
