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

    val configClient: IConfigClientService
    val actorSystem: ActorSystem<SpawnProtocol.Command>

    suspend fun existsConfig(path: String, id: String? = null): Boolean =
            id?.let { configClient.exists(Path.of(path), ConfigId(id)).await() }
                    ?: configClient.exists(Path.of(path)).await()

    suspend fun getConfig(path: String): Config? {
        val configData = configClient.getActive(Path.of(path)).await().nullable()
        return configData?.toJConfigObject(actorSystem)?.await()
    }
}