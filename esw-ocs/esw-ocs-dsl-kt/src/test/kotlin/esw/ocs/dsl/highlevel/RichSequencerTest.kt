package esw.ocs.dsl.highlevel

import akka.util.Timeout
import csw.params.commands.CommandResponse
import csw.params.commands.Sequence
import csw.params.core.models.Id
import csw.prefix.models.Prefix
import csw.prefix.models.Subsystem
import csw.time.core.models.UTCTime
import esw.ocs.api.SequencerApi
import esw.ocs.api.models.ObsMode
import esw.ocs.api.models.Variation
import esw.ocs.api.protocol.`Ok$`
import esw.ocs.dsl.highlevel.models.CommandError
import esw.ocs.dsl.highlevel.models.TCS
import io.kotest.assertions.throwables.shouldNotThrow
import io.kotest.assertions.throwables.shouldThrow
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import scala.concurrent.Future
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

@Suppress("DANGEROUS_CHARACTERS")
class RichSequencerTest {

    private val coroutineScope: CoroutineScope = CoroutineScope(EmptyCoroutineContext)

    private val Ok = `Ok$`.`MODULE$`

    private val hint = "test-hint"
    private val startTime: UTCTime = UTCTime.now()

    private val subsystem: Subsystem = TCS
    private val obsMode: ObsMode = ObsMode("darknight")
    private val sequence: Sequence = mockk()
    private val variation = Variation("random")

    //ESW-561
    private val sequencerApiFactory: (Subsystem, ObsMode, Variation?) -> CompletableFuture<SequencerApi> = { _, _, _ -> CompletableFuture.completedFuture(sequencerApi) }

    private val timeoutDuration: Duration = 10.seconds
    private val timeout = Timeout(timeoutDuration.inWholeNanoseconds, TimeUnit.NANOSECONDS)

    private val defaultTimeoutDuration: Duration = 5.seconds
    private val defaultTimeout = Timeout(defaultTimeoutDuration.inWholeNanoseconds, TimeUnit.NANOSECONDS)

    //ESW-561
    private val tcsSequencer = RichSequencer(subsystem, obsMode, variation, sequencerApiFactory, defaultTimeoutDuration, coroutineScope)

    private val sequencerApi: SequencerApi = mockk()

    @Test
    fun `submit should resolve sequencerCommandService for given sequencer and call submit method on it | ESW-245, ESW-195 `() = runBlocking {

        every { sequencerApi.submit(sequence) }.answers { Future.successful(CommandResponse.Completed(Id.apply())) }

        tcsSequencer.submit(sequence)

        verify { sequencerApi.submit(sequence) }
    }

    @Test
    fun `submit should resolve sequencerCommandService for given sequencer, call submit and should throw exception if submit response is negative and resumeOnError=false | ESW-245, ESW-195, ESW-139, ESW-249 `() = runBlocking {
        val message = "error-occurred"
        val invalidSubmitResponse = CommandResponse.Error(Id.apply(), message)

        every { sequencerApi.submit(sequence) }.answers { Future.successful(invalidSubmitResponse) }

        shouldThrow<CommandError> { tcsSequencer.submit(sequence) }

        verify { sequencerApi.submit(sequence) }
    }

    @Test
    fun `submit should resolve sequencerCommandService for given sequencer, call submit and shouldn't throw exception if submit response is negative and resumeOnError=true | ESW-245, ESW-195, ESW-139, ESW-249 `() = runBlocking {
        val message = "error-occurred"
        val invalidSubmitResponse = CommandResponse.Error(Id.apply(), message)

        every { sequencerApi.submit(sequence) }.answers { Future.successful(invalidSubmitResponse) }

        shouldNotThrow<CommandError> { tcsSequencer.submit(sequence, resumeOnError = true) }

        verify { sequencerApi.submit(sequence) }
    }

    @Test
    fun `query should resolve sequencerCommandService for given sequencer and call query method on it | ESW-245, ESW-195 `() = runBlocking {
        val runId: Id = mockk()

        every { sequencerApi.query(runId) }.answers { Future.successful(CommandResponse.Completed(Id.apply())) }

        tcsSequencer.query(runId)

        verify { sequencerApi.query(runId) }
    }

