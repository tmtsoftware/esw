package esw.ocs.script

import akka.actor.typed.{ActorSystem, SpawnProtocol}
import com.typesafe.config.Config
import csw.alarm.api.javadsl.IAlarmService
import csw.event.api.javadsl.IEventService
import csw.logging.api.javadsl.ILogger
import csw.prefix.models.{Prefix, Subsystem}
import esw.ocs.api.SequencerApi
import esw.ocs.api.models.{ObsMode, Variation}
import esw.ocs.dsl.script.ScriptDsl
import esw.ocs.impl.core.SequenceOperator
import esw.ocs.impl.script.{ScriptApi, ScriptContext, ScriptLoader}
import esw.ocs.testkit.utils.BaseTestSuite
import org.mockito.Mockito.when
import esw.ocs.impl.script.exceptions.ScriptLoadingException.{InvalidScriptException, ScriptNotFound}
import esw.ocs.impl.script.exceptions.ScriptInitialisationFailedException

import java.io.File
import java.time.Duration
import java.util.concurrent.CompletionStage
import scala.concurrent.duration.DurationInt
import scala.jdk.DurationConverters.ScalaDurationOps

class ScriptLoaderTest extends BaseTestSuite {

  private val actorSystem             = ActorSystem(SpawnProtocol(), "test-system")
  private val logger                  = mock[ILogger]
  private val sequenceOperatorFactory = () => mock[SequenceOperator]
  private val iEventService           = mock[IEventService]
  private val iAlarmService           = mock[IAlarmService]
  private val sequencerClientFactory  = mock[(Subsystem, ObsMode, Option[Variation]) => CompletionStage[SequencerApi]]
  private val prefix                  = Prefix("ESW.filter.wheel")
  private val config                  = mock[Config]
  private val heartbeatInterval       = Duration.ofSeconds(3)

  when(config.getConfig("csw-alarm")).thenReturn(config)
  when(config.getDuration("refresh-interval")).thenReturn(2.seconds.toJava)

  val scriptContext = new ScriptContext(
    heartbeatInterval,
    prefix,
    ObsMode.from(prefix),
    logger,
    sequenceOperatorFactory,
    actorSystem,
    iEventService,
    iAlarmService,
    sequencerClientFactory,
    config
  )

  // Gets the test script file for the given class name
  private def getScriptFile(scriptClass: String): File = {
    val scriptFile =
      new File(
        if (new File("examples").isDirectory) "examples" else "../examples",
        scriptClass
          .replace("esw.ocs.scripts.examples.testData", "testData")
          .replace(".", "/") + ".seq.kts"
      )
    scriptFile
  }

  "load" must {

    "load script class if subsystem and obsMode is provided | ESW-102, ESW-136" in {
      val scriptFile        = getScriptFile("esw.ocs.scripts.examples.testData.scriptLoader.ValidTestScript")
      val loader: ScriptApi = ScriptLoader.loadKotlinScript(scriptFile, scriptContext)
      loader shouldBe a[ScriptDsl]
    }

    "throw InvalidScriptException if provided class is not a script | ESW-102, ESW-136" in {
      val scriptFile = getScriptFile("esw.ocs.scripts.examples.testData.scriptLoader.InvalidTestScript")
      val exception = intercept[InvalidScriptException] {
        ScriptLoader.loadKotlinScript(scriptFile, scriptContext)
      }
      exception.getMessage shouldBe s"$scriptFile should contain a subclass of Script"
    }

    "throw ScriptNotFound if provided file does not exist at configured path | ESW-102, ESW-136" in {
      val invalidScriptClass = "invalid.path.TestScriptDoesNotExist"
      val scriptFile         = getScriptFile(invalidScriptClass)
      val exception = intercept[ScriptNotFound] {
        ScriptLoader.loadKotlinScript(scriptFile, scriptContext)
      }
      exception.getMessage shouldBe s"$scriptFile not found at configured path"
    }

    "throw ``ScriptInitialisationFailedException if script initialisation fails | ESW-102, ESW-136, ESW-243" in {
      val exception = intercept[ScriptInitialisationFailedException] {
        val scriptFile = getScriptFile("esw.ocs.scripts.examples.testData.scriptLoader.InitialisationExceptionTestScript")
        ScriptLoader.loadKotlinScript(scriptFile, scriptContext)
      }
      exception.getMessage shouldBe "Script initialization failed with : initialisation failed"
    }
  }
}
