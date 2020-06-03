package esw.agent.app.process

import akka.actor.typed.ActorSystem
import esw.agent.app.FutureExt.FutureOps

import scala.compat.java8.StreamConverters.StreamHasToScala
import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration
import scala.jdk.FutureConverters.CompletionStageOps

object ProcessUtils {
  def isInstalled(cmd: String): Boolean = new ProcessBuilder("command", "-v", cmd).start().waitFor() == 0

  private def destroyF(p: ProcessHandle, f: ProcessHandle => Boolean) = { f(p); p.onExit().asScala }
  private def destroy(p: ProcessHandle)         = destroyF(p, _.destroy())
  private def destroyForcibly(p: ProcessHandle) = destroyF(p, _.destroyForcibly())

  def kill(process: Process, terminationTimeout: FiniteDuration)(implicit system: ActorSystem[_]): Future[ProcessHandle] = {
    import system.executionContext

    def kill(p: ProcessHandle) =
      destroy(p).timeout(terminationTimeout).recoverWith(_ => destroyForcibly(p))

    val parent   = process.toHandle
    val children = process.descendants().toScala[List]

    Future
      .traverse(children)(kill)
      .transformWith(_ => kill(parent))
  }
}
