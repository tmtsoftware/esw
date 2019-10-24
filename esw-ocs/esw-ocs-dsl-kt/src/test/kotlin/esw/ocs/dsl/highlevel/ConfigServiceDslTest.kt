package esw.ocs.dsl.highlevel

import akka.stream.Materializer
import csw.config.api.ConfigData
import csw.config.api.javadsl.IConfigClientService
import esw.ocs.dsl.nullable
import io.kotlintest.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import java.util.*
import java.util.concurrent.CompletableFuture

class ConfigServiceDslTest : ConfigServiceDsl {

    override val materializer: Materializer = mockk()

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
//    @Test
//    fun `getConfig should delegate to configClientService#getActive and return ConfigData | ESW-123`() = runBlocking {
//        val defaultStrConf = "foo { bar { baz : 1234 } }"
//        val configData = ConfigData.fromString(defaultStrConf)
//        val optionalConfigData = Optional.of(configData)
//        every { configClient.getActive(any()) }.returns(CompletableFuture.completedFuture(optionalConfigData))
//        every { optionalConfigData.nullable()?.toJStringF(materializer) }.returns(CompletableFuture.completedFuture(defaultStrConf))
//        getConfig(path) shouldBe defaultStrConf
//        verify { configClient.getActive(any()) }
//    }


}