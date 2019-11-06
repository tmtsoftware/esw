package esw.ocs.dsl.highlevel

import akka.actor.typed.ActorSystem
import akka.actor.typed.javadsl.Behaviors
import akka.stream.Materializer
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
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle
import java.util.*
import java.util.concurrent.CompletableFuture

@TestInstance(Lifecycle.PER_CLASS)
class ConfigServiceDslTest : ConfigServiceDsl {

    val actorSystem: ActorSystem<Any> = ActorSystem.create(Behaviors.empty(), "config-dsl")
    override val materializer: Materializer = Materializer.createMaterializer(actorSystem)

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

    //Todo: This test might work on removing `private[config]` restriction from `toJStringF()` api from CSW `ConfigData` class`
    @Test
    fun `getConfig should delegate to configClientService#getActive and return ConfigData | ESW-123`() = runBlocking {
        val defaultStrConf = "foo { bar { baz : 1234 } }"
        val configData = ConfigData.fromString(defaultStrConf)
        every { configClient.getActive(any()) }.returns(CompletableFuture.completedFuture(Optional.of(configData)))
        getConfig(path) shouldBe ConfigFactory.parseString(defaultStrConf)
        verify { configClient.getActive(any()) }
    }

}