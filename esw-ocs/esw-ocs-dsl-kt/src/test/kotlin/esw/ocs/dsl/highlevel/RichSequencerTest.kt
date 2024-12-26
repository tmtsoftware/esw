package esw.ocs.dsl.highlevel

import org.apache.pekko.util.Timeout
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
    fun `submit_should_resolve_sequencerCommandService_for_given_sequencer_and_call_submit_method_on_it_|_ESW-245,_ESW-195_`() = runBlocking {

        every { sequencerApi.submit(sequence) }.answers { Future.successful(CommandResponse.Completed(Id.apply())) }

        tcsSequencer.submit(sequence)

        verify { sequencerApi.submit(sequence) }
    }

    @Test
    fun `submit_should_resolve_sequencerCommandService_for_given_sequencer,_call_submit_and_should_throw_exception_if_submit_response_is_negative_and_resumeOnError=false_|_ESW-245,_ESW-195,_ESW-139,_ESW-249_`() = runBlocking {
        val message = "error-occurred"
        val invalidSubmitResponse = CommandResponse.Error(Id.apply(), message)

        every { sequencerApi.submit(sequence) }.answers { Future.successful(invalidSubmitResponse) }

        shouldThrow<CommandError> { tcsSequencer.submit(sequence) }

        verify { sequencerApi.submit(sequence) }
    }

    @Test
    fun `submit_should_resolve_sequencerCommandService_for_given_sequencer,_call_submit_and_shouldn't_throw_exception_if_submit_response_is_negative_and_resumeOnError=true_|_ESW-245,_ESW-195,_ESW-139,_ESW-249_`() = runBlocking {
        val message = "error-occurred"
        val invalidSubmitResponse = CommandResponse.Error(Id.apply(), message)

        every { sequencerApi.submit(sequence) }.answers { Future.successful(invalidSubmitResponse) }

        shouldNotThrow<CommandError> { tcsSequencer.submit(sequence, resumeOnError = true) }

        verify { sequencerApi.submit(sequence) }
    }

    @Test
    fun `query_should_resolve_sequencerCommandService_for_given_sequencer_and_call_query_method_on_it_|_ESW-245,_ESW-195_`() = runBlocking {
        val runId: Id = mockk()

        every { sequencerApi.query(runId) }.answers { Future.successful(CommandResponse.Completed(Id.apply())) }

        tcsSequencer.query(runId)

        verify { sequencerApi.query(runId) }
    }

    @Test
    fun `query_should_resolve_sequencerCommandService_for_given_sequencer,_call_query_and_should_throw_exception_if_submit_response_is_negative_and_resumeOnError=false_|_ESW-245,_ESW-195,_ESW-139,_ESW-249_`() = runBlocking {
        val runId: Id = mockk()

        val message = "error-occurred"
        val invalidSubmitResponse = CommandResponse.Error(Id.apply(), message)

        every { sequencerApi.query(runId) }.answers { Future.successful(invalidSubmitResponse) }

        shouldThrow<CommandError> { tcsSequencer.query(runId) }

        verify { sequencerApi.query(runId) }
    }

    @Test
    fun `query_should_resolve_sequencerCommandService_for_given_sequencer,_call_query_and_shouldn't_throw_exception_if_submit_response_is_negative_and_resumeOnError=true_|_ESW-245,_ESW-195,_ESW-139,_ESW-249_`() = runBlocking {
        val runId: Id = mockk()

        val message = "error-occurred"
        val invalidSubmitResponse = CommandResponse.Error(Id.apply(), message)

        every { sequencerApi.query(runId) }.answers { Future.successful(invalidSubmitResponse) }

        shouldNotThrow<CommandError> { tcsSequencer.query(runId, resumeOnError = true) }

        verify { sequencerApi.query(runId) }
    }

    @Test
    fun `queryFinal_should_resolve_sequencerCommandService_for_given_sequencer_and_call_queryFinal_method_on_it_|_ESW-245,_ESW-195_`() = runBlocking {
        val runId: Id = mockk()

        every { sequencerApi.queryFinal(runId, timeout) }.answers { Future.successful(CommandResponse.Completed(Id.apply())) }

        tcsSequencer.queryFinal(runId, timeoutDuration)

        verify { sequencerApi.queryFinal(runId, timeout) }
    }

    @Test
    fun `queryFinal_should_resolve_sequencerCommandService_for_given_sequencer_and_call_queryFinal_method_on_it_with_defaultTimeout_if_timeout_is_not_provided_|_ESW-245,_ESW-195,_ESW-139,_ESW-249_`() = runBlocking {
        val runId: Id = mockk()

        every { sequencerApi.queryFinal(runId, defaultTimeout) }.answers { Future.successful(CommandResponse.Completed(Id.apply())) }

        tcsSequencer.queryFinal(runId)

        verify { sequencerApi.queryFinal(runId, defaultTimeout) }
    }

    @Test
    fun `queryFinal_should_resolve_sequencerCommandService_for_given_sequencer,_call_queryFinal_and_should_throw_exception_if_submit_response_is_negative_and_resumeOnError=false_|_ESW-245,_ESW-195,_ESW-139,_ESW-249_`() = runBlocking {
        val runId: Id = mockk()

        val message = "error-occurred"
        val invalidSubmitResponse = CommandResponse.Error(Id.apply(), message)

        every { sequencerApi.queryFinal(runId, timeout) }.answers { Future.successful(invalidSubmitResponse) }

        shouldThrow<CommandError> { tcsSequencer.queryFinal(runId, timeoutDuration) }

        verify { sequencerApi.queryFinal(runId, timeout) }
    }

    @Test
    fun `queryFinal_should_resolve_sequencerCommandService_for_given_sequencer,_call_queryFinal_and_shouldn't_throw_exception_if_submit_response_is_negative_and_resumeOnError=true_|_ESW-245,_ESW-195,_ESW-139,_ESW-249_`() = runBlocking {
        val runId: Id = mockk()

        val message = "error-occurred"
        val invalidSubmitResponse = CommandResponse.Error(Id.apply(), message)

        every { sequencerApi.queryFinal(runId, timeout) }.answers { Future.successful(invalidSubmitResponse) }

        shouldNotThrow<CommandError> { tcsSequencer.queryFinal(runId, timeoutDuration, resumeOnError = true) }

        verify { sequencerApi.queryFinal(runId, timeout) }
    }

    @Test
    fun `submitAndWait_should_resolve_sequencerCommandService_for_given_sequencer_and_call_submitAndWait_|_ESW-245,_ESW-195_`() = runBlocking {

        every { sequencerApi.submitAndWait(sequence, timeout) }.answers { Future.successful(CommandResponse.Completed(Id.apply())) }

        tcsSequencer.submitAndWait(sequence, timeoutDuration)

        verify { sequencerApi.submitAndWait(sequence, timeout) }
    }

    @Test
    fun `submitAndWait_should_resolve_sequencerCommandService_for_given_sequencer_and_call_submitAndWait_on_it_with_defaultTimeout_if_timeout_is_not_provided_|_ESW-245,_ESW-195,_ESW-139,_ESW-249_`() = runBlocking {

        every { sequencerApi.submitAndWait(sequence, defaultTimeout) }.answers { Future.successful(CommandResponse.Completed(Id.apply())) }

        tcsSequencer.submitAndWait(sequence)

        verify { sequencerApi.submitAndWait(sequence, defaultTimeout) }
    }

    @Test
    fun `submitAndWait_should_resolve_sequencerCommandService_for_given_sequencer,_call_submitAndWait_and_should_throw_exception_if_submit_response_is_negative_and_resumeOnError=false_|_ESW-245,_ESW-195,_ESW-139,_ESW-249_`() = runBlocking {

        val message = "error-occurred"
        val invalidSubmitResponse = CommandResponse.Error(Id.apply(), message)

        every { sequencerApi.submitAndWait(sequence, timeout) }.answers { Future.successful(invalidSubmitResponse) }

        shouldThrow<CommandError> { tcsSequencer.submitAndWait(sequence, timeoutDuration) }

        verify { sequencerApi.submitAndWait(sequence, timeout) }
    }

    @Test
    fun `submitAndWait_should_resolve_sequencerCommandService_for_given_sequencer,_call_submitAndWait_and_shouldn't_throw_exception_if_submit_response_is_negative_and_resumeOnError=true_|_ESW-245,_ESW-195,_ESW-139,_ESW-249_`() = runBlocking {

        val message = "error-occurred"
        val invalidSubmitResponse = CommandResponse.Error(Id.apply(), message)

        every { sequencerApi.submitAndWait(sequence, timeout) }.answers { Future.successful(invalidSubmitResponse) }

        shouldNotThrow<CommandError> { tcsSequencer.submitAndWait(sequence, timeoutDuration, resumeOnError = true) }

        verify { sequencerApi.submitAndWait(sequence, timeout) }
    }

    @Test
    fun `diagnosticMode_should_resolve_sequencerAdmin_for_given_sequencer_and_call_diagnosticMode_method_on_it_|_ESW-143,_ESW-245_`() = runBlocking {

        every { sequencerApi.diagnosticMode(startTime, hint) }.answers { Future.successful(Ok) }

        tcsSequencer.diagnosticMode(startTime, hint)
        verify { sequencerApi.diagnosticMode(startTime, hint) }
    }

    @Test
    fun `operationsMode_should_resolve_sequencerAdmin_for_given_sequencer_and_call_operationsMode_method_on_it_|_ESW-143,_ESW-245_`() = runBlocking {

        every { sequencerApi.operationsMode() }.answers { Future.successful(Ok) }

        tcsSequencer.operationsMode()
        verify { sequencerApi.operationsMode() }
    }

    @Test
    fun `goOnline_should_resolve_sequencerAdmin_for_given_sequencer_and_call_goOnline_method_on_it_|_ESW-236,_ESW-245_`() = runBlocking {

        every { sequencerApi.goOnline() }.answers { Future.successful(Ok) }

        tcsSequencer.goOnline()
        verify { sequencerApi.goOnline() }
    }

    @Test
    fun `goOffline_should_resolve_sequencerAdmin_for_given_sequencer_and_call_goOffline_method_on_it_|_ESW-236,_ESW-245_`() {
        runBlocking {

            every { sequencerApi.goOffline() }.answers { Future.successful(Ok) }

            tcsSequencer.goOffline()
            verify { sequencerApi.goOffline() }
        }
    }

    @Test
    fun `abortSequence_should_resolve_sequencerAdmin_for_given_sequencer_and_call_abortSequence_method_on_it_|_ESW-137,_ESW-245_`() = runBlocking {

        every { sequencerApi.abortSequence() }.answers { Future.successful(Ok) }

        tcsSequencer.abortSequence()
        verify { sequencerApi.abortSequence() }
    }

    @Test
    fun `stop_should_resolve_sequencerAdmin_for_given_sequencer_and_call_stop_method_on_it_|_ESW-138,_ESW-245_`() = runBlocking {

        every { sequencerApi.stop() }.answers { Future.successful(Ok) }

        tcsSequencer.stop()
        verify { sequencerApi.stop() }
    }
}
