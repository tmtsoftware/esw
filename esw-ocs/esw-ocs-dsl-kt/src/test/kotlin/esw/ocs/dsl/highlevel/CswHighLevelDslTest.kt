package esw.ocs.dsl.highlevel

import akka.actor.typed.ActorSystem
import akka.actor.typed.javadsl.Behaviors
import csw.database.DatabaseServiceFactory
import csw.location.api.javadsl.ILocationService
import csw.location.api.javadsl.JComponentType
import csw.location.api.scaladsl.LocationService
import csw.logging.api.javadsl.ILogger
import csw.params.core.models.Prefix
import esw.ocs.dsl.script.CswServices
import esw.ocs.dsl.script.StrandEc
import esw.ocs.dsl.script.utils.LockUnlockUtil
import io.kotlintest.shouldBe
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class CswHighLevelDslTest {

    private val actorSystem: ActorSystem<Any> = ActorSystem.create(Behaviors.empty(), "csw-high-level")

    private val cswServices: CswServices = mockk()
    private val jLocationService: ILocationService = mockk()
    private val locationService: LocationService = mockk()
    private val lockUnlockUtil: LockUnlockUtil = mockk()
    private val databaseServiceFactory: DatabaseServiceFactory = mockk()
    private val jLogger: ILogger = mockk()
    private val prefix: Prefix = mockk()

    init {
        every { cswServices.actorSystem() }.answers { actorSystem }
        every { cswServices.lockUnlockUtil() }.answers { lockUnlockUtil }
        every { cswServices.locationService() }.answers { jLocationService }
        every { cswServices.databaseServiceFactory() }.answers { databaseServiceFactory }
        every { cswServices.jLogger() }.answers { jLogger }
        every { cswServices.prefix() }.answers { prefix }
        every { jLocationService.asScala() }.answers { locationService }
    }

    @AfterAll
    fun tearDown() = actorSystem.terminate()

    @Nested
    inner class Script : CswHighLevelDsl(cswServices) {
        override val strandEc: StrandEc = mockk()
        override val coroutineScope: CoroutineScope = mockk()

        @Test
        fun `Assembly should resolve the RichComponent with given name and assembly component type | ESW-245`() = runBlocking {
            val sampleAssembly = Assembly("sampleAssembly")

            sampleAssembly.componentType shouldBe JComponentType.Assembly()
            sampleAssembly.name shouldBe "sampleAssembly"
        }

        @Test
        fun `HCD should resolve the RichComponent with given name and hcd component type | ESW-245`() = runBlocking {
            val sampleHcd = HCD("sampleHcd")

            sampleHcd.componentType shouldBe JComponentType.HCD()
            sampleHcd.name shouldBe "sampleHcd"
        }
    }

}