package esw.ocs.impl.internal

import esw.dsl.script.CswServices
import esw.ocs.api.BaseTestSuite
import esw.dsl.script.utils.ScriptLoader
import esw.dsl.script.exceptions.ScriptLoadingException._

class ScriptLoaderTest extends BaseTestSuite {

  private val cswServices = mock[CswServices]

  "load" must {
    "load script class if sequencerId and observingMode is provided | ESW-102" in {
      val scriptClass = classOf[ValidTestScript].getCanonicalName

      val loader = ScriptLoader.load(scriptClass, cswServices)
      loader shouldBe a[ValidTestScript]
    }

    "throw InvalidScriptException if provided class is not a script | ESW-102" in {
      val scriptClass = classOf[InvalidTestScript].getCanonicalName

      val exception = intercept[InvalidScriptException] {
        ScriptLoader.load(scriptClass, cswServices)
      }

      val invalidTestScript = new InvalidTestScript(cswServices).getClass.getCanonicalName
      exception.getMessage shouldBe s"$invalidTestScript should be subclass of Script"
    }

    "throw ScriptNotFound if provided class does not exist at configured path | ESW-102" in {
      val invalidScriptClass = "invalid.path.TestScriptDoesNotExist"

      val exception = intercept[ScriptNotFound] {
        ScriptLoader.load(invalidScriptClass, cswServices)
      }

      exception.getMessage shouldBe "invalid.path.TestScriptDoesNotExist not found at configured path"
    }
  }
}
