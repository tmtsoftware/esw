package esw.ocs.dsl.highlevel

import akka.actor.typed.ActorSystem
import akka.actor.typed.SpawnProtocol
import com.typesafe.config.Config
import csw.config.api.javadsl.IConfigClientService
import csw.config.models.ConfigId
import esw.ocs.dsl.nullable
import kotlinx.coroutines.future.await
import java.nio.file.Path

interface ConfigServiceDsl {

    val configService: IConfigClientService
    val actorSystem: ActorSystem<SpawnProtocol.Command>

    /**
     * Checks if configuration file exists at provided path
     *
     * @param path relative configuration file path
     * @param id optional revision of the file
     * @return true if the file exists, false otherwise
     * @throws [csw.config.api.exceptions.InvalidInput] | [csw.config.api.exceptions.FileNotFound]
     */
    suspend fun existsConfig(path: String, id: String? = null): Boolean =
            id?.let { configService.exists(Path.of(path), ConfigId(id)).await() }
                    ?: configService.exists(Path.of(path)).await()

    /**
     * Retrieves active configuration file contents present at provided path
     *
     * @param path relative configuration file path
     * @return file content as [Config] object if file exists, otherwise returns null
     * @throws [csw.config.api.exceptions.EmptyResponse] | [csw.config.api.exceptions.InvalidInput] | [csw.config.api.exceptions.FileNotFound]
     */
    suspend fun getConfig(path: String): Config? {
        val configData = configService.getActive(Path.of(path)).await().nullable()
        return configData?.toJConfigObject(actorSystem)?.await()
    }
}