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

class OnlineOfflineDslTest : WordSpec(), OnlineOfflineDsl {

    private val componentName = "testComponent1"
    private val sequencerId = "testSequencer"
    private val observingMode = "DarkNight"
    private val componentType: ComponentType = JComponentType.HCD

    private val locationServiceUtil: LocationServiceUtil = mockk()
    private val sequencerAdminApi: SequencerAdminApi = mockk()
    private val sequencerAdminFactoryApi: SequencerAdminFactoryApi = mockk()
    private val componentRef: ActorRef<ComponentMessage> = mockk()

    override val commonUtils: CommonUtils = CommonUtils(sequencerAdminFactoryApi, locationServiceUtil)

    init {
        "OnlineOfflineDsl" should {
            "goOnlineModeForComponent should resolve component ref and send GoOnline msg | ESW-236" {
                val goOnlineMsg = Lifecycle(`GoOnline$`.`MODULE$`)

                every { componentRef.tell(goOnlineMsg) }.answers { Unit }
                every { locationServiceUtil.jResolveComponentRef(componentName, componentType) }
                    .answers { CompletableFuture.completedFuture(componentRef) }

                goOnlineModeForComponent(componentName, componentType)

                verify { locationServiceUtil.jResolveComponentRef(componentName, componentType) }
                verify { componentRef.tell(goOnlineMsg) }
            }

            "goOfflineModeForComponent should resolve component ref and send GoOffline msg | ESW-236" {
                val goOfflineMsg = Lifecycle(`GoOffline$`.`MODULE$`)

                every { componentRef.tell(goOfflineMsg) }.answers { Unit }
                every { locationServiceUtil.jResolveComponentRef(componentName, componentType) }
                    .answers { CompletableFuture.completedFuture(componentRef) }

                goOfflineModeForComponent(componentName, componentType)

                verify { locationServiceUtil.jResolveComponentRef(componentName, componentType) }
                verify { componentRef.tell(goOfflineMsg) }
            }

            "goOnlineModeForSequencer should delegate to sequencerAdminApi.goOnline | ESW-236" {

                // return value gets discarded
                every { sequencerAdminApi.goOnline() }
                    .answers { Future.successful(`Ok$`.`MODULE$`) }

                every { sequencerAdminFactoryApi.jMake(sequencerId, observingMode) }
                    .returns(CompletableFuture.completedFuture(sequencerAdminApi))

                goOnlineModeForSequencer(sequencerId, observingMode)

                verify { sequencerAdminFactoryApi.jMake(sequencerId, observingMode) }
                verify { sequencerAdminApi.goOnline() }
            }

            "goOfflineModeForSequencer should delegate to sequencerAdminApi.goOffline | ESW-236" {

                every { sequencerAdminApi.goOffline() }
                    .answers { Future.successful(`Ok$`.`MODULE$`) }
                every { sequencerAdminFactoryApi.jMake(sequencerId, observingMode) }
                    .returns(CompletableFuture.completedFuture(sequencerAdminApi))

                goOfflineModeForSequencer(sequencerId, observingMode)

                verify { sequencerAdminFactoryApi.jMake(sequencerId, observingMode) }
                verify { sequencerAdminApi.goOffline() }
            }
        }
    }
}
