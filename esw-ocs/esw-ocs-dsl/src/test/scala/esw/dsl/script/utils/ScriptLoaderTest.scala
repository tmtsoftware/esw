package esw.dsl.script.utils

import esw.dsl.script.CswServices
import esw.dsl.script.exceptions.ScriptLoadingException._
import esw.ocs.api.BaseTestSuite

class ScriptLoaderTest extends BaseTestSuite {

  private val cswServices = mock[CswServices]

  "load" must {
    "load script class if packageId and observingMode is provided | ESW-102" in {
      val scriptClass = classOf[ValidTestScript].getCanonicalName

      val loader = ScriptLoader.loadClass(scriptClass, cswServices)
      loader shouldBe a[ValidTestScript]
    }

    "throw InvalidScriptException if provided class is not a script | ESW-102" in {
      val scriptClass = classOf[InvalidTestScript].getCanonicalName

      val exception = intercept[InvalidScriptException] {
        ScriptLoader.loadClass(scriptClass, cswServices)
      }

      val invalidTestScript = new InvalidTestScript(cswServices).getClass.getCanonicalName
      exception.getMessage shouldBe s"$invalidTestScript should be subclass of Script"
    }

    "throw ScriptNotFound if provided class does not exist at configured path | ESW-102" in {
      val invalidScriptClass = "invalid.path.TestScriptDoesNotExist"

      val exception = intercept[ScriptNotFound] {
        ScriptLoader.loadClass(invalidScriptClass, cswServices)
      }

      exception.getMessage shouldBe "invalid.path.TestScriptDoesNotExist not found at configured path"
    }
  }
}
