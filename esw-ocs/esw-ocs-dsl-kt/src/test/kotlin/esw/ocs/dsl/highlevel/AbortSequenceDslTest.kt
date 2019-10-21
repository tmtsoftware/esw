package esw.ocs.dsl.highlevel

import akka.actor.typed.ActorRef
import csw.command.client.messages.ComponentMessage
import csw.location.api.javadsl.JComponentType
import csw.location.models.ComponentType
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

class AbortSequenceDslTest : AbortSequenceDsl {

    private val sequencerId = "testSequencer"
    private val observingMode = "DarkNight"

    private val locationServiceUtil: LocationServiceUtil = mockk()
    private val sequencerAdminApi: SequencerAdminApi = mockk()
    private val sequencerAdminFactoryApi: SequencerAdminFactoryApi = mockk()

    override val commonUtils: CommonUtils = CommonUtils(sequencerAdminFactoryApi, locationServiceUtil)

    @Test
    fun `abortSequenceForSequencer should delegate to sequencerAdminApi#abortSequence | ESW-155, ESW-137`() = runBlocking {

        // return value gets discarded
        every { sequencerAdminApi.abortSequence() }
                .answers { Future.successful(`Ok$`.`MODULE$`) }

        every { sequencerAdminFactoryApi.jMake(sequencerId, observingMode) }
                .returns(CompletableFuture.completedFuture(sequencerAdminApi))

        abortSequenceForSequencer(sequencerId, observingMode)

        verify { sequencerAdminFactoryApi.jMake(sequencerId, observingMode) }
        verify { sequencerAdminApi.abortSequence() }
    }
}
