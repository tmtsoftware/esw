package esw.ocs.script

import akka.actor.typed.{ActorSystem, SpawnProtocol}
import csw.alarm.api.javadsl.IAlarmService
import csw.config.api.javadsl.IConfigClientService
import csw.event.api.javadsl.IEventService
import csw.location.api.javadsl.ILocationService
import csw.time.scheduler.TimeServiceSchedulerFactory
import esw.ocs.api.{BaseTestSuite, SequencerAdminFactoryApi, SequencerCommandFactoryApi}
import esw.ocs.dsl.script.exceptions.ScriptLoadingException.{
  InvalidScriptException,
  ScriptInitialisationFailedException,
  ScriptNotFound
}
import esw.ocs.dsl.script.utils.{LockUnlockUtil, ScriptLoader}
import esw.ocs.dsl.script.{CswServices, JScriptDsl, SequenceOperator}

class ScriptLoaderTest extends BaseTestSuite {

  private val actorSystem                 = ActorSystem(SpawnProtocol(), "test-system")
  private val sequenceOperator            = mock[SequenceOperator]
  private val iLocationService            = mock[ILocationService]
  private val iEventService               = mock[IEventService]
  private val timeServiceSchedulerFactory = mock[TimeServiceSchedulerFactory]
  private val sequencerAdminFactoryApi    = mock[SequencerAdminFactoryApi]
  private val sequencerCommandFactoryApi  = mock[SequencerCommandFactoryApi]
  private val lockUnlockUtil              = mock[LockUnlockUtil]
  private val iConfigClientService        = mock[IConfigClientService]
  private val iAlarmService               = mock[IAlarmService]

  val cswServices = new CswServices(
    () => sequenceOperator,
    actorSystem,
    iLocationService,
    iEventService,
    timeServiceSchedulerFactory,
    sequencerAdminFactoryApi,
    sequencerCommandFactoryApi,
    lockUnlockUtil,
    iConfigClientService,
    iAlarmService
  )

  "load" must {

    "load script class if packageId and observingMode is provided | ESW-102" in {
      val loader: JScriptDsl =
        ScriptLoader.loadKotlinScript("esw.ocs.scripts.examples.testData.scriptLoader.ValidTestScript", cswServices)
      loader shouldBe a[JScriptDsl]
    }

    "throw InvalidScriptException if provided class is not a script | ESW-102" in {
      val exception = intercept[InvalidScriptException] {
        ScriptLoader.loadKotlinScript("esw.ocs.scripts.examples.testData.scriptLoader.InvalidTestScript", cswServices)
      }

      exception.getMessage shouldBe s"esw.ocs.scripts.examples.testData.scriptLoader.InvalidTestScript should be subclass of Script"
    }

    "throw ScriptNotFound if provided class does not exist at configured path | ESW-102" in {
      val invalidScriptClass = "invalid.path.TestScriptDoesNotExist"

      val exception = intercept[ScriptNotFound] {
        ScriptLoader.loadKotlinScript(invalidScriptClass, cswServices)
      }

      exception.getMessage shouldBe "invalid.path.TestScriptDoesNotExist not found at configured path"
    }

    "throw ScriptInitialisationFailedException if script initialisation fails | ESW-102,ESW-243" in {
      val exception = intercept[ScriptInitialisationFailedException] {
        ScriptLoader.loadKotlinScript(
          "esw.ocs.scripts.examples.testData.scriptLoader.InitialisationExceptionTestScript",
          cswServices
        )
      }
      exception.getMessage shouldBe "Script initialization failed with : initialisation failed"
    }
  }
}
