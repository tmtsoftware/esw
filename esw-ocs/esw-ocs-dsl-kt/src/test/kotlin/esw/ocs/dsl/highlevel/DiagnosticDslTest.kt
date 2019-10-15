package esw.ocs.dsl.highlevel

import akka.actor.typed.ActorRef
import csw.command.client.messages.ComponentMessage
import csw.command.client.messages.DiagnosticDataMessage.DiagnosticMode
import csw.command.client.messages.DiagnosticDataMessage.`OperationsMode$`
import csw.location.api.javadsl.JComponentType
import csw.location.models.ComponentType
import csw.time.core.models.UTCTime
import esw.ocs.api.SequencerAdminApi
import esw.ocs.api.SequencerAdminFactoryApi
import esw.ocs.api.protocol.`Ok$`
import esw.ocs.dsl.sequence_manager.LocationServiceUtil
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import scala.concurrent.Future
import java.util.concurrent.CompletableFuture

class DiagnosticDslTest : DiagnosticDsl {

    private val componentName = "testComponent1"
    private val sequencerId = "testSequencer"
    private val observingMode = "DarkNight"
    private val hint = "test-hint"
    private val componentType: ComponentType = JComponentType.HCD

    private val locationServiceUtil: LocationServiceUtil = mockk()
    private val sequencerAdminApi: SequencerAdminApi = mockk()
    private val sequencerAdminFactoryApi: SequencerAdminFactoryApi = mockk()
    private val componentRef: ActorRef<ComponentMessage> = mockk()

    private val startTime: UTCTime = UTCTime.now()

    override val commonUtils: CommonUtils = CommonUtils(sequencerAdminFactoryApi, locationServiceUtil)

    @Test
    fun `DiagnosticDsl should diagnosticModeForComponent should resolve component ref and send DiagnosticMode msg | ESW-118`() = runBlocking {
        val diagnosticMode = DiagnosticMode(startTime, hint)

        every { componentRef.tell(diagnosticMode) }.answers { Unit }
        every { locationServiceUtil.jResolveComponentRef(componentName, componentType) }
                .answers { CompletableFuture.completedFuture(componentRef) }

        diagnosticModeForComponent(componentName, componentType, startTime, hint)

        verify { locationServiceUtil.jResolveComponentRef(componentName, componentType) }
        verify { componentRef.tell(diagnosticMode) }
    }

    @Test
    fun `operationsModeForComponent should resolve component ref and send OperationsMode msg | ESW-118`() = runBlocking {
        val opsMode = `OperationsMode$`.`MODULE$`

        every { componentRef.tell(opsMode) }.answers { Unit }
        every { locationServiceUtil.jResolveComponentRef(componentName, componentType) }
                .answers { CompletableFuture.completedFuture(componentRef) }

        operationsModeForComponent(componentName, componentType)

        verify { locationServiceUtil.jResolveComponentRef(componentName, componentType) }
        verify { componentRef.tell(opsMode) }
    }

    @Test
    fun `diagnosticModeForSequencer should delegate to sequencerAdminApi#diagnosticMode | ESW-143`() = runBlocking {

        // return value gets discarded
        every { sequencerAdminApi.diagnosticMode(startTime, hint) }
                .answers { Future.successful(`Ok$`.`MODULE$`) }

        every { sequencerAdminFactoryApi.jMake(sequencerId, observingMode) }
                .returns(CompletableFuture.completedFuture(sequencerAdminApi))

        diagnosticModeForSequencer(sequencerId, observingMode, startTime, hint)

        verify { sequencerAdminFactoryApi.jMake(sequencerId, observingMode) }
        verify { sequencerAdminApi.diagnosticMode(startTime, hint) }
    }

    @Test
    fun `operationsModeForSequencer should delegate to sequencerAdminApi#operationsMode | ESW-143`() = runBlocking {

        every { sequencerAdminApi.operationsMode() }
                .answers { Future.successful(`Ok$`.`MODULE$`) }
        every { sequencerAdminFactoryApi.jMake(sequencerId, observingMode) }
                .returns(CompletableFuture.completedFuture(sequencerAdminApi))

        operationsModeForSequencer(sequencerId, observingMode)

        verify { sequencerAdminFactoryApi.jMake(sequencerId, observingMode) }
        verify { sequencerAdminApi.operationsMode() }
    }
}