    @Test
    fun `query should resolve sequencerCommandService for given sequencer, call query and should throw exception if submit response is negative and resumeOnError=false | ESW-245, ESW-195, ESW-139, ESW-249 `() = runBlocking {
        val runId: Id = mockk()

        val message = "error-occurred"
        val invalidSubmitResponse = CommandResponse.Error(Id.apply(), message)

        every { sequencerApi.query(runId) }.answers { Future.successful(invalidSubmitResponse) }

        shouldThrow<CommandError> { tcsSequencer.query(runId) }

        verify { sequencerApi.query(runId) }
    }

    @Test
    fun `query should resolve sequencerCommandService for given sequencer, call query and shouldn't throw exception if submit response is negative and resumeOnError=true | ESW-245, ESW-195, ESW-139, ESW-249 `() = runBlocking {
        val runId: Id = mockk()

        val message = "error-occurred"
        val invalidSubmitResponse = CommandResponse.Error(Id.apply(), message)

        every { sequencerApi.query(runId) }.answers { Future.successful(invalidSubmitResponse) }

        shouldNotThrow<CommandError> { tcsSequencer.query(runId, resumeOnError = true) }

        verify { sequencerApi.query(runId) }
    }

    @Test
    fun `queryFinal should resolve sequencerCommandService for given sequencer and call queryFinal method on it | ESW-245, ESW-195 `() = runBlocking {
        val runId: Id = mockk()

        every { sequencerApi.queryFinal(runId, timeout) }.answers { Future.successful(CommandResponse.Completed(Id.apply())) }

        tcsSequencer.queryFinal(runId, timeoutDuration)

        verify { sequencerApi.queryFinal(runId, timeout) }
    }

    @Test
    fun `queryFinal should resolve sequencerCommandService for given sequencer and call queryFinal method on it with defaultTimeout if timeout is not provided | ESW-245, ESW-195, ESW-139, ESW-249 `() = runBlocking {
        val runId: Id = mockk()

        every { sequencerApi.queryFinal(runId, defaultTimeout) }.answers { Future.successful(CommandResponse.Completed(Id.apply())) }

        tcsSequencer.queryFinal(runId)

        verify { sequencerApi.queryFinal(runId, defaultTimeout) }
    }

    @Test
    fun `queryFinal should resolve sequencerCommandService for given sequencer, call queryFinal and should throw exception if submit response is negative and resumeOnError=false | ESW-245, ESW-195, ESW-139, ESW-249 `() = runBlocking {
        val runId: Id = mockk()

        val message = "error-occurred"
        val invalidSubmitResponse = CommandResponse.Error(Id.apply(), message)

        every { sequencerApi.queryFinal(runId, timeout) }.answers { Future.successful(invalidSubmitResponse) }

        shouldThrow<CommandError> { tcsSequencer.queryFinal(runId, timeoutDuration) }

        verify { sequencerApi.queryFinal(runId, timeout) }
    }

    @Test
    fun `queryFinal should resolve sequencerCommandService for given sequencer, call queryFinal and shouldn't throw exception if submit response is negative and resumeOnError=true | ESW-245, ESW-195, ESW-139, ESW-249 `() = runBlocking {
        val runId: Id = mockk()

        val message = "error-occurred"
        val invalidSubmitResponse = CommandResponse.Error(Id.apply(), message)

        every { sequencerApi.queryFinal(runId, timeout) }.answers { Future.successful(invalidSubmitResponse) }

        shouldNotThrow<CommandError> { tcsSequencer.queryFinal(runId, timeoutDuration, resumeOnError = true) }

        verify { sequencerApi.queryFinal(runId, timeout) }
    }

    @Test
    fun `submitAndWait should resolve sequencerCommandService for given sequencer and call submitAndWait | ESW-245, ESW-195 `() = runBlocking {

        every { sequencerApi.submitAndWait(sequence, timeout) }.answers { Future.successful(CommandResponse.Completed(Id.apply())) }

        tcsSequencer.submitAndWait(sequence, timeoutDuration)

        verify { sequencerApi.submitAndWait(sequence, timeout) }
    }

