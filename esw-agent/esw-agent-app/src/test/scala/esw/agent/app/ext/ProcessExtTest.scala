package esw.agent.app.ext

import java.util.concurrent.CompletableFuture
import java.util.stream

import akka.actor.typed.{ActorSystem, SpawnProtocol}
import esw.agent.app.ext.ProcessExt.ProcessOps
import esw.testcommons.BaseTestSuite
import org.mockito.verification.VerificationMode
import org.scalatest.TryValues._

import scala.concurrent.duration._
import scala.concurrent.{Future, Promise}
import scala.jdk.FutureConverters.FutureOps

class ProcessExtTest extends BaseTestSuite {
  implicit private lazy val system: ActorSystem[SpawnProtocol.Command] = ActorSystem(SpawnProtocol(), "future-ext")
  import system.executionContext

  override protected def afterAll(): Unit = {
    system.terminate()
    system.whenTerminated.futureValue
  }

  override implicit def patienceConfig: PatienceConfig = PatienceConfig(5.seconds)

  "onComplete" must {
    "execute callback when process exits | ESW-325" in {
      val process       = mock[Process]
      var exitCode: Int = -1

      when(process.onExit()).thenReturn(CompletableFuture.completedFuture(process))
      when(process.exitValue()).thenReturn(0)

      process.onComplete(t => exitCode = t.success.value.exitValue())
      eventually(exitCode should ===(0))
    }
  }

  "kill" must {

    "destroy parent process along with all the children | ESW-325" in {
      val mockedProcesses = new MockedProcesses
      import mockedProcesses._

      when(process.toHandle).thenReturn(parent)
      when(process.descendants()).thenReturn(childrenStream)

      mockOnExitSuccess(parent, child1, child2)
      mockDestroy(parent, child1, child2)

      process.kill(5.seconds).futureValue should ===(parent)
      verifyDestroy(List(child1, child2, parent))
      verifyDestroyForcibly(List(child1, child2, parent), never)
    }

    "destroy parent process forcibly along with all the children when graceful termination fails | ESW-325" in {
      val mockedProcesses = new MockedProcesses
      import mockedProcesses._

      when(process.toHandle).thenReturn(parent)
      when(process.descendants()).thenReturn(childrenStream)

      mockOnExit((parent, delayedFuture(parent, 500.millis).asJava.toCompletableFuture))
      mockOnExitSuccess(child1, child2)
      mockDestroy(parent, child1, child2)

      process.kill(100.milli).futureValue should ===(parent)
      verifyDestroy(List(child1, child2, parent))

      // graceful termination succeeds for child1 and child2
      verifyDestroyForcibly(List(child1, child2), never)

      // graceful termination fails for parent, as it does not get terminated within the timeout
      verifyDestroyForcibly(List(parent))
    }
  }

  private def mockDestroy(handles: ProcessHandle*): Unit =
    handles.foreach(p => when(p.destroy()).thenReturn(true))

  private def mockOnExit(handles: (ProcessHandle, CompletableFuture[ProcessHandle])*): Unit =
    handles.foreach { case (process, onExit) => when(process.onExit()).thenReturn(onExit) }

  private def mockOnExitSuccess(handles: ProcessHandle*): Unit =
    mockOnExit(handles.map(p => (p, CompletableFuture.completedFuture(p))): _*)

  private def verifyDestroy(handles: List[ProcessHandle], mode: VerificationMode = times(1)): Unit =
    handles.foreach(verify(_, mode).destroy())

  private def verifyDestroyForcibly(handles: List[ProcessHandle], mode: VerificationMode = times(1)): Unit =
    handles.foreach(verify(_, mode).destroyForcibly())

  private def delayedFuture[T](value: T, delay: FiniteDuration): Future[T] = {
    val promise = Promise[T]()
    system.scheduler.scheduleOnce(delay, () => promise.success(value))(system.executionContext)
    promise.future
  }

  class MockedProcesses {
    val process: Process                             = mock[Process]
    val parent: ProcessHandle                        = mock[ProcessHandle]
    val child1: ProcessHandle                        = mock[ProcessHandle]
    val child2: ProcessHandle                        = mock[ProcessHandle]
    val childrenStream: stream.Stream[ProcessHandle] = java.util.stream.Stream.of(child1, child2)
  }
}
