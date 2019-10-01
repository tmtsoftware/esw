package esw.ocs.dsl.highlevel

import akka.actor.typed.ActorRef
import csw.command.client.messages.ComponentMessage
import csw.command.client.messages.DiagnosticDataMessage.DiagnosticMode
import csw.command.client.messages.DiagnosticDataMessage.`OperationsMode$`
import csw.location.api.javadsl.JComponentType
import csw.location.models.ComponentType
import csw.time.core.models.UTCTime
import esw.dsl.sequence_manager.LocationServiceUtil
import esw.ocs.api.SequencerAdminApi
import esw.ocs.api.SequencerAdminFactoryApi
import esw.ocs.api.protocol.`Ok$`
import io.kotlintest.specs.WordSpec
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import scala.concurrent.Future
import java.util.concurrent.CompletableFuture

class DiagnosticDslTest : WordSpec({

    class Mocks {
        val componentName = "testComponent1"
        val sequencerId = "testSequencer"
        val observingMode = "DarkNight"
        val hint = "test-hint"
        val componentType: ComponentType = JComponentType.HCD

        val _locationServiceUtil: LocationServiceUtil = mockk()
        val sequencerAdminApi: SequencerAdminApi = mockk()
        val sequencerAdminFactoryApi: SequencerAdminFactoryApi = mockk()
        val componentRef: ActorRef<ComponentMessage> = mockk()

        val startTime: UTCTime = UTCTime.now()

        val diagnosticDsl = object : DiagnosticDsl {
            override val sequencerAdminFactory: SequencerAdminFactoryApi = sequencerAdminFactoryApi
            override val locationServiceUtil: LocationServiceUtil = _locationServiceUtil
        }
    }

    "DiagnosticDsl" should {
        "diagnosticModeForComponent should resolve component ref and send DiagnosticMode msg | ESW-118" {
            with(Mocks()) {
                val diagnosticMode = DiagnosticMode(startTime, hint)

                every { componentRef.tell(diagnosticMode) }.answers { Unit }
                every { _locationServiceUtil.jResolveComponentRef(componentName, componentType) }
                    .answers { CompletableFuture.completedFuture(componentRef) }

                diagnosticDsl.diagnosticModeForComponent(componentName, componentType, startTime, hint)

                verify { _locationServiceUtil.jResolveComponentRef(componentName, componentType) }
                verify { componentRef.tell(diagnosticMode) }
            }
        }

        "operationsModeForComponent should resolve component ref and send OperationsMode msg | ESW-118" {
            with(Mocks()) {
                val opsMode = `OperationsMode$`.`MODULE$`

                every { componentRef.tell(opsMode) }.answers { Unit }
                every { _locationServiceUtil.jResolveComponentRef(componentName, componentType) }
                    .answers { CompletableFuture.completedFuture(componentRef) }

                diagnosticDsl.operationsModeForComponent(componentName, componentType)

                verify { _locationServiceUtil.jResolveComponentRef(componentName, componentType) }
                verify { componentRef.tell(opsMode) }
            }
        }

        "diagnosticModeForSequencer should delegate to sequencerAdminApi.diagnosticMode | ESW-143" {
            with(Mocks()) {

                // return value gets discarded
                every { sequencerAdminApi.diagnosticMode(startTime, hint) }
                    .answers { Future.successful(`Ok$`.`MODULE$`) }

                every { sequencerAdminFactoryApi.jMake(sequencerId, observingMode) }
                    .returns(CompletableFuture.completedFuture(sequencerAdminApi))

                diagnosticDsl.diagnosticModeForSequencer(sequencerId, observingMode, startTime, hint)

                verify { sequencerAdminFactoryApi.jMake(sequencerId, observingMode) }
                verify { sequencerAdminApi.diagnosticMode(startTime, hint) }
            }
        }

        "operationsModeForSequencer should delegate to sequencerAdminApi.operationsMode | ESW-143" {
            with(Mocks()) {

                every { sequencerAdminApi.operationsMode() }
                    .answers { Future.successful(`Ok$`.`MODULE$`) }
                every { sequencerAdminFactoryApi.jMake(sequencerId, observingMode) }
                    .returns(CompletableFuture.completedFuture(sequencerAdminApi))

                diagnosticDsl.operationsModeForSequencer(sequencerId, observingMode)

                verify { sequencerAdminFactoryApi.jMake(sequencerId, observingMode) }
                verify { sequencerAdminApi.operationsMode() }
            }
        }
    }
})