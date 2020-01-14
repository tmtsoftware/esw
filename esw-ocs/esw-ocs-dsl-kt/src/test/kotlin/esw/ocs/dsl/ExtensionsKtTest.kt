package esw.ocs.dsl

import csw.params.commands.CommandResponse.*
import csw.params.commands.Result
import csw.params.core.models.Id
import esw.ocs.dsl.highlevel.models.CommandError
import io.kotlintest.shouldBe
import io.kotlintest.shouldNotThrow
import io.kotlintest.shouldThrow
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

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
    fun `isFailed should return true if submit response is negative and false for other | ESW-249`() {
        error.isFailed shouldBe true
        locked.isFailed shouldBe true
        cancelled.isFailed shouldBe true
        invalid.isFailed shouldBe true

        started.isFailed shouldBe false
        completed.isFailed shouldBe false
    }

    @Test
    fun `isStarted should return true if submit response is Started and false for other | ESW-249`() {
        started.isStarted shouldBe true

        completed.isStarted shouldBe false
        error.isStarted shouldBe false
        locked.isStarted shouldBe false
        cancelled.isStarted shouldBe false
        invalid.isStarted shouldBe false
    }

    @Test
    fun `isCompleted should return true if submit response is Completed and false for other | ESW-249`() {
        completed.isCompleted shouldBe true

        started.isCompleted shouldBe false
        error.isCompleted shouldBe false
        locked.isCompleted shouldBe false
        cancelled.isCompleted shouldBe false
        invalid.isCompleted shouldBe false
    }


    @Test
    fun `onStarted should execute given block if response is Started`() = runBlocking {
        var isCalled = false

        started.onStarted { isCalled = true }
        isCalled shouldBe true
    }

    @Test
    fun `onStarted should not execute given block if response is not Started`() = runBlocking {
        var isCalled = false

        error.onStarted { isCalled = true }
        isCalled shouldBe false
    }


    @Test
    fun `onCompleted should execute given block if response is Completed`() = runBlocking {
        var isCalled = false

        completed.onCompleted { isCalled = true }
        isCalled shouldBe true
    }

    @Test
    fun `onCompleted should not execute given block if response is not Completed`() = runBlocking {
        var isCalled = false

        error.onCompleted { isCalled = true }
        isCalled shouldBe false
    }


    @Test
    fun `onFailed should execute given block if response is negative`() = runBlocking {
        var isCalled = false

        error.onFailed { isCalled = true }
        isCalled shouldBe true
    }

    @Test
    fun `onFailed should not execute given block if response is not negative`() = runBlocking {
        var isCalled = false

        started.onFailed { isCalled = true }
        isCalled shouldBe false
    }

    @Test
    fun `onFailedTerminate should throw exception for negative responses`() = runBlocking<Unit> {
        shouldThrow<CommandError> { error.onFailedTerminate() }
        shouldThrow<CommandError> { locked.onFailedTerminate() }
        shouldThrow<CommandError> { cancelled.onFailedTerminate() }
        shouldThrow<CommandError> { invalid.onFailedTerminate() }
    }

    @Test
    fun `onFailedTerminate should not throw exception for positive responses`() = runBlocking {
        shouldNotThrow<CommandError> { started.onFailedTerminate() }
        shouldNotThrow<CommandError> { completed.onFailedTerminate() }
    }

    @Test
    fun `completed should throw exception if called on any other submit response than completed`() = runBlocking<Unit> {
        shouldNotThrow<CommandError> { completed.completed }
        shouldThrow<CommandError> { started.completed }
    }

    @Test
    fun `result should return result from Completed response if called on Completed response and throw for others`() = runBlocking<Unit> {
        val result = Result()
        Completed(id, result).result shouldBe result
        shouldThrow<CommandError> { started.completed }
    }
}