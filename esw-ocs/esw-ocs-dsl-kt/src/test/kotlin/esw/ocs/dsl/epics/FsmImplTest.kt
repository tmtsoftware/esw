package esw.ocs.dsl.epics

import csw.logging.api.javadsl.ILogger
import csw.params.core.generics.Parameter
import csw.params.events.EventName
import csw.params.events.SystemEvent
import csw.params.javadsl.JKeyType
import csw.prefix.models.Prefix
import esw.ocs.dsl.highlevel.CswHighLevelDslApi
import esw.ocs.dsl.highlevel.models.TCS
import esw.ocs.dsl.params.Params
import esw.ocs.dsl.script.StrandEc
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.assertions.nondeterministic.eventually
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@Suppress("DANGEROUS_CHARACTERS")
class FsmImplTest {

    // These are needed to simulate script like single threaded environment
    private val job = SupervisorJob()
    private val _strandEc = StrandEc.apply()
    private val dispatcher = _strandEc.executorService().asCoroutineDispatcher()
    private val exceptionHandler = CoroutineExceptionHandler { _, exception ->
        println("Exception thrown in script with a message: ${exception.message}, invoking exception handler " + exception)
    }
    private val coroutineScope = CoroutineScope(job + exceptionHandler + dispatcher)
    private val cswHighLevelDslApi: CswHighLevelDslApi = mockk()

    private
    val init = "INIT"
    private val inProgress = "INPROGRESS"
    private val invalid = "INVALIDSTATE"
    private val testMachineName = "test-state-machine"
    private val timeout = 100.milliseconds

    private var initFlag = false
    private val initState: suspend FsmStateScope.(Params) -> Unit = { initFlag = true }
    private var parameterSet = Params(setOf())

    // instantiating to not to deal with nullable
    private var fsm = FsmImpl(testMachineName, invalid, coroutineScope, cswHighLevelDslApi)

    @BeforeEach
    fun beforeEach() {
        every { cswHighLevelDslApi.debug(any()) }.returns(Unit)

        fsm = FsmImpl(testMachineName, init, coroutineScope, cswHighLevelDslApi)
        fsm.state(init, initState)

        initFlag = false
    }

    private suspend fun checkInitFlag() {
        eventually(timeout) { initFlag shouldBe true }
    }

    @Test
    fun `start_should_start_the_fsm_and_evaluate_the_initial_state_|_ESW-142`() = runBlocking {
        fsm.start()
        checkInitFlag()
    }

    @Test
    fun `start_should_throw_exception_if_invalid_initial_state_is_given_|_ESW-142`() = runBlocking<Unit> {
        val invalidStateMachine = FsmImpl(testMachineName, invalid, coroutineScope, cswHighLevelDslApi)
        shouldThrow<InvalidStateException> { invalidStateMachine.start() }
    }

    @Test
    fun `become_should_transition_state_to_given_state_and_evaluate_it_|_ESW-142`() = runBlocking {
        var inProgressFlag = false
        fsm.state(inProgress) { inProgressFlag = true }

        fsm.start()
        checkInitFlag()

        fsm.become(inProgress)
        eventually(timeout) { inProgressFlag shouldBe true }
    }

    @Test
    fun `become_should_throw_exception_if_invalid_state_is_given_|_ESW-142`() = runBlocking<Unit> {
        shouldThrow<InvalidStateException> {
            fsm.become("INVALIDSTATE")
        }
    }

    @Test
    fun `become_should_treat_stateNames_case_insensitively_|_ESW-142`() = runBlocking {
        fsm.become(init.lowercase())
        checkInitFlag()
    }

    @Test
    fun `become_should_be_able_to_pass_parameters_to_next_state_|_ESW-252`() = runBlocking {
        val parameter: Parameter<Int> = JKeyType.IntKey().make("encoder").set(1)
        val event = SystemEvent(Prefix(TCS, "test"), EventName("trigger.INIT.state")).add(parameter)
        val expectedParamsInProgressState = Params(event.jParamSet())

        fsm.state(inProgress) { params ->
            parameterSet = params
        }

        fsm.start()

        fsm.become(inProgress, Params(event.jParamSet()))

        eventually(timeout) {
            parameterSet shouldBe expectedParamsInProgressState
        }

        fsm.refresh()

        eventually(timeout) {
            parameterSet shouldBe expectedParamsInProgressState
        }

    }

    @Test
    fun `state_should_add_the_given_lambda_against_the_state_|_ESW-142`() = runBlocking {
        fsm.start()
        checkInitFlag()
    }

    @Test
    fun `refresh_should_evaluate_fsm_with_its_current_state_|_ESW-142`() = runBlocking {
        var firstCalled = false
        var refreshFlag = false

        fsm.state(inProgress) {
            if (firstCalled) refreshFlag = true
            else firstCalled = true
        }

        fsm.start()
        checkInitFlag()

        fsm.become(inProgress)
        eventually(timeout) { firstCalled shouldBe true }
        eventually(timeout) { refreshFlag shouldBe false }

        fsm.refresh()
        eventually(timeout) { refreshFlag shouldBe true }
    }

