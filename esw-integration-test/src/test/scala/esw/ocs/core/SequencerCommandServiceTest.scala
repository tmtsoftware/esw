package esw.ocs.core

import akka.actor.typed.{ActorSystem, SpawnProtocol}
import csw.command.api.scaladsl.SequencerCommandService
import csw.command.client.internal.SequencerCommandServiceImpl
import csw.location.models.AkkaLocation
import csw.params.commands.CommandResponse.Completed
import csw.params.commands.{CommandName, Sequence, Setup}
import csw.params.core.models.Prefix
import csw.testkit.scaladsl.ScalaTestFrameworkTestKit
import esw.ocs.BaseTestSuite
import esw.ocs.api.models.messages.error.RegistrationError
import esw.ocs.internal.SequencerWiring
import org.scalatest.time.{Millis, Span}

class SequencerCommandServiceTest extends ScalaTestFrameworkTestKit with BaseTestSuite {
  import frameworkTestKit._
  private implicit val sys: ActorSystem[SpawnProtocol] = actorSystem

  private var wiring: SequencerWiring                                    = _
  private var sequencerLocation: Either[RegistrationError, AkkaLocation] = _

  override def beforeAll(): Unit = {
    super.beforeAll()
    wiring = new SequencerWiring("testSequencerId1", "testObservingMode1")
    sequencerLocation = wiring.start()
  }

  override protected def afterAll(): Unit = {
    wiring.shutDown().futureValue
    super.afterAll()
  }

  "should submit and process sequence" in {
    val command1 = Setup(Prefix("test"), CommandName("command-1"), None)
    val sequence = Sequence(command1)

    implicit val patienceConfig: PatienceConfig          = PatienceConfig(Span(500, Millis))
    val sequencerCommandService: SequencerCommandService = new SequencerCommandServiceImpl(sequencerLocation.rightValue)
    sequencerCommandService.submit(sequence).futureValue should ===(Completed(sequence.runId))
  }
}
