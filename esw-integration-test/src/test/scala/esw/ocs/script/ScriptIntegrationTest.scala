package esw.ocs.script

import akka.actor.typed.scaladsl.AskPattern._
import akka.actor.typed.{ActorRef, ActorSystem, SpawnProtocol}
import akka.util.Timeout
import com.typesafe.config.ConfigFactory
import csw.command.client.messages.sequencer.{SequencerMsg, SubmitSequenceAndWait}
import csw.location.api.extensions.URIExtension.RichURI
import csw.location.api.scaladsl.LocationService
import csw.location.client.scaladsl.HttpLocationServiceFactory
import csw.params.commands.CommandResponse.SubmitResponse
import csw.params.commands.{CommandName, Sequence, Setup}
import csw.params.core.models.Prefix
import csw.testkit.ConfigTestKit
import csw.testkit.scaladsl.CSWService.{AlarmServer, ConfigServer, EventServer}
import csw.testkit.scaladsl.ScalaTestFrameworkTestKit
import esw.ocs.api.BaseTestSuite
import esw.ocs.app.wiring.SequencerWiring
import esw.ocs.impl.internal.Timeouts

import scala.concurrent.Future
import scala.concurrent.duration.DurationDouble

class ScriptIntegrationTest extends ScalaTestFrameworkTestKit(EventServer, AlarmServer, ConfigServer) with BaseTestSuite {

  import frameworkTestKit.mat

  implicit val actorSystem: ActorSystem[SpawnProtocol.Command] = frameworkTestKit.actorSystem

  private implicit val askTimeout: Timeout = Timeouts.DefaultTimeout

  override implicit def patienceConfig: PatienceConfig = PatienceConfig(10.seconds)

  // TestScript.kt
  private val ocsPackageId     = "esw"
  private val ocsObservingMode = "darknight"
  private val tcsPackageId     = "tcs"
  private val tcsObservingMode = "darknight"

  // TestScript4.kts
  private val irmsPackageId     = "irms"
  private val irmsObservingMode = "darknight"

  private var locationService: LocationService      = _
  private var ocsWiring: SequencerWiring            = _
  private var ocsSequencer: ActorRef[SequencerMsg]  = _
  private var tcsWiring: SequencerWiring            = _
  private var tcsSequencer: ActorRef[SequencerMsg]  = _
  private var irmsWiring: SequencerWiring           = _
  private var irmsSequencer: ActorRef[SequencerMsg] = _
  private val configTestKit: ConfigTestKit          = frameworkTestKit.configTestKit

  override def beforeAll(): Unit = {
    super.beforeAll()
    frameworkTestKit.spawnStandalone(ConfigFactory.load("standalone.conf"))
  }

  override def beforeEach(): Unit = {
    locationService = HttpLocationServiceFactory.makeLocalClient
    tcsWiring = new SequencerWiring(tcsPackageId, tcsObservingMode, None)
    tcsWiring.sequencerServer.start()
    tcsSequencer = tcsWiring.sequencerRef

    //start IRMS sequencer as OCS send commands to IRMS downstream sequencer
    irmsWiring = new SequencerWiring(irmsPackageId, irmsObservingMode, None)
    irmsWiring.sequencerServer.start()
    irmsSequencer = irmsWiring.sequencerRef

    ocsWiring = new SequencerWiring(ocsPackageId, ocsObservingMode, None)
    ocsSequencer = ocsWiring.sequencerServer.start().rightValue.uri.toActorRef.unsafeUpcast[SequencerMsg]
  }

  override def afterEach(): Unit = {
    ocsWiring.sequencerServer.shutDown().futureValue
    irmsWiring.sequencerServer.shutDown().futureValue
    tcsWiring.sequencerServer.shutDown().futureValue
  }

  "New Sequencer with no CRM" must {
    "handle success scenario for top level commands/steps" in {
      val craWiring    = new SequencerWiring("nocrm", "noCrmTest", None)
      val craSequencer = craWiring.sequencerServer.start().rightValue.uri.toActorRef.unsafeUpcast[SequencerMsg]
      val command1     = Setup(Prefix("TCS"), CommandName("command-1"), None)
      val command2     = Setup(Prefix("TCS"), CommandName("command-2"), None)
      val sequence     = Sequence(command1, command2)

      val submitResponse: Future[SubmitResponse] = craSequencer ? (SubmitSequenceAndWait(sequence, _))
      println(submitResponse.futureValue)
      craWiring.sequencerServer.shutDown().futureValue
    }

    "handle exception scenario for top level commands" in {
      val craWiring    = new SequencerWiring("nocrm", "noCrmTest", None)
      val craSequencer = craWiring.sequencerServer.start().rightValue.uri.toActorRef.unsafeUpcast[SequencerMsg]
      val command3     = Setup(Prefix("TCS"), CommandName("command-3"), None)
      val command4     = Setup(Prefix("TCS"), CommandName("command-4"), None)
      val sequence     = Sequence(command3, command4)

      val submitResponse: Future[SubmitResponse] = craSequencer ? (SubmitSequenceAndWait(sequence, _))
      println(submitResponse.futureValue)
      craWiring.sequencerServer.shutDown().futureValue
    }

    "handle 'finishWithError' scenario for top level commands" in {
      val craWiring    = new SequencerWiring("nocrm", "noCrmTest", None)
      val craSequencer = craWiring.sequencerServer.start().rightValue.uri.toActorRef.unsafeUpcast[SequencerMsg]
      val command5     = Setup(Prefix("TCS"), CommandName("command-5"), None)
      val command6     = Setup(Prefix("TCS"), CommandName("command-6"), None)
      val sequence     = Sequence(command5, command6)

      val submitResponse: Future[SubmitResponse] = craSequencer ? (SubmitSequenceAndWait(sequence, _))
      println(submitResponse.futureValue)
      craWiring.sequencerServer.shutDown().futureValue
    }
  }
}
