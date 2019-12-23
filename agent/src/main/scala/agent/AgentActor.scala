package agent

import agent.AgentCommand.SpawnSequenceComponent
import agent.Response.{Error, Started}
import agent.utils.ProcessOutput
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import csw.location.api.scaladsl.LocationService
import csw.location.models.Connection.AkkaConnection
import csw.location.models.{ComponentId, ComponentType}

import scala.concurrent.duration.DurationInt
import scala.util.control.NonFatal
import scala.util.{Failure, Success}

//todo: imp: log everything
//todo: consider killing the process if it does not register in given time
class AgentActor(locationService: LocationService, outChannel: ProcessOutput) {

  def behavior: Behavior[AgentCommand] = Behaviors.receive { (ctx, command) =>
    import ctx.executionContext

    runCommand(command, outChannel)
    command match {
      case SpawnSequenceComponent(replyTo, prefix) =>
        val akkaLocF = locationService.resolve(AkkaConnection(ComponentId(prefix, ComponentType.SequenceComponent)), 5.seconds)
        akkaLocF.onComplete {
          case Success(Some(_)) => replyTo ! Started
          case Success(None)    => replyTo ! Error("could not get a response from spawned process")
          case Failure(_)       => replyTo ! Error("error while waiting for seq comp to get registered")
        }
    }
    Behaviors.same
  }

  private def runCommand(agentCommand: AgentCommand, output: ProcessOutput): Unit = {
    try {
      val processBuilder = new ProcessBuilder(agentCommand.strings: _*)
      val process        = processBuilder.start()
      println("PID=" + process.pid())
      output.attachProcess(process, agentCommand.prefix)
    }
    catch {
      case NonFatal(err) => err.printStackTrace()
    }
  }
}
