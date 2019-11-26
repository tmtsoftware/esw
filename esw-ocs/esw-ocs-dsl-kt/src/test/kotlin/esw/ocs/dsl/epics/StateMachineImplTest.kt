package esw.ocs.dsl.epics

import esw.ocs.dsl.script.StrandEc
import io.kotlintest.eventually
import io.kotlintest.shouldBe
import io.kotlintest.shouldThrow
import kotlinx.coroutines.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.time.milliseconds
import io.kotlintest.milliseconds as jMilliseconds


class StateMachineImplTest {

    // These are needed to simulate script like environment
    val job = SupervisorJob()
    private val _strandEc = StrandEc.apply()
    private val dispatcher = _strandEc.executorService().asCoroutineDispatcher()
    private val exceptionHandler = CoroutineExceptionHandler { _, exception ->
        println("Exception thrown in script with a message: ${exception.message}, invoking exception handler " + exception)
    }
    val coroutineScope = CoroutineScope(job + exceptionHandler + dispatcher)

    val init = "INIT"
    val inProgress = "INPROGRESS"
    val invalid = "INVALIDSTATE"
    val testMachineName = "test-state-machine"
    val timeout = 100.jMilliseconds

    var initFlag = false
    // instantiating to not to deal with nullable
    var stateMachine = StateMachineImpl(testMachineName, invalid, coroutineScope)

    @BeforeEach
    fun beforeEach() {
        stateMachine = StateMachineImpl(testMachineName, init, coroutineScope)
        stateMachine.state(init) { initFlag = true }

        initFlag = false
    }

    private fun checkInitFlag() {
        eventually(timeout) { initFlag shouldBe true }
    }

    @Test
    fun `start should start the fsm and evaluate the initial state | ESW-142`() {
        stateMachine.start()
        checkInitFlag()
    }

    @Test
    fun `start should throw exception if invalid initial state is given | ESW-142`() {
        val invalidStateMachine = StateMachineImpl(testMachineName, invalid, coroutineScope)
        shouldThrow<InvalidStateException> { invalidStateMachine.start() }
    }

    @Test
    fun `become should transition state to given state and evaluate it | ESW-142`() = runBlocking {
        var inProgressFlag = false
        stateMachine.state(inProgress) { inProgressFlag = true }

        stateMachine.start()
        checkInitFlag()

        stateMachine.become(inProgress)
        eventually(timeout) { inProgressFlag shouldBe true }
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
        checkInitFlag()
    }

    @Test
    fun `state should add the given lambda against the state | ESW-142`() {
        stateMachine.start()
        checkInitFlag()
    }

    @Test
    fun `refresh should evaluate fsm with its current state | ESW-142`() {
        var firstCalled = false
        var refreshFlag = false

        stateMachine.state(inProgress) {
            if (firstCalled) refreshFlag = true
            else firstCalled = true
        }

        stateMachine.start()
        checkInitFlag()

        stateMachine.become(inProgress)
        eventually(timeout) { firstCalled shouldBe true }
        eventually(timeout) { refreshFlag shouldBe false }

        stateMachine.refresh()
        eventually(timeout) { refreshFlag shouldBe true }
    }

    @Test
    fun `on should execute the given lambda if given condition is true | ESW-142`() = runBlocking {
        var flag = false
        stateMachine.on(true) {
            flag = true
        }

        flag shouldBe true
    }

    @Test
    fun `on should not execute the given lambda if given condition is false | ESW-142`() = runBlocking {
        var flag = false
        stateMachine.on(false) {
            flag = true
        }

        flag shouldBe false
    }

    @Test
    fun `after should execute given lambda after specified time | ESW-142`() = runBlocking {
        var flag = false
        coroutineScope.launch {
            stateMachine.after(100.milliseconds) {
                flag = true
            }
        }

        flag shouldBe false
        delay(100)
        eventually(30.jMilliseconds) { flag shouldBe true }
    }

    @Test
    fun `entry should call the given lambda only if state transition happens from other state | ESW-142`() = runBlocking {
        var entryCalled = false

        stateMachine.start()
        checkInitFlag()

        coroutineScope.launch {
            // current state is INIT and previous state is null.
            stateMachine.entry {
                entryCalled = true
            }
            entryCalled shouldBe true

        }.join()
    }

    @Test
    fun `entry should not call the given lambda if state transition happens in same state | ESW-142`() = runBlocking {
        var entryCalled = false
        stateMachine.start()
        stateMachine.become(init)
        checkInitFlag()

        coroutineScope.launch {
            // current and previous state both are INIT
            stateMachine.entry {
                entryCalled = true
            }

            entryCalled shouldBe false
        }.join()
    }

    @Test
    fun `completeFsm should complete fsm and cancel next operations | ESW-142`() = runBlocking {
        var shouldChange = false
        var shouldNotChange = false

        stateMachine.state(inProgress) {
            shouldChange = true
            stateMachine.completeFSM()
            shouldNotChange = true
        }

        stateMachine.start()
        checkInitFlag()
        stateMachine.become(inProgress)

        coroutineScope.launch {
            eventually(timeout) { shouldChange shouldBe true }
            eventually(timeout) { shouldNotChange shouldBe false }

            stateMachine.await()
            eventually(timeout) { shouldNotChange shouldBe false }
        }.join()
    }

    @Test
    fun `await should wait for completion of fsm | ESW-142`() = runBlocking {
        var waitingStarted = false
        var waitingFinished = false

        coroutineScope.launch {
            waitingStarted = true
            stateMachine.await()
            waitingFinished = true
        }

        eventually(timeout) { waitingStarted shouldBe true }
        eventually(timeout) { waitingFinished shouldBe false }

        stateMachine.completeFSM()

        eventually(timeout) { waitingFinished shouldBe true }
    }
}