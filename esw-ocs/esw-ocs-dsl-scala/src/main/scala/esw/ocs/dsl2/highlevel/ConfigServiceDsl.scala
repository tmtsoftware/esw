package esw.ocs.dsl2.highlevel

import akka.actor.typed.ActorSystem
import com.typesafe.config.Config
import csw.config.api.ConfigData
import csw.config.api.scaladsl.{ConfigClientService, ConfigService}
import csw.config.models.ConfigId

import java.nio.file.Path
import async.Async.*
import scala.concurrent.{ExecutionContext, Future}

class ConfigServiceDsl(configService: ConfigClientService)(using ActorSystem[_], ExecutionContext) {
  inline def existsConfig(path: String, id: String = null): Boolean =
    await(configService.exists(Path.of(path), Option(id).map(ConfigId.apply)))

  inline def getConfig(path: String): Option[Config] = {
    val maybeConfigData = await(configService.getActive(Path.of(path)))
    maybeConfigData.map(data => await(data.toConfigObject))
  }
}