    @Test
    fun `submitAndWait should resolve sequencerCommandService for given sequencer and call submitAndWait on it with defaultTimeout if timeout is not provided | ESW-245, ESW-195, ESW-139, ESW-249 `() = runBlocking {

        every { sequencerApi.submitAndWait(sequence, defaultTimeout) }.answers { Future.successful(CommandResponse.Completed(Id.apply())) }

        tcsSequencer.submitAndWait(sequence)

        verify { sequencerApi.submitAndWait(sequence, defaultTimeout) }
    }

    @Test
    fun `submitAndWait should resolve sequencerCommandService for given sequencer, call submitAndWait and should throw exception if submit response is negative and resumeOnError=false | ESW-245, ESW-195, ESW-139, ESW-249 `() = runBlocking {

        val message = "error-occurred"
        val invalidSubmitResponse = CommandResponse.Error(Id.apply(), message)

        every { sequencerApi.submitAndWait(sequence, timeout) }.answers { Future.successful(invalidSubmitResponse) }

        shouldThrow<CommandError> { tcsSequencer.submitAndWait(sequence, timeoutDuration) }

        verify { sequencerApi.submitAndWait(sequence, timeout) }
    }

    @Test
    fun `submitAndWait should resolve sequencerCommandService for given sequencer, call submitAndWait and shouldn't throw exception if submit response is negative and resumeOnError=true | ESW-245, ESW-195, ESW-139, ESW-249 `() = runBlocking {

        val message = "error-occurred"
        val invalidSubmitResponse = CommandResponse.Error(Id.apply(), message)

        every { sequencerApi.submitAndWait(sequence, timeout) }.answers { Future.successful(invalidSubmitResponse) }

        shouldNotThrow<CommandError> { tcsSequencer.submitAndWait(sequence, timeoutDuration, resumeOnError = true) }

        verify { sequencerApi.submitAndWait(sequence, timeout) }
    }

    @Test
    fun `diagnosticMode should resolve sequencerAdmin for given sequencer and call diagnosticMode method on it | ESW-143, ESW-245 `() = runBlocking {

        every { sequencerApi.diagnosticMode(startTime, hint) }.answers { Future.successful(Ok) }

        tcsSequencer.diagnosticMode(startTime, hint)
        verify { sequencerApi.diagnosticMode(startTime, hint) }
    }

    @Test
    fun `operationsMode should resolve sequencerAdmin for given sequencer and call operationsMode method on it | ESW-143, ESW-245 `() = runBlocking {

        every { sequencerApi.operationsMode() }.answers { Future.successful(Ok) }

        tcsSequencer.operationsMode()
        verify { sequencerApi.operationsMode() }
    }

    @Test
    fun `goOnline should resolve sequencerAdmin for given sequencer and call goOnline method on it | ESW-236, ESW-245 `() = runBlocking {

        every { sequencerApi.goOnline() }.answers { Future.successful(Ok) }

        tcsSequencer.goOnline()
        verify { sequencerApi.goOnline() }
    }

    @Test
    fun `goOffline should resolve sequencerAdmin for given sequencer and call goOffline method on it | ESW-236, ESW-245 `() {
        runBlocking {

            every { sequencerApi.goOffline() }.answers { Future.successful(Ok) }

            tcsSequencer.goOffline()
            verify { sequencerApi.goOffline() }
        }
    }

    @Test
    fun `abortSequence should resolve sequencerAdmin for given sequencer and call abortSequence method on it | ESW-137, ESW-245 `() = runBlocking {

        every { sequencerApi.abortSequence() }.answers { Future.successful(Ok) }

        tcsSequencer.abortSequence()
        verify { sequencerApi.abortSequence() }
    }

    @Test
    fun `stop should resolve sequencerAdmin for given sequencer and call stop method on it | ESW-138, ESW-245 `() = runBlocking {

        every { sequencerApi.stop() }.answers { Future.successful(Ok) }

        tcsSequencer.stop()
        verify { sequencerApi.stop() }
    }
}
