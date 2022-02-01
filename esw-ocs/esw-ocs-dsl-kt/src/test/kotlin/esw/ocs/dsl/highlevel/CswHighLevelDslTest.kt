package esw.ocs.dsl.highlevel

import akka.actor.typed.ActorSystem
import akka.actor.typed.SpawnProtocol
import com.typesafe.config.Config
import csw.location.api.javadsl.ILocationService
import csw.location.api.scaladsl.LocationService
import csw.params.events.SequencerObserveEvent
import esw.ocs.api.models.ObsMode
import esw.ocs.dsl.highlevel.models.Assembly
import esw.ocs.dsl.highlevel.models.HCD
import esw.ocs.dsl.highlevel.models.Prefix
import esw.ocs.dsl.highlevel.models.TCS
import esw.ocs.dsl.lowlevel.CswServices
import esw.ocs.dsl.script.StrandEc
import esw.ocs.impl.script.ScriptContext
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

class CswHighLevelDslTest {

    private val system: ActorSystem<SpawnProtocol.Command> = ActorSystem.create(SpawnProtocol.create(), "csw-high-level")

    private val config: Config = mockk()
    private val alarmConfig: Config = mockk()
    private val cswServices: CswServices = mockk()
    private val locationService: LocationService = mockk()
    private val iLocationService: ILocationService = mockk()
    private val scriptContext = ScriptContext(mockk(), Prefix("TCS.filter.wheel"), mockk(), mockk(), mockk(), system, mockk(), mockk(), mockk(), config)

    init {
        every { config.getConfig("csw-alarm") }.returns(alarmConfig)
        every { alarmConfig.getDuration("refresh-interval") }.returns(2.seconds.toJavaDuration())
        every { cswServices.locationService }.returns(iLocationService)
        every { iLocationService.asScala() }.returns(locationService)
    }

    @AfterAll
    fun tearDown() = system.terminate()

    @Nested
    inner class Script : CswHighLevelDsl(cswServices, scriptContext) {
        override val strandEc: StrandEc = mockk()
        override val coroutineScope: CoroutineScope = mockk()
        override val isOnline: Boolean get() = true
        override val prefix: String = scriptContext.prefix().toString()
        override val obsMode: ObsMode = scriptContext.obsMode()
        override val sequencerObserveEvent: SequencerObserveEvent = SequencerObserveEvent(Prefix(prefix))
        override val actorSystem: ActorSystem<SpawnProtocol.Command> = system

        private val defaultTimeoutDuration: Duration = 5.seconds

        @Test
        fun `Assembly should resolve the RichComponent with given name and assembly component type | ESW-245`() = runBlocking {
            val sampleAssembly = Assembly(TCS, "sampleAssembly", defaultTimeoutDuration)

            sampleAssembly.componentType shouldBe Assembly
            sampleAssembly.prefix shouldBe Prefix("TCS.sampleAssembly")
        }

        @Test
        fun `HCD should resolve the RichComponent with given name and hcd component type | ESW-245`() = runBlocking {
            val sampleHcd = Hcd(TCS, "sampleHcd", defaultTimeoutDuration)

            sampleHcd.componentType shouldBe HCD
            sampleHcd.prefix shouldBe Prefix("TCS.sampleHcd")
        }
    }
}
