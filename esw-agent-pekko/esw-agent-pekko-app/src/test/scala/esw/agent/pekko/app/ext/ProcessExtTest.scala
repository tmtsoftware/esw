package esw.agent.pekko.app.ext

import org.apache.pekko.actor.typed.{ActorSystem, SpawnProtocol}
import esw.agent.pekko.app.ext.ProcessExt.ProcessOps
import esw.testcommons.BaseTestSuite
import org.mockito.Mockito.{times, verify, when}
import org.mockito.verification.VerificationMode
import org.scalatest.TryValues.*

import java.util.concurrent.CompletableFuture
import java.util.stream
import scala.concurrent.duration.*
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
      val process = mock[ProcessHandle]
      var isAlive = true

      when(process.onExit()).thenReturn(CompletableFuture.completedFuture(process))

      process.onComplete(t => isAlive = t.success.value.isAlive)
      eventually(isAlive should ===(false))
    }
  }

  "kill" must {

    "destroy parent process along with all the children | ESW-325" in {
      val mockedProcesses = new MockedProcesses
      import mockedProcesses.*

      when(parent.descendants()).thenReturn(childrenStream)

      mockOnExitSuccess(parent, child1, child2)
      mockDestroy(parent, child1, child2)

      parent.kill(5.seconds).futureValue should ===(parent)
      verifyDestroy(List(child1, child2, parent))
      verifyDestroyForcibly(List(child1, child2, parent), times(0))
    }

    "destroy parent process forcibly along with all the children when graceful termination fails | ESW-325" in {
      val mockedProcesses = new MockedProcesses
      import mockedProcesses.*

      when(parent.descendants()).thenReturn(childrenStream)

      mockOnExit((parent, delayedFuture(parent, 500.millis).asJava.toCompletableFuture))
      mockOnExitSuccess(child1, child2)
      mockDestroy(parent, child1, child2)

      parent.kill(100.milli).futureValue should ===(parent)
      verifyDestroy(List(child1, child2, parent))

      // graceful termination succeeds for child1 and child2
      verifyDestroyForcibly(List(child1, child2), times(0))

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
    val parent: ProcessHandle                        = mock[ProcessHandle]
    val child1: ProcessHandle                        = mock[ProcessHandle]
    val child2: ProcessHandle                        = mock[ProcessHandle]
    val childrenStream: stream.Stream[ProcessHandle] = java.util.stream.Stream.of(child1, child2)
  }
}
