package agent

import java.nio.file.Paths

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import csw.prefix.models.Prefix
import RichProcessExt._
import csw.location.api.scaladsl.LocationService
import csw.location.models.{ComponentId, ComponentType}
import csw.location.models.Connection.{AkkaConnection, HttpConnection}

import scala.concurrent.duration.DurationInt
import scala.util.{Failure, Success}
import scala.util.control.NonFatal

sealed trait AgentCommand {
  val strings: List[String]
  val prefix: Prefix
}

sealed trait Response
case object Started           extends Response
case class Error(msg: String) extends Response

case class SpawnSequenceComponent(replyTo: ActorRef[Response], prefix: Prefix) extends AgentCommand {
  private val executablePath: String = Paths.get("target/universal/stage/bin/esw-ocs-app").toAbsolutePath.toString
  override val strings               = List(executablePath, "seqcomp", "-s", prefix.subsystem.toString, "-n", prefix.componentName)
}

object SpawnSequenceComponent {
  def apply(prefix: Prefix)(replyTo: ActorRef[Response]): SpawnSequenceComponent = new SpawnSequenceComponent(replyTo, prefix)
}

//todo: imp: log everything
//todo: consider killing the process if it does not register in given time

class AgentActor(locationService: LocationService) {

  def behavior: Behavior[AgentCommand] = Behaviors.receive { (ctx, command) =>
    import ctx.system
    import ctx.executionContext

    runCommand(command)(system)
    command match {
      case SpawnSequenceComponent(replyTo, prefix) =>
        val akkaLocF = locationService.resolve(AkkaConnection(ComponentId(prefix, ComponentType.SequenceComponent)), 10.seconds)
        akkaLocF.onComplete {
          case Success(Some(_)) => replyTo ! Started
          case Success(None)    => replyTo ! Error("could not get a response from spawned process")
          case Failure(_)       => replyTo ! Error("error while waiting for seq comp to get registered")
        }
    }
    Behaviors.same
  }

  private def runCommand(agentCommand: AgentCommand)(implicit system: ActorSystem[_]): Unit = {
    try {
      val processBuilder = new ProcessBuilder(agentCommand.strings: _*)
      val process        = processBuilder.start()
      println("PID=" + process.pid())
      process.attachToConsole(agentCommand.prefix)
    }
    catch {
      case NonFatal(err) => err.printStackTrace()
    }
  }
}
