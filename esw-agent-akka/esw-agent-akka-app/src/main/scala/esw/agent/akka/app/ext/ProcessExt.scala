package esw.agent.akka.app.ext

import akka.actor.typed.ActorSystem
import esw.agent.akka.app.ext.FutureExt.FutureOps

import scala.compat.java8.StreamConverters.StreamHasToScala
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, Future}
import scala.jdk.FutureConverters.CompletionStageOps
import scala.util.Try

/**
 * This is a convenience extension on top of [[java.lang.ProcessHandle]] while working with [[java.lang.Process]].
 */
object ProcessExt {

  implicit class ProcessOps(private val parent: ProcessHandle) extends AnyVal {

    def onComplete[T](f: Try[ProcessHandle] => T)(implicit executor: ExecutionContext): Unit =
      parent.onExit().asScala.onComplete(f)

    def kill(terminationTimeout: FiniteDuration)(implicit system: ActorSystem[_]): Future[ProcessHandle] = {
      import system.executionContext
      def destroyF(p: ProcessHandle, f: ProcessHandle => Boolean) = { f(p); p.onExit().asScala }
      def destroy(p: ProcessHandle)         = destroyF(p, _.destroy())
      def destroyForcibly(p: ProcessHandle) = destroyF(p, _.destroyForcibly())

      def kill(p: ProcessHandle) =
        destroy(p).timeout(terminationTimeout).recoverWith(_ => destroyForcibly(p))

      // descendants can throw exception
      Future(parent.descendants().toScala[List]).flatMap { children =>
        Future
          .traverse(children)(kill)
          .transformWith(_ => kill(parent))
      }
    }
  }

}
