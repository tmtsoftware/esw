package esw.ocs.dsl.highlevel

import akka.actor.typed.ActorSystem
import akka.actor.typed.SpawnProtocol
import akka.actor.typed.javadsl.Behaviors
import com.typesafe.config.ConfigFactory
import csw.config.api.ConfigData
import csw.config.api.javadsl.IConfigClientService
import io.kotlintest.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Test
import java.util.*
import java.util.concurrent.CompletableFuture

class ConfigServiceDslTest : ConfigServiceDsl {

    override val actorSystem: ActorSystem<SpawnProtocol.Command> = ActorSystem.create(Behaviors.empty(), "config-dsl")

    @AfterAll
    fun tearDown() = actorSystem.terminate()

    override val configClient: IConfigClientService = mockk()
    private val path = "/test/config1.conf"

    @Test
    fun `existsConfig should delegate to configClientService#exists | ESW-123`() = runBlocking {
        every { configClient.exists(any()) }.returns(CompletableFuture.completedFuture(true))
        existsConfig(path)
        verify { configClient.exists(any()) }
    }

    @Test
    fun `getConfig should delegate to configClientService#getActive and return null if ConfigData is empty | ESW-123`() = runBlocking {
        every { configClient.getActive(any()) }.returns(CompletableFuture.completedFuture(Optional.empty()))
        getConfig(path) shouldBe null
        verify { configClient.getActive(any()) }
    }

    @Test
    fun `getConfig should delegate to configClientService#getActive and return ConfigData | ESW-123`() = runBlocking {
        val defaultStrConf = "foo { bar { baz : 1234 } }"
        val configData = ConfigData.fromString(defaultStrConf)
        every { configClient.getActive(any()) }.returns(CompletableFuture.completedFuture(Optional.of(configData)))
        getConfig(path) shouldBe ConfigFactory.parseString(defaultStrConf)
        verify { configClient.getActive(any()) }
    }

}