package esw.ocs.dsl.highlevel

import csw.config.api.ConfigData
import csw.config.api.javadsl.IConfigClientService
import csw.config.models.ConfigId
import esw.ocs.dsl.nullable
import kotlinx.coroutines.future.await
import java.nio.file.Path

interface ConfigServiceDsl {

    val configClientService: IConfigClientService

    suspend fun existsConfig(path: String, id: String? = null): Boolean =
            id?.let { configClientService.exists(Path.of(path), ConfigId(id)).await() }
                    ?: configClientService.exists(Path.of(path)).await()


    suspend fun getConfig(path: String): ConfigData? = configClientService.getActive(Path.of(path)).await().nullable()
}