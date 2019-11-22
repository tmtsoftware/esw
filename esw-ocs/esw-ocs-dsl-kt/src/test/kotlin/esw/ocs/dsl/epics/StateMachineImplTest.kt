package esw.ocs.dsl.epics

import io.kotlintest.eventually
import io.kotlintest.milliseconds
import io.kotlintest.shouldBe
import io.kotlintest.shouldThrow
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test


class StateMachineImplTest {

    val job = SupervisorJob()

    private val exceptionHandler = CoroutineExceptionHandler { _, exception ->
        println("Exception thrown in script with a message: ${exception.message}, invoking exception handler " + exception)
    }

    val coroutineScope = CoroutineScope(job + exceptionHandler)
    val init = "INIT"
    val inProgress = "INPROGRESS"
    val invalid = "INVALIDSTATE"
    val testMachineName = "test-state-machine"

    var initFlag = false
    var inProgressFlag = false

    // instantiating to deal with nullable
    var stateMachine = StateMachineImpl(testMachineName, invalid, coroutineScope)

    @BeforeEach
    fun beforeEach() {
        stateMachine = StateMachineImpl(testMachineName, init, coroutineScope)
        stateMachine.state(init) { initFlag = true }

        initFlag = false
        inProgressFlag = false
    }

    @Test
    fun `start should start the fsm and evaluate the initial state | ESW-142`() {
        stateMachine.start()
        eventually(500.milliseconds) { initFlag shouldBe true }
    }

    @Test
    fun `start should throw exception if invalid initial state is given | ESW-142`() {
        val invalidStateMachine = StateMachineImpl(testMachineName, invalid, coroutineScope)
        shouldThrow<InvalidStateException> { invalidStateMachine.start() }
    }

    @Test
    fun `become should transition state to given state and evaluate it | ESW-142`() = runBlocking {
        stateMachine.state(inProgress) { inProgressFlag = true }

        stateMachine.start()
        eventually(500.milliseconds) { initFlag shouldBe true }

        stateMachine.become(inProgress)
        eventually(500.milliseconds) { inProgressFlag shouldBe true }
    }

    @Test
    fun `become should throw exception if invalid state is given | ESW-142`() {
        shouldThrow<InvalidStateException> {
            stateMachine.become("INVALIDSTATE")
        }
    }

    @Test
    fun `become should treat stateNames case insensitively | ESW-142`() {
        stateMachine.become(init.toLowerCase())
        eventually(500.milliseconds) { initFlag shouldBe true }
    }

    @Test
    fun `state should add the given lambda against the state | ESW-142`() {
        stateMachine.start()
        eventually(500.milliseconds) { initFlag shouldBe true }
    }
//
//    @Test
//    fun `refresh should evaluate fsm with its current state | ESW-142`() {
//
//    }
//
//    @Test
//    fun `on should execute given lambda if given condition is true | ESW-142`() {
//
//    }
//
//    @Test
//    fun `after should execute given lambda after specified time | ESW-142`() {
//
//    }
//
//    @Test
//    fun `entry should call the given lambda only if state transition happens from other state | ESW-142`() {
//
//    }
//
//    @Test
//    fun `entry should not call the given lambda is state transition happens in same state | ESW-142`() {
//
//    }
//
//    @Test
//    fun `await should wait for completion of fsm | ESW-142`() {
//
//    }
//
//    @Test
//    fun `completeFsm should complete fsm and cancel next operations | ESW-142`() {
//
//    }

    // IMP - Add tests about exceptions thrown in Fsm state handler
}