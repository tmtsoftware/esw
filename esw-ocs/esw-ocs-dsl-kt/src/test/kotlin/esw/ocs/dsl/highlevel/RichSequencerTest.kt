package esw.ocs.dsl.highlevel

import akka.util.Timeout
import csw.location.models.AkkaLocation
import csw.params.commands.CommandResponse
import csw.params.commands.Sequence
import csw.params.core.models.Id
import csw.prefix.javadsl.JSubsystem
import csw.prefix.models.Subsystem
import csw.time.core.models.UTCTime
import esw.ocs.api.SequencerApi
import esw.ocs.api.protocol.`Ok$`
import esw.ocs.dsl.highlevel.models.CommandError
import esw.ocs.impl.SequencerActorProxyFactory
import esw.ocs.impl.internal.LocationServiceUtil
import io.kotlintest.shouldNotThrow
import io.kotlintest.shouldThrow
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
import kotlin.time.seconds

class RichSequencerTest {

    private val coroutineScope: CoroutineScope = CoroutineScope(EmptyCoroutineContext)

    private val hint = "test-hint"
    private val startTime: UTCTime = UTCTime.now()

    private val sequencerId: Subsystem = JSubsystem.TCS()
    private val observingMode: String = "darknight"
    private val sequence: Sequence = mockk()

    private val sequencerApiFactory: SequencerActorProxyFactory = mockk()
    private val locationServiceUtil: LocationServiceUtil = mockk()

    private val timeoutDuration: Duration = 10.seconds
    private val timeout = Timeout(timeoutDuration.toLongNanoseconds(), TimeUnit.NANOSECONDS)

    private val defaultTimeoutDuration: Duration = 5.seconds
    private val defaultTimeout = Timeout(defaultTimeoutDuration.toLongNanoseconds(), TimeUnit.NANOSECONDS)

    private val tcsSequencer = RichSequencer(sequencerId, observingMode, sequencerApiFactory, defaultTimeoutDuration, coroutineScope)

    private val sequencerApi: SequencerApi = mockk()

    private val sequencerLocation: AkkaLocation = mockk()

    @Test
    fun `submit should resolve sequencerCommandService for given sequencer and call submit method on it | ESW-245, ESW-195 `() = runBlocking {

        every { locationServiceUtil.resolveSequencer(sequencerId, observingMode, any()) }.answers { Future.successful(sequencerLocation) }
        every { sequencerApiFactory.jMake(sequencerId, observingMode) }.answers { CompletableFuture.completedFuture(sequencerApi) }
        every { sequencerApi.submit(sequence) }.answers { Future.successful(CommandResponse.Completed(Id.apply())) }

        tcsSequencer.submit(sequence)

        verify { sequencerApi.submit(sequence) }
    }

    @Test
    fun `query should resolve sequencerCommandService for given sequencer and call query method on it | ESW-245, ESW-195 `() = runBlocking {
        val runId: Id = mockk()

        every { locationServiceUtil.resolveSequencer(sequencerId, observingMode, any()) }.answers { Future.successful(sequencerLocation) }
        every { sequencerApiFactory.jMake(sequencerId, observingMode) }.answers { CompletableFuture.completedFuture(sequencerApi) }
        every { sequencerApi.query(runId) }.answers { Future.successful(CommandResponse.Completed(Id.apply())) }

        tcsSequencer.query(runId)

        verify { sequencerApi.query(runId) }
    }

    @Test
    fun `queryFinal should resolve sequencerCommandService for given sequencer and call queryFinal method on it | ESW-245, ESW-195 `() = runBlocking {
        val runId: Id = mockk()

        every { locationServiceUtil.resolveSequencer(sequencerId, observingMode, any()) }.answers { Future.successful(sequencerLocation) }
        every { sequencerApiFactory.jMake(sequencerId, observingMode) }.answers { CompletableFuture.completedFuture(sequencerApi) }
        every { sequencerApi.queryFinal(runId, timeout) }.answers { Future.successful(CommandResponse.Completed(Id.apply())) }

        tcsSequencer.queryFinal(runId, timeoutDuration)

        verify { sequencerApi.queryFinal(runId, timeout) }
    }

    @Test
    fun `queryFinal should resolve sequencerCommandService for given sequencer and call queryFinal method on it with defaultTimeout if timeout is not provided | ESW-245, ESW-195 `() = runBlocking {
        val runId: Id = mockk()

        every { locationServiceUtil.resolveSequencer(sequencerId, observingMode, any()) }.answers { Future.successful(sequencerLocation) }
        every { sequencerApiFactory.jMake(sequencerId, observingMode) }.answers { CompletableFuture.completedFuture(sequencerApi) }
        every { sequencerApi.queryFinal(runId, defaultTimeout) }.answers { Future.successful(CommandResponse.Completed(Id.apply())) }

        tcsSequencer.queryFinal(runId)

        verify { sequencerApi.queryFinal(runId, defaultTimeout) }
    }

    @Test
    fun `submitAndWait should resolve sequencerCommandService for given sequencer and call submitAndWait | ESW-245, ESW-195 `() = runBlocking {

        every { locationServiceUtil.resolveSequencer(sequencerId, observingMode, any()) }.answers { Future.successful(sequencerLocation) }
        every { sequencerApiFactory.jMake(sequencerId, observingMode) }.answers { CompletableFuture.completedFuture(sequencerApi) }
        every { sequencerApi.submitAndWait(sequence, timeout) }.answers { Future.successful(CommandResponse.Completed(Id.apply())) }

        tcsSequencer.submitAndWait(sequence, timeoutDuration)

        verify { sequencerApi.submitAndWait(sequence, timeout) }
    }

