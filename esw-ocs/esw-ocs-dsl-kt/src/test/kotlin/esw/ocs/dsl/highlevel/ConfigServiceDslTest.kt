package esw.ocs.dsl.highlevel

import csw.config.api.ConfigData
import csw.config.api.javadsl.IConfigClientService
import io.kotlintest.shouldBe
import io.kotlintest.shouldThrow
import io.kotlintest.shouldThrowExactly
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import java.nio.file.InvalidPathException
import java.nio.file.Path
import java.util.*
import java.util.concurrent.CompletableFuture

class ConfigServiceDslTest : ConfigServiceDsl {

    override val configClientService: IConfigClientService = mockk()
    private val path = "/test/config1.conf"


    @Test
    fun `existsConfig should delegate to configClientService#exists | ESW-123`() = runBlocking {
        every { configClientService.exists(any()) }.returns(CompletableFuture.completedFuture(true))
        existsConfig(path)
        verify { configClientService.exists(any()) }
    }

    @Test
    fun `getConfig should delegate to configClientService#getActive and return null if ConfigData is empty | ESW-123`() = runBlocking {
        every { configClientService.getActive(any()) }.returns(CompletableFuture.completedFuture(Optional.empty()))
        getConfig(path) shouldBe null
        verify { configClientService.getActive(any()) }
    }

    @Test
    fun `getConfig should delegate to configClientService#getActive and return ConfigData | ESW-123`() = runBlocking {
        val configData = ConfigData.fromBytes(ByteArray(1))
        every { configClientService.getActive(any()) }.returns(CompletableFuture.completedFuture(Optional.of(configData)))
        getConfig(path) shouldBe configData
        verify { configClientService.getActive(any()) }
    }




}