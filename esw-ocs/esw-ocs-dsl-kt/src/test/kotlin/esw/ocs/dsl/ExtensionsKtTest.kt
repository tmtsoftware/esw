package esw.ocs.dsl

import csw.params.commands.CommandResponse.*
import csw.params.commands.Result
import csw.params.core.models.Id
import esw.ocs.dsl.highlevel.models.CommandError
import io.kotest.assertions.throwables.shouldNotThrow
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

@Suppress("DANGEROUS_CHARACTERS")
class ExtensionsKtTest {
    val id = Id("testId")
    private val msg = "msg"

    private val error = Error(id, msg)
    private val locked = Locked(id)
    private val cancelled = Cancelled(id)
    private val invalid = Invalid(id) { msg }
    private val started = Started(id)
    private val completed = Completed(id)

    @Test
    fun `isFailed_should_return_true_if_submit_response_is_negative_and_false_for_other_|_ESW-249`() {
        error.isFailed shouldBe true
        locked.isFailed shouldBe true
        cancelled.isFailed shouldBe true
        invalid.isFailed shouldBe true

        started.isFailed shouldBe false
        completed.isFailed shouldBe false
    }

    @Test
    fun `isStarted_should_return_true_if_submit_response_is_Started_and_false_for_other_|_ESW-249`() {
        started.isStarted shouldBe true

        completed.isStarted shouldBe false
        error.isStarted shouldBe false
        locked.isStarted shouldBe false
        cancelled.isStarted shouldBe false
        invalid.isStarted shouldBe false
    }

    @Test
    fun `isCompleted_should_return_true_if_submit_response_is_Completed_and_false_for_other_|_ESW-249`() {
        completed.isCompleted shouldBe true

        started.isCompleted shouldBe false
        error.isCompleted shouldBe false
        locked.isCompleted shouldBe false
        cancelled.isCompleted shouldBe false
        invalid.isCompleted shouldBe false
    }


    @Test
    fun `onStarted_should_execute_given_block_if_response_is_Started`() = runBlocking {
        var isCalled = false

        started.onStarted { isCalled = true }
        isCalled shouldBe true
    }

    @Test
    fun `onStarted_should_not_execute_given_block_if_response_is_not_Started`() = runBlocking {
        var isCalled = false

        error.onStarted { isCalled = true }
        isCalled shouldBe false
    }


    @Test
    fun `onCompleted_should_execute_given_block_if_response_is_Completed`() = runBlocking {
        var isCalled = false

        completed.onCompleted { isCalled = true }
        isCalled shouldBe true
    }

    @Test
    fun `onCompleted_should_not_execute_given_block_if_response_is_not_Completed`() = runBlocking {
        var isCalled = false

        error.onCompleted { isCalled = true }
        isCalled shouldBe false
    }


    @Test
    fun `onFailed_should_execute_given_block_if_response_is_negative`() = runBlocking {
        var isCalled = false

        error.onFailed { isCalled = true }
        isCalled shouldBe true
    }

    @Test
    fun `onFailed_should_not_execute_given_block_if_response_is_not_negative`() = runBlocking {
        var isCalled = false

        started.onFailed { isCalled = true }
        isCalled shouldBe false
    }

    @Test
    fun `onFailedTerminate_should_throw_exception_for_negative_responses`() = runBlocking<Unit> {
        shouldThrow<CommandError> { error.onFailedTerminate() }
        shouldThrow<CommandError> { locked.onFailedTerminate() }
        shouldThrow<CommandError> { cancelled.onFailedTerminate() }
        shouldThrow<CommandError> { invalid.onFailedTerminate() }
    }

    @Test
    fun `onFailedTerminate_should_not_throw_exception_for_positive_responses`() = runBlocking {
        shouldNotThrow<CommandError> { started.onFailedTerminate() }
        shouldNotThrow<CommandError> { completed.onFailedTerminate() }
    }

    @Test
    fun `completed_should_throw_exception_if_called_on_any_other_submit_response_than_completed`() = runBlocking<Unit> {
        shouldNotThrow<CommandError> { completed.completed }
        shouldThrow<CommandError> { started.completed }
    }

    @Test
    fun `result_should_return_result_from_Completed_response_if_called_on_Completed_response_and_throw_for_others`() = runBlocking<Unit> {
        val result = Result()
        Completed(id, result).result shouldBe result
        shouldThrow<CommandError> { started.completed }
    }
}
