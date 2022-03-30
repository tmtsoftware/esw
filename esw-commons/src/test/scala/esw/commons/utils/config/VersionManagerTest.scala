/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package esw.commons.utils.config

import java.nio.file.Path

import akka.actor.testkit.typed.scaladsl.ActorTestKit
import akka.actor.typed.ActorSystem
import com.typesafe.config.{Config, ConfigException, ConfigOrigin}
import csw.config.api.exceptions.FileNotFound
import csw.config.client.commons.ConfigUtils
import esw.testcommons.BaseTestSuite
import org.scalatest.prop.Tables.Table

import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, ExecutionContextExecutor, Future}
import org.mockito.Mockito.when
class VersionManagerTest extends BaseTestSuite {
  val actorTestKit: ActorTestKit            = ActorTestKit()
  val actorSystem: ActorSystem[_]           = actorTestKit.system
  implicit val ec: ExecutionContextExecutor = actorSystem.executionContext

  private val runtimeErrorStr = randomString(20)
  "findVersion" must {
    val errorMsg        = randomString(10)
    val versionConfPath = Path.of(randomString(10))

    "return scripts version if config is present | ESW-360" in {
      val configUtils    = mock[ConfigUtils]
      val versionManager = new VersionManager(versionConfPath, configUtils)
      val config         = mock[Config]
      val version        = randomString(10)

      when(configUtils.getConfig(versionConfPath, isLocal = false)).thenReturn(Future.successful(config))
      when(config.getString("scripts")).thenReturn(version)
      versionManager.getScriptVersion.futureValue should ===(version)
    }

    "return esw version if config is present | ESW-360" in {
      val configUtils    = mock[ConfigUtils]
      val versionManager = new VersionManager(versionConfPath, configUtils)
      val config         = mock[Config]
      val version        = randomString(10)

      when(configUtils.getConfig(versionConfPath, isLocal = false)).thenReturn(Future.successful(config))
      when(config.getString("esw")).thenReturn(version)
      versionManager.eswVersion.futureValue should ===(version)
    }

    Table(
      ("exception", "expectedMsg"),
      (FileNotFound(errorMsg), errorMsg),
      (new ConfigException.Missing(versionConfPath.toString), "scripts is not present"),
      (new ConfigException.WrongType(mock[ConfigOrigin], versionConfPath.toString), "value of scripts is not string"),
      (new RuntimeException(runtimeErrorStr), s"Failed to fetch scripts version: $runtimeErrorStr")
    ).foreach { case (exception, msg) =>
      s"throw ScriptVersionConfException if ${exception.getClass.getSimpleName} | ESW-360" in {
        val configUtils    = mock[ConfigUtils]
        val versionManager = new VersionManager(versionConfPath, configUtils)

        when(configUtils.getConfig(versionConfPath, isLocal = false)).thenReturn(Future.failed(exception))

        val scriptVersionConfException = intercept[FetchingScriptVersionFailed] {
          Await.result(versionManager.getScriptVersion, 100.millis)
        }
        scriptVersionConfException should ===(FetchingScriptVersionFailed(msg))
      }
    }

    Table(
      ("exception", "expectedMsg"),
      (FileNotFound(errorMsg), errorMsg),
      (new ConfigException.Missing(versionConfPath.toString), "esw is not present"),
      (new ConfigException.WrongType(mock[ConfigOrigin], versionConfPath.toString), "value of esw is not string"),
      (new RuntimeException(runtimeErrorStr), s"Failed to fetch esw version: $runtimeErrorStr")
    ).foreach { case (exception, msg) =>
      s"throw eswVersionConfException if ${exception.getClass.getSimpleName} | ESW-360, ESW-445" in {
        val configUtils    = mock[ConfigUtils]
        val versionManager = new VersionManager(versionConfPath, configUtils)

        when(configUtils.getConfig(versionConfPath, isLocal = false)).thenReturn(Future.failed(exception))

        val scriptVersionConfException = intercept[FetchingScriptVersionFailed] {
          Await.result(versionManager.eswVersion, 100.millis)
        }
        scriptVersionConfException should ===(FetchingScriptVersionFailed(msg))
      }
    }
  }

  override protected def afterAll(): Unit = {
    actorTestKit.shutdownTestKit()
    super.afterAll()
  }
}
