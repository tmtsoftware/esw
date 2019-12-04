package esw.ocs.dsl.epics

import csw.params.core.generics.Parameter
import csw.params.core.models.Prefix
import csw.params.events.EventName
import csw.params.events.SystemEvent
import csw.params.javadsl.JKeyType
import csw.params.javadsl.JSubsystem
import esw.ocs.dsl.highlevel.CswHighLevelDslApi
import esw.ocs.dsl.params.Params
import esw.ocs.dsl.params.set
import esw.ocs.dsl.script.StrandEc
import io.kotlintest.eventually
import io.kotlintest.shouldBe
import io.kotlintest.shouldThrow
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.time.milliseconds
import io.kotlintest.milliseconds as jMilliseconds

class StateMachineImplTest {

    // These are needed to simulate script like environment
    private val job = SupervisorJob()
    private val _strandEc = StrandEc.apply()
    private val dispatcher = _strandEc.executorService().asCoroutineDispatcher()
    private val exceptionHandler = CoroutineExceptionHandler { _, exception ->
        println("Exception thrown in script with a message: ${exception.message}, invoking exception handler " + exception)
    }
    private val coroutineScope = CoroutineScope(job + exceptionHandler + dispatcher)
    val cswHighLevelDslApi: CswHighLevelDslApi = mockk()

    private val init = "INIT"
    private val inProgress = "INPROGRESS"
    private val invalid = "INVALIDSTATE"
    private val testMachineName = "test-state-machine"
    private val timeout = 100.jMilliseconds

    private var initFlag = false
    private var parameterSet = Params(setOf())
    // instantiating to not to deal with nullable
    private var stateMachine = StateMachineImpl(testMachineName, invalid, coroutineScope, cswHighLevelDslApi)

    @BeforeEach
    fun beforeEach() {
        stateMachine = StateMachineImpl(testMachineName, init, coroutineScope, cswHighLevelDslApi)
        stateMachine.state(init) { initFlag = true }

        initFlag = false
    }

    private fun checkInitFlag() {
        eventually(timeout) { initFlag shouldBe true }
    }

    @Test
    fun `start should start the fsm and evaluate the initial state | ESW-142`() = runBlocking {
        stateMachine.start()
        checkInitFlag()
    }

    @Test
    fun `start should throw exception if invalid initial state is given | ESW-142`() = runBlocking<Unit> {
        val invalidStateMachine = StateMachineImpl(testMachineName, invalid, coroutineScope, cswHighLevelDslApi)
        shouldThrow<InvalidStateException> { invalidStateMachine.start() }
    }

    @Test
    fun `become should transition state to given state and evaluate it | ESW-142, ESW-252`() = runBlocking {
        var inProgressFlag = false
        stateMachine.state(inProgress) { inProgressFlag = true }

        stateMachine.start()
        checkInitFlag()

        stateMachine.become(inProgress)
        eventually(timeout) { inProgressFlag shouldBe true }
    }

    @Test
    fun `become should throw exception if invalid state is given | ESW-142, ESW-252`() = runBlocking<Unit> {
        shouldThrow<InvalidStateException> {
            stateMachine.become("INVALIDSTATE")
        }
    }

    @Test
    fun `become should treat stateNames case insensitively | ESW-142, ESW-252`() = runBlocking {
        stateMachine.become(init.toLowerCase())
        checkInitFlag()
    }

    @Test
    fun `become should be able to pass parameters to next state | ESW-252`() = runBlocking {
        val parameter: Parameter<Int> = JKeyType.IntKey().make("encoder").set(1)
        val event = SystemEvent(Prefix(JSubsystem.TCS, "test"), EventName("trigger.INIT.state")).add(parameter)
        val expectedParamsInProgressState = Params(event.jParamSet())

        stateMachine.state(inProgress) { params ->
            parameterSet = params
        }

        stateMachine.start()

        stateMachine.become(inProgress, Params(event.jParamSet()))

        eventually(timeout) {
            parameterSet shouldBe expectedParamsInProgressState
        }

        stateMachine.refresh()

        eventually(timeout) {
            parameterSet shouldBe expectedParamsInProgressState
        }

    }

    @Test
    fun `state should add the given lambda against the state | ESW-142`() = runBlocking {
        stateMachine.start()
        checkInitFlag()
    }

    @Test
    fun `refresh should evaluate fsm with its current state | ESW-142`() = runBlocking {
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
    fun `completeFsm should complete fsm and remove all subscriptions | ESW-142`() = runBlocking {
        var shouldChange = false
        var shouldNotChange = false

        val subscription1: FSMSubscription = mockk()
        val subscription2: FSMSubscription = mockk()
        stateMachine.addFSMSubscription(subscription1)
        stateMachine.addFSMSubscription(subscription2)

        coEvery { subscription1.cancel() }.returns(Unit)
        coEvery { subscription2.cancel() }.returns(Unit)

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
            coVerify { subscription1.cancel() }
            coVerify { subscription2.cancel() }
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

    @Test
    fun `should complete FSM if an exception is thrown in any state`() = runBlocking {
        stateMachine.state(inProgress) { throw RuntimeException("Boom!") }
        stateMachine.start()
        stateMachine.become(inProgress)
        checkInitFlag()
        withTimeout(timeout.toMillis()) {
            stateMachine.await()
        }
    }

    fun `should call the exception handler if exception is thrown in any state`() = runBlocking {
        val job = SupervisorJob()

        var exceptionHandlerCalled = false
        val exceptionHandler = CoroutineExceptionHandler { _, exception -> exceptionHandlerCalled = true }

        val coroutineScope = CoroutineScope(job + exceptionHandler)
        val machine = StateMachineImpl(testMachineName, init, coroutineScope, cswHighLevelDslApi)

        machine.state(init) { initFlag = true }
        machine.state(inProgress) { throw RuntimeException("Boom!") }

        machine.start()
        checkInitFlag()
        machine.become(inProgress)

        eventually(timeout) { exceptionHandlerCalled shouldBe true }
    }

}
