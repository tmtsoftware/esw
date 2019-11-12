package esw.ocs.dsl.highlevel

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

class StopDslTest : StopDsl {

    private val sequencerId = "testSequencer"
    private val observingMode = "DarkNight"

    private val locationServiceUtil: LocationServiceUtil = mockk()
    private val sequencerAdminApi: SequencerAdminApi = mockk()
    private val sequencerAdminFactoryApi: SequencerAdminFactoryApi = mockk()

    override val commonUtils: CommonUtils = CommonUtils(sequencerAdminFactoryApi, locationServiceUtil, mockk(), mockk(), mockk())

    @Test
    fun `stop should delegate to sequencerAdminApi#stop | ESW-156, ESW-138`() = runBlocking {

        // return value gets discarded
        every { sequencerAdminApi.stop() }
                .answers { Future.successful(`Ok$`.`MODULE$`) }

        every { sequencerAdminFactoryApi.jMake(sequencerId, observingMode) }
                .returns(CompletableFuture.completedFuture(sequencerAdminApi))

        stopSequencer(sequencerId, observingMode)

        verify { sequencerAdminFactoryApi.jMake(sequencerId, observingMode) }
        verify { sequencerAdminApi.stop() }
    }
}
