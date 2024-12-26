package esw.ocs.dsl.highlevel

import org.apache.pekko.actor.typed.ActorSystem
import org.apache.pekko.actor.typed.SpawnProtocol
import org.apache.pekko.actor.typed.javadsl.Behaviors
import com.typesafe.config.ConfigFactory
import csw.config.api.ConfigData
import csw.config.api.javadsl.IConfigClientService
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Test
import java.util.*
import java.util.concurrent.CompletableFuture

@Suppress("DANGEROUS_CHARACTERS")
class ConfigServiceDslTest : ConfigServiceDsl {

    override val actorSystem: ActorSystem<SpawnProtocol.Command> = ActorSystem.create(Behaviors.empty(), "config-dsl")

    @AfterAll
    fun tearDown() = actorSystem.terminate()

    override val configService: IConfigClientService = mockk()
    private val path = "/test/config1.conf"

    @Test
    fun `existsConfig_should_delegate_to_configClientService#exists_|_ESW-123`() = runBlocking {
        every { configService.exists(any()) }.returns(CompletableFuture.completedFuture(true))
        existsConfig(path)
        verify { configService.exists(any()) }
    }

    @Test
    fun `getConfig_should_delegate_to_configClientService#getActive_and_return_null_if_ConfigData_is_empty_|_ESW-123`() = runBlocking {
        every { configService.getActive(any()) }.returns(CompletableFuture.completedFuture(Optional.empty()))
        getConfig(path) shouldBe null
        verify { configService.getActive(any()) }
    }

    @Test
    fun `getConfig_should_delegate_to_configClientService#getActive_and_return_ConfigData_|_ESW-123`() = runBlocking {
        val defaultStrConf = "foo { bar { baz : 1234 } }"
        val configData = ConfigData.fromString(defaultStrConf)
        every { configService.getActive(any()) }.returns(CompletableFuture.completedFuture(Optional.of(configData)))
        getConfig(path) shouldBe ConfigFactory.parseString(defaultStrConf)
        verify { configService.getActive(any()) }
    }

}
