package agent

import agent.AgentCommand.{KillAllProcesses, SpawnSequenceComponent}
import agent.Response.{Error, Done}
import agent.utils.ProcessOutput
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import csw.location.api.scaladsl.LocationService
import csw.location.models.Connection.AkkaConnection
import csw.location.models.{ComponentId, ComponentType}
import csw.logging.api.scaladsl.Logger

import scala.concurrent.duration.DurationInt
import scala.util.control.NonFatal
import scala.util.{Failure, Success}

//todo: imp: log everything
//todo: consider killing the process if it does not register in given time
//todo: spawned processes should run in background even if agent process dies
class AgentActor(locationService: LocationService, outChannel: ProcessOutput) {

  private val log: Logger            = AgentLogger.getLogger
  private var processIds: List[Long] = List.empty

  def behavior: Behavior[AgentCommand] = Behaviors.receive { (ctx, command) =>
    import ctx.executionContext
    log.info(s"Received command: $command")

    command match {
      case command @ SpawnSequenceComponent(replyTo, prefix) =>
        runCommand(command, outChannel)
        val akkaLocF = locationService.resolve(AkkaConnection(ComponentId(prefix, ComponentType.SequenceComponent)), 5.seconds)
        akkaLocF.onComplete {
          case Success(Some(_)) => replyTo ! Done
          case Success(None)    => replyTo ! Error("could not get a response from spawned process")
          case Failure(_)       => replyTo ! Error("error while waiting for seq comp to get registered")
        }

      case KillAllProcesses(replyTo) =>
        processIds.foreach(ProcessHandle.of(_).ifPresent(p => p.destroyForcibly()))
        replyTo ! Done
    }
    Behaviors.same
  }

  private def runCommand(agentCommand: ShellCommand, output: ProcessOutput): Unit = {
    try {
      val processBuilder = new ProcessBuilder(agentCommand.strings: _*)
      val process        = processBuilder.start()
      processIds ::= process.pid()
      log.info(s"[${process.pid()}] Executing command: ${processBuilder.command()}")
      output.attachProcess(process, agentCommand.prefix)
    }
    catch {
      case NonFatal(err) => err.printStackTrace()
    }
  }
}
