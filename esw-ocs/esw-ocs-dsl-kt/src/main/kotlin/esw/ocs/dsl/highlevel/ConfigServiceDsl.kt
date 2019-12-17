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

    suspend fun existsConfig(path: String, id: String? = null): Boolean =
            id?.let { configService.exists(Path.of(path), ConfigId(id)).await() }
                    ?: configService.exists(Path.of(path)).await()

    suspend fun getConfig(path: String): Config? {
        val configData = configService.getActive(Path.of(path)).await().nullable()
        return configData?.toJConfigObject(actorSystem)?.await()
    }
}