package esw.ocs.dsl.highlevel

import akka.actor.typed.ActorRef
import csw.command.client.messages.ComponentMessage
import csw.command.client.messages.RunningMessage.Lifecycle
import csw.command.client.models.framework.ToComponentLifecycleMessage.`GoOffline$`
import csw.command.client.models.framework.ToComponentLifecycleMessage.`GoOnline$`
import csw.location.api.javadsl.JComponentType
import csw.location.models.ComponentType
import esw.ocs.api.SequencerAdminApi
import esw.ocs.api.SequencerAdminFactoryApi
import esw.ocs.api.protocol.`Ok$`
import esw.ocs.dsl.sequence_manager.LocationServiceUtil
import io.kotlintest.specs.WordSpec
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.util.concurrent.CompletableFuture
import scala.concurrent.Future

class OnlineOfflineDslTest : WordSpec({

    class Mocks {
        val componentName = "testComponent1"
        val sequencerId = "testSequencer"
        val observingMode = "DarkNight"
        val componentType: ComponentType = JComponentType.HCD

        val _locationServiceUtil: LocationServiceUtil = mockk()
        val sequencerAdminApi: SequencerAdminApi = mockk()
        val sequencerAdminFactoryApi: SequencerAdminFactoryApi = mockk()
        val componentRef: ActorRef<ComponentMessage> = mockk()

        val onlineOfflineDsl = object : OnlineOfflineDsl {
            override val commonUtils: CommonUtils = CommonUtils(sequencerAdminFactoryApi, _locationServiceUtil)
        }
    }

    "OnlineOfflineDsl" should {
        "goOnlineModeForComponent should resolve component ref and send GoOnline msg | ESW-236" {
            with(Mocks()) {
                val goOnlineMsg = Lifecycle(`GoOnline$`.`MODULE$`)

                every { componentRef.tell(goOnlineMsg) }.answers { Unit }
                every { _locationServiceUtil.jResolveComponentRef(componentName, componentType) }
                    .answers { CompletableFuture.completedFuture(componentRef) }

                onlineOfflineDsl.goOnlineModeForComponent(componentName, componentType)

                verify { _locationServiceUtil.jResolveComponentRef(componentName, componentType) }
                verify { componentRef.tell(goOnlineMsg) }
            }
        }

        "goOfflineModeForComponent should resolve component ref and send GoOffline msg | ESW-236" {
            with(Mocks()) {
                val goOfflineMsg = Lifecycle(`GoOffline$`.`MODULE$`)

                every { componentRef.tell(goOfflineMsg) }.answers { Unit }
                every { _locationServiceUtil.jResolveComponentRef(componentName, componentType) }
                    .answers { CompletableFuture.completedFuture(componentRef) }

                onlineOfflineDsl.goOfflineModeForComponent(componentName, componentType)

                verify { _locationServiceUtil.jResolveComponentRef(componentName, componentType) }
                verify { componentRef.tell(goOfflineMsg) }
            }
        }

        "goOnlineModeForSequencer should delegate to sequencerAdminApi.goOnline | ESW-236" {
            with(Mocks()) {

                // return value gets discarded
                every { sequencerAdminApi.goOnline() }
                    .answers { Future.successful(`Ok$`.`MODULE$`) }

                every { sequencerAdminFactoryApi.jMake(sequencerId, observingMode) }
                    .returns(CompletableFuture.completedFuture(sequencerAdminApi))

                onlineOfflineDsl.goOnlineModeForSequencer(sequencerId, observingMode)

                verify { sequencerAdminFactoryApi.jMake(sequencerId, observingMode) }
                verify { sequencerAdminApi.goOnline() }
            }
        }

        "goOfflineModeForSequencer should delegate to sequencerAdminApi.goOffline | ESW-236" {
            with(Mocks()) {

                every { sequencerAdminApi.goOffline() }
                    .answers { Future.successful(`Ok$`.`MODULE$`) }
                every { sequencerAdminFactoryApi.jMake(sequencerId, observingMode) }
                    .returns(CompletableFuture.completedFuture(sequencerAdminApi))

                onlineOfflineDsl.goOfflineModeForSequencer(sequencerId, observingMode)

                verify { sequencerAdminFactoryApi.jMake(sequencerId, observingMode) }
                verify { sequencerAdminApi.goOffline() }
            }
        }
    }
})