    @Test
    fun `on_should_execute_the_given_lambda_if_given_condition_is_true_|_ESW-142`() = runBlocking {
        var flag = false
        fsm.on(true) {
            flag = true
        }

        flag shouldBe true
    }

    @Test
    fun `on_should_not_execute_the_given_lambda_if_given_condition_is_false_|_ESW-142`() = runBlocking {
        var flag = false
        fsm.on(false) {
            flag = true
        }

        flag shouldBe false
    }

    @Test
    fun `after_should_execute_given_lambda_after_specified_time_|_ESW-142`() = runBlocking {
        var flag = false
        coroutineScope.launch {
            fsm.after(2.seconds) {
                flag = true
            }
        }

        delay(1000)
        flag shouldBe false
        delay(1000)
        eventually(200.milliseconds) { flag shouldBe true }
    }

    @Test
    fun `entry_should_call_the_given_lambda_only_if_state_transition_happens_from_other_state_|_ESW-142`() = runBlocking {
        var entryCalled = false

        fsm.start()
        checkInitFlag()

        // current state is INIT and previous state is null.
        fsm.entry {
            entryCalled = true

        }
        entryCalled shouldBe true
    }

    @Test
    fun `entry_should_not_call_the_given_lambda_if_state_transition_happens_in_same_state_|_ESW-142`() = runBlocking {
        var entryCalled = false
        fsm.start()
        fsm.become(init)
        checkInitFlag()


        // current and previous state both are INIT
        fsm.entry {
            entryCalled = true
        }

        entryCalled shouldBe false
    }

    @Test
    fun `completeFsm_should_complete_fsm_and_remove_all_subscriptions_|_ESW-142`() = runBlocking {
        val coroutineScope = CoroutineScope(SupervisorJob() + dispatcher)
        val fsm = FsmImpl(testMachineName, init, coroutineScope, cswHighLevelDslApi)

        var shouldChange = false
        var shouldNotChange = false

        val subscription1: FsmSubscription = mockk()
        val subscription2: FsmSubscription = mockk()
        fsm.addFsmSubscription(subscription2)
        fsm.addFsmSubscription(subscription1)

        coEvery { subscription1.cancel() }.returns(Unit)
        coEvery { subscription2.cancel() }.returns(Unit)

        fsm.state(init, initState)
        fsm.state(inProgress) {
            shouldChange = true
            fsm.completeFsm()
            shouldNotChange = true
        }

        fsm.start()
        checkInitFlag()
        fsm.become(inProgress)

        coroutineScope.launch {
            eventually(timeout) { shouldChange shouldBe true }
            eventually(timeout) { shouldNotChange shouldBe false }

            fsm.await()
            coVerify { subscription1.cancel() }
            coVerify { subscription2.cancel() }
            eventually(timeout) { shouldNotChange shouldBe false }
        }.join()
    }

    @Test
    fun `await_should_wait_for_completion_of_fsm_|_ESW-142`() = runBlocking {
        var waitingStarted = false
        var waitingFinished = false

        coroutineScope.launch {
            waitingStarted = true
            fsm.await()
            waitingFinished = true
        }

        eventually(timeout) { waitingStarted shouldBe true }
        eventually(timeout) { waitingFinished shouldBe false }

        fsm.completeFsm()

        eventually(timeout) { waitingFinished shouldBe true }
    }

    @Test
    fun `await_should_start_and_wait_for_completion_of_fsm_if_not_started_previously_|_ESW-142`() = runBlocking {
        var started = false
        fsm.state(init) {
            started = true
            completeFsm()
        }
        eventually(timeout) { started shouldBe false }

        fsm.await()
        eventually(timeout) { started shouldBe true }
    }

    @Test
    fun `should_complete_Fsm_if_an_exception_is_thrown_in_any_state`() = runBlocking {
        fsm.state(inProgress) { throw RuntimeException("Boom!") }
        fsm.start()
        fsm.become(inProgress)
        checkInitFlag()
        withTimeout(timeout) {
            fsm.await()
        }
    }

    @Test
    fun `should_call_the_exception_handler_if_exception_is_thrown_in_any_state`() = runBlocking {
        val job = SupervisorJob()

        var exceptionHandlerCalled = false
        val exceptionHandler = CoroutineExceptionHandler { _, _ -> exceptionHandlerCalled = true }

        val coroutineScope = CoroutineScope(job + exceptionHandler)
        val fsm = FsmImpl(testMachineName, init, coroutineScope, cswHighLevelDslApi)

        fsm.state(init, initState)
        fsm.state(inProgress) { throw RuntimeException("Boom!") }

        fsm.start()
        checkInitFlag()
        fsm.become(inProgress)

        eventually(timeout) { exceptionHandlerCalled shouldBe true }
    }

}
