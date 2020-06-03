package esw.agent.app.ext

import akka.actor.typed.ActorSystem
import esw.agent.app.ext.FutureExt.FutureOps

import scala.compat.java8.StreamConverters.StreamHasToScala
import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration.FiniteDuration
import scala.jdk.FutureConverters.CompletionStageOps
import scala.util.Try

object ProcessExt {

  implicit class ProcessOps(private val process: Process) extends AnyVal {

    def onComplete[T](f: Try[Process] => T)(implicit executor: ExecutionContext): Unit = process.onExit().asScala.onComplete(f)

    def kill(terminationTimeout: FiniteDuration)(implicit system: ActorSystem[_]): Future[ProcessHandle] = {
      import system.executionContext
      def destroyF(p: ProcessHandle, f: ProcessHandle => Boolean) = { f(p); p.onExit().asScala }
      def destroy(p: ProcessHandle)         = destroyF(p, _.destroy())
      def destroyForcibly(p: ProcessHandle) = destroyF(p, _.destroyForcibly())

      def kill(p: ProcessHandle) =
        destroy(p).timeout(terminationTimeout).recoverWith(_ => destroyForcibly(p))

      val parent   = process.toHandle
      val children = process.descendants().toScala[List]

      Future
        .traverse(children)(kill)
        .transformWith(_ => kill(parent))
    }
  }

}
