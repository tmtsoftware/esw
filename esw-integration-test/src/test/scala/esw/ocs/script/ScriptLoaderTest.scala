package esw.ocs.script

import akka.actor.typed.{ActorSystem, SpawnProtocol}
import com.typesafe.config.Config
import csw.alarm.api.javadsl.IAlarmService
import csw.event.api.javadsl.IEventService
import csw.logging.api.javadsl.ILogger
import csw.prefix.models.Prefix
import esw.ocs.dsl.script.ScriptDsl
import esw.ocs.dsl.script.exceptions.ScriptInitialisationFailedException
import esw.ocs.impl.SequencerActorProxyFactory
import esw.ocs.impl.core.api.SequenceOperator
import esw.ocs.impl.core.script.ScriptLoadingException.{InvalidScriptException, ScriptNotFound}
import esw.ocs.impl.core.script.{ScriptApi, ScriptContext, ScriptLoader}
import esw.ocs.testkit.BaseTestSuite

import scala.concurrent.duration.DurationInt
import scala.jdk.DurationConverters.ScalaDurationOps

class ScriptLoaderTest extends BaseTestSuite {

  private val actorSystem             = ActorSystem(SpawnProtocol(), "test-system")
  private val logger                  = mock[ILogger]
  private val sequenceOperatorFactory = () => mock[SequenceOperator]
  private val iEventService           = mock[IEventService]
  private val iAlarmService           = mock[IAlarmService]
  private val sequencerClientFactory  = mock[SequencerActorProxyFactory]
  private val prefix                  = mock[Prefix]
  private val config                  = mock[Config]

  when(config.getConfig("csw-alarm")).thenReturn(config)
  when(config.getDuration("refresh-interval")).thenReturn(2.seconds.toJava)

  val scriptContext = new ScriptContext(
    prefix,
    logger,
    sequenceOperatorFactory,
    actorSystem,
    iEventService,
    iAlarmService,
    sequencerClientFactory,
    config
  )

  "load" must {

    "load script class if subsystem and observingMode is provided | ESW-102, ESW-136" in {
      val loader: ScriptApi =
        ScriptLoader.loadKotlinScript("esw.ocs.scripts.examples.testData.scriptLoader.ValidTestScript", scriptContext)
      loader shouldBe a[ScriptDsl]
    }

    "throw InvalidScriptException if provided class is not a script | ESW-102, ESW-136" in {
      val exception = intercept[InvalidScriptException] {
        ScriptLoader.loadKotlinScript("esw.ocs.scripts.examples.testData.scriptLoader.InvalidTestScript", scriptContext)
      }

      exception.getMessage shouldBe s"esw.ocs.scripts.examples.testData.scriptLoader.InvalidTestScript should be subclass of Script"
    }

    "throw ScriptNotFound if provided class does not exist at configured path | ESW-102, ESW-136" in {
      val invalidScriptClass = "invalid.path.TestScriptDoesNotExist"

      val exception = intercept[ScriptNotFound] {
        ScriptLoader.loadKotlinScript(invalidScriptClass, scriptContext)
      }

      exception.getMessage shouldBe "invalid.path.TestScriptDoesNotExist not found at configured path"
    }

    "throw ScriptInitialisationFailedException if script initialisation fails | ESW-102, ESW-136, ESW-243" in {
      val exception = intercept[ScriptInitialisationFailedException] {
        ScriptLoader.loadKotlinScript(
          "esw.ocs.scripts.examples.testData.scriptLoader.InitialisationExceptionTestScript",
          scriptContext
        )
      }
      exception.getMessage shouldBe "Script initialization failed with : initialisation failed"
    }
  }
}
