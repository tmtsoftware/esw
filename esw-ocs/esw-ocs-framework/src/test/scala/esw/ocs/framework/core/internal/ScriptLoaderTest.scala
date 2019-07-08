package esw.ocs.framework.core.internal

import akka.actor.typed.{ActorSystem, SpawnProtocol}
import esw.ocs.framework.BaseTestSuite
import esw.ocs.framework.dsl.CswServices
import esw.ocs.framework.exceptions.ScriptLoadingException._
import org.mockito.MockitoSugar.mock

class ScriptLoaderTest extends BaseTestSuite {
  private implicit val actorSystem: ActorSystem[SpawnProtocol] = ActorSystem(SpawnProtocol.behavior, "test-ac")

  private val cswServices = mock[CswServices]

  "ScriptLoader" must {
    "load script class if sequencerId and observingMode is provided | ESW-102" in {
      val loader = new ScriptLoader("testSequencerId1", "testObservingMode1").load(cswServices)
      assert(loader.isInstanceOf[ValidTestScript])
    }

    "throw ScriptConfigurationMissingException if script config is not provided for given sequencerId and observingMode | ESW-102" in {
      val sequencerId   = "invalidSequencerId"
      val observingMode = "invalidObservingMode"

      val exception = intercept[ScriptConfigurationMissingException] {
        new ScriptLoader(sequencerId, observingMode).load(cswServices)
      }

      exception.getMessage shouldBe s"Script configuration missing for $sequencerId with $observingMode"
    }

    "throw InvalidScriptException if provided class is not a script | ESW-102" in {
      val sequencerId   = "testSequencerId2"
      val observingMode = "testObservingMode2"

      val exception = intercept[InvalidScriptException] {
        new ScriptLoader(sequencerId, observingMode).load(cswServices)
      }

      val invalidTestScript = new InvalidTestScript(cswServices).getClass.getCanonicalName

      exception.getMessage shouldBe s"$invalidTestScript should be subclass of Script"
    }

    "throw ScriptNotFound if provided class is not a does not exist at configured path | ESW-102" in {
      val sequencerId   = "testSequencerId3"
      val observingMode = "testObservingMode3"

      val exception = intercept[ScriptNotFound] {
        new ScriptLoader(sequencerId, observingMode).load(cswServices)
      }

      exception.getMessage shouldBe "invalid.path.TestScriptDoesNotExist not found at configured path"
    }
  }
}