    @Test
    fun `submitAndWait should resolve sequencerCommandService for given sequencer and call submitAndWait on it with defaultTimeout if timeout is not provided | ESW-245, ESW-195 `() = runBlocking {

        every { locationServiceUtil.resolveSequencer(sequencerId, observingMode, any()) }.answers { Future.successful(sequencerLocation) }
        every { sequencerApiFactory.jMake(sequencerId, observingMode) }.answers { CompletableFuture.completedFuture(sequencerApi) }
        every { sequencerApi.submitAndWait(sequence, defaultTimeout) }.answers { Future.successful(CommandResponse.Completed(Id.apply())) }

        tcsSequencer.submitAndWait(sequence)

        verify { sequencerApi.submitAndWait(sequence, defaultTimeout) }
    }

    @Test
    fun `submitAndWait should resolve sequencerCommandService for given sequencer, call submitAndWait and should throw exception if submit response is negative and resumeOnError=false | ESW-245, ESW-195 `() = runBlocking {

        val message = "error-occurred"
        val invalidSubmitResponse = CommandResponse.Error(Id.apply(), message)

        every { locationServiceUtil.resolveSequencer(sequencerId, observingMode, any()) }.answers { Future.successful(sequencerLocation) }
        every { sequencerApiFactory.jMake(sequencerId, observingMode) }.answers { CompletableFuture.completedFuture(sequencerApi) }
        every { sequencerApi.submitAndWait(sequence, timeout) }.answers { Future.successful(invalidSubmitResponse) }

        shouldThrow<CommandError> { tcsSequencer.submitAndWait(sequence, timeoutDuration) }

        verify { sequencerApi.submitAndWait(sequence, timeout) }
    }

    @Test
    fun `submitAndWait should resolve sequencerCommandService for given sequencer, call submitAndWait and shouldn't throw exception if submit response is negative and resumeOnError=true | ESW-245, ESW-195 `() = runBlocking {

        val message = "error-occurred"
        val invalidSubmitResponse = CommandResponse.Error(Id.apply(), message)

        every { locationServiceUtil.resolveSequencer(sequencerId, observingMode, any()) }.answers { Future.successful(sequencerLocation) }
        every { sequencerApi.submitAndWait(sequence, timeout) }.answers { Future.successful(invalidSubmitResponse) }
        every { sequencerApiFactory.jMake(sequencerId, observingMode) }.answers { CompletableFuture.completedFuture(sequencerApi) }

        shouldNotThrow<CommandError> { tcsSequencer.submitAndWait(sequence, timeoutDuration, resumeOnError = true) }

        verify { sequencerApi.submitAndWait(sequence, timeout) }
    }

    @Test
    fun `diagnosticMode should resolve sequencerAdmin for given sequencer and call diagnosticMode method on it | ESW-143, ESW-245 `() = runBlocking {

        every { sequencerApiFactory.jMake(sequencerId, observingMode) }.answers { CompletableFuture.completedFuture(sequencerApi) }
        every { sequencerApi.diagnosticMode(startTime, hint) }.answers { Future.successful(`Ok$`.`MODULE$`) }

        tcsSequencer.diagnosticMode(startTime, hint)
        verify { sequencerApi.diagnosticMode(startTime, hint) }
    }

    @Test
    fun `operationsMode should resolve sequencerAdmin for given sequencer and call operationsMode method on it | ESW-143, ESW-245 `() = runBlocking {

        every { sequencerApiFactory.jMake(sequencerId, observingMode) }.answers { CompletableFuture.completedFuture(sequencerApi) }
        every { sequencerApi.operationsMode() }.answers { Future.successful(`Ok$`.`MODULE$`) }

        tcsSequencer.operationsMode()
        verify { sequencerApi.operationsMode() }
    }

    @Test
    fun `goOnline should resolve sequencerAdmin for given sequencer and call goOnline method on it | ESW-236, ESW-245 `() = runBlocking {

        every { sequencerApiFactory.jMake(sequencerId, observingMode) }.answers { CompletableFuture.completedFuture(sequencerApi) }
        every { sequencerApi.goOnline() }.answers { Future.successful(`Ok$`.`MODULE$`) }

        tcsSequencer.goOnline()
        verify { sequencerApi.goOnline() }
    }

    @Test
    fun `goOffline should resolve sequencerAdmin for given sequencer and call goOffline method on it | ESW-236, ESW-245 `() = runBlocking {

        every { sequencerApiFactory.jMake(sequencerId, observingMode) }.answers { CompletableFuture.completedFuture(sequencerApi) }
        every { sequencerApi.goOffline() }.answers { Future.successful(`Ok$`.`MODULE$`) }

        tcsSequencer.goOffline()
        verify { sequencerApi.goOffline() }
    }

    @Test
    fun `abortSequence should resolve sequencerAdmin for given sequencer and call abortSequence method on it | ESW-137, ESW-245 `() = runBlocking {

        every { sequencerApiFactory.jMake(sequencerId, observingMode) }.answers { CompletableFuture.completedFuture(sequencerApi) }
        every { sequencerApi.abortSequence() }.answers { Future.successful(`Ok$`.`MODULE$`) }

        tcsSequencer.abortSequence()
        verify { sequencerApi.abortSequence() }
    }

    @Test
    fun `stop should resolve sequencerAdmin for given sequencer and call stop method on it | ESW-138, ESW-245 `() = runBlocking {

        every { sequencerApiFactory.jMake(sequencerId, observingMode) }.answers { CompletableFuture.completedFuture(sequencerApi) }
        every { sequencerApi.stop() }.answers { Future.successful(`Ok$`.`MODULE$`) }

        tcsSequencer.stop()
        verify { sequencerApi.stop() }
    }
}