package esw.ocs.impl.internal

import java.net.URI
import org.apache.pekko.Done
import org.apache.pekko.actor.CoordinatedShutdown
import org.apache.pekko.actor.CoordinatedShutdown.UnknownReason
import org.apache.pekko.actor.testkit.typed.scaladsl.TestProbe
import org.apache.pekko.actor.typed.{ActorRef, ActorSystem, SpawnProtocol}
import csw.location.api.PekkoRegistrationFactory
import csw.location.api.exceptions.{OtherLocationIsRegistered, RegistrationFailed}
import csw.location.api.extensions.ActorExtension.RichActor
import csw.location.api.models.ComponentType.SequenceComponent
import csw.location.api.models.Connection.PekkoConnection
import csw.location.api.models.*
import csw.location.api.scaladsl.{LocationService, RegistrationResult}
import csw.prefix.models.Subsystem.ESW
import csw.prefix.models.{Prefix, Subsystem}
import esw.commons.utils.location.EswLocationError
import esw.ocs.api.actor.messages.SequenceComponentMsg
import esw.ocs.api.actor.messages.SequenceComponentMsg.Stop
import esw.testcommons.BaseTestSuite
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.scalatest.time.SpanSugar.convertFloatToGrainOfTime

import scala.concurrent.{ExecutionContext, Future}
import org.mockito.Mockito.{reset, times, verify, when}
class SequenceComponentRegistrationTest extends BaseTestSuite {
  private val subsystem = Subsystem.TCS
  private val uri       = new URI("uri")

  override implicit def patienceConfig: PatienceConfig = PatienceConfig(10.seconds)

  private val agentPrefix        = Prefix(ESW, "agent1")
  private def metadata: Metadata = Metadata().withAgentPrefix(agentPrefix).withPid(ProcessHandle.current().pid())

  private val registrationResult: RegistrationResult = mock[RegistrationResult]
  private val locationService: LocationService       = mock[LocationService]

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    reset(registrationResult)
    reset(locationService)
  }

  "registerSequenceComponent with name and agent prefix provided" must {
    val retryCount      = 2
    val name            = Some("primary")
    val prefix          = Prefix("TCS.primary")
    val pekkoConnection = PekkoConnection(ComponentId(prefix, SequenceComponent))
    val pekkoLocation   = PekkoLocation(pekkoConnection, uri, metadata)

    "return successful RegistrationResult | ESW-144, ESW-366" in {
      implicit val system: ActorSystem[SpawnProtocol.Command] = ActorSystem(SpawnProtocol(), "test")
      implicit val ec: ExecutionContext                       = system.executionContext
      val coordinatedShutdown                                 = CoordinatedShutdown(system)
      val (sequenceComponentRegistration, sequenceComponentProbe) =
        createSequenceComponentRegistration(locationService, name, Some(agentPrefix))
      val pekkoRegistration = PekkoRegistrationFactory.make(pekkoConnection, sequenceComponentProbe.ref, metadata)

      when(locationService.register(pekkoRegistration)).thenReturn(Future(registrationResult))
      when(registrationResult.location).thenReturn(pekkoLocation)
      when(registrationResult.unregister()).thenReturn(Future.successful(Done))

      sequenceComponentRegistration.registerSequenceComponent(retryCount).rightValue should ===(pekkoLocation)
      coordinatedShutdown.run(UnknownReason).futureValue
      verify(locationService).register(pekkoRegistration)
      verify(registrationResult, times(2)).location
      verify(registrationResult).unregister()
    }

    "map location service registration failure to RegistrationError | ESW-144, ESW-366" in {
      implicit val system: ActorSystem[SpawnProtocol.Command] = ActorSystem(SpawnProtocol(), "test")
      val coordinatedShutdown                                 = CoordinatedShutdown(system)
      val errorMsg                                            = "error message"
      val locationService                                     = mock[LocationService]
      val (sequenceComponentRegistration, sequenceComponentProbe) =
        createSequenceComponentRegistration(locationService, name, Some(agentPrefix))

      val pekkoRegistration = PekkoRegistrationFactory.make(pekkoConnection, sequenceComponentProbe.ref, metadata)

      when(locationService.register(pekkoRegistration)).thenReturn(Future.failed(OtherLocationIsRegistered(errorMsg)))

      sequenceComponentRegistration.registerSequenceComponent(retryCount).leftValue should ===(
        EswLocationError.OtherLocationIsRegistered(errorMsg)
      )

      // assert that No retry attempt in case of subsystem and name are provided
      verify(locationService, times(1)).register(pekkoRegistration)
      coordinatedShutdown.run(UnknownReason).futureValue
    }
  }

  "registerSequenceComponent without name" must {
    val prefix          = Prefix("TCS.TCS_23")
    val pekkoConnection = PekkoConnection(ComponentId(prefix, SequenceComponent))
    val pekkoLocation   = PekkoLocation(pekkoConnection, uri, metadata)

    "return successful RegistrationResult | ESW-144, ESW-366" in {
      implicit val system: ActorSystem[SpawnProtocol.Command] = ActorSystem(SpawnProtocol(), "test")
      implicit val ec: ExecutionContext                       = system.executionContext
      val coordinatedShutdown                                 = CoordinatedShutdown(system)
      val retryCount                                          = 2
      val (sequenceComponentRegistration, sequenceComponentProbe) =
        createSequenceComponentRegistration(locationService, None, Some(agentPrefix))

      when(locationService.register(any[PekkoRegistration])).thenReturn(Future(registrationResult))
      when(registrationResult.location).thenReturn(pekkoLocation)
      when(registrationResult.unregister()).thenReturn(Future.successful(Done))

      sequenceComponentRegistration.registerSequenceComponent(retryCount).rightValue should ===(pekkoLocation)

      coordinatedShutdown.run(UnknownReason).futureValue

      verifyRegister(sequenceComponentProbe, metadata)
      verify(registrationResult, times(2)).location
      verify(registrationResult).unregister()
    }

    "retry if OtherLocationIsRegistered | ESW-144, ESW-366" in {
      implicit val system: ActorSystem[SpawnProtocol.Command] = ActorSystem(SpawnProtocol(), "test")
      val coordinatedShutdown                                 = CoordinatedShutdown(system)

      val errorMsg   = "error message"
      val retryCount = 1
      val (sequenceComponentRegistration, sequenceComponentProbe) =
        createSequenceComponentRegistration(locationService, None, Some(agentPrefix))

      when(locationService.register(any[PekkoRegistration]))
        .thenReturn(
          Future.failed(OtherLocationIsRegistered(errorMsg)),
          Future.successful(registrationResult)
        )
      when(registrationResult.location).thenReturn(pekkoLocation)
      when(registrationResult.unregister()).thenReturn(Future.successful(Done))

      val regResult = sequenceComponentRegistration.registerSequenceComponent(retryCount)

      // Assert that sequenceComponentActor ref receives Stop message once when OtherLocationIsRegistered is received
      sequenceComponentProbe.expectMessage(Stop)
      regResult.rightValue should ===(pekkoLocation)
      coordinatedShutdown.run(UnknownReason).futureValue
      verifyRegister(sequenceComponentProbe, metadata, 2)
      verify(registrationResult, times(2)).location
      verify(registrationResult).unregister()
    }

    "not retry if RegistrationFailed | ESW-144, ESW-366" in {
      implicit val system: ActorSystem[SpawnProtocol.Command] = ActorSystem(SpawnProtocol(), "test")
      implicit val ec: ExecutionContext                       = system.executionContext

      val errorMsg   = "error message"
      val retryCount = 3
      val (sequenceComponentRegistration, sequenceComponentProbe) =
        createSequenceComponentRegistration(locationService, None, Some(agentPrefix))

      when(locationService.register(any[PekkoRegistration]))
        .thenReturn(Future.failed(RegistrationFailed(errorMsg)), Future(registrationResult))

      val error = sequenceComponentRegistration.registerSequenceComponent(retryCount).leftValue

      error should ===(EswLocationError.RegistrationFailed(errorMsg))
      verifyRegister(sequenceComponentProbe, metadata)
      system.terminate()
      system.whenTerminated.futureValue
    }

    "map location service registration failure to RegistrationError if could not register after retry attempts | ESW-144, ESW-366" in {
      implicit val system: ActorSystem[SpawnProtocol.Command] = ActorSystem(SpawnProtocol(), "test")
      val (sequenceComponentRegistration, sequenceComponentProbe) =
        createSequenceComponentRegistration(locationService, None, Some(agentPrefix))

      val errorMsg   = "error message"
      val retryCount = 2

      when(locationService.register(any[PekkoRegistration]))
        .thenReturn(
          Future.failed(OtherLocationIsRegistered(errorMsg)),
          Future.failed(OtherLocationIsRegistered(errorMsg)),
          Future.failed(OtherLocationIsRegistered(errorMsg))
        )

      sequenceComponentRegistration.registerSequenceComponent(retryCount).leftValue should ===(
        EswLocationError.OtherLocationIsRegistered(errorMsg)
      )

      // verify attempt for retries when error is OtherLocationIsRegistered
      // verify that location service register call is made (retry count + 1 original call) times as error is OtherLocationIsRegistered
      verifyRegister(sequenceComponentProbe, metadata, retryCount + 1)
      system.terminate()
      system.whenTerminated.futureValue
    }
  }

  "registerSequenceComponent without agent prefix" must {
    val retryCount      = 2
    val name            = Some("primary")
    val prefix          = Prefix("TCS.primary")
    val pekkoConnection = PekkoConnection(ComponentId(prefix, SequenceComponent))
    val pekkoLocation   = PekkoLocation(pekkoConnection, uri, Metadata.empty)

    "return successful RegistrationResult | ESW-144, ESW-366" in {
      implicit val system: ActorSystem[SpawnProtocol.Command] = ActorSystem(SpawnProtocol(), "test")
      implicit val ec: ExecutionContext                       = system.executionContext
      val coordinatedShutdown                                 = CoordinatedShutdown(system)
      val (sequenceComponentRegistration, sequenceComponentProbe) =
        createSequenceComponentRegistration(locationService, name, None)

      val pekkoRegistration = PekkoRegistrationFactory.make(
        pekkoConnection,
        sequenceComponentProbe.ref,
        Metadata().withPid(ProcessHandle.current().pid())
      )

      when(locationService.register(pekkoRegistration)).thenReturn(Future(registrationResult))
      when(registrationResult.location).thenReturn(pekkoLocation)
      when(registrationResult.unregister()).thenReturn(Future.successful(Done))

      sequenceComponentRegistration.registerSequenceComponent(retryCount).rightValue should ===(pekkoLocation)
      coordinatedShutdown.run(UnknownReason).futureValue
      verify(locationService).register(pekkoRegistration)
      verify(registrationResult, times(2)).location
      verify(registrationResult).unregister()
    }
  }

  private def createSequenceComponentRegistration(
      locationService: LocationService,
      name: Option[String],
      agentPrefix: Option[Prefix]
  )(implicit
      actorSystem: ActorSystem[SpawnProtocol.Command]
  ): (SequenceComponentRegistration, TestProbe[SequenceComponentMsg]) = {
    val sequenceComponentProbe: TestProbe[SequenceComponentMsg]          = TestProbe[SequenceComponentMsg]()
    val seqCompFactory: Prefix => Future[ActorRef[SequenceComponentMsg]] = _ => Future.successful(sequenceComponentProbe.ref)

    val sequenceComponentRegistration =
      new SequenceComponentRegistration(subsystem, name, agentPrefix, locationService, seqCompFactory)
    (sequenceComponentRegistration, sequenceComponentProbe)
  }

  private def verifyRegister(
      sequenceComponentProbe: TestProbe[SequenceComponentMsg],
      metadata: Metadata,
      noOfInvocation: Int = 1
  ): Unit = {
    val captor: ArgumentCaptor[PekkoRegistration] = ArgumentCaptor.forClass(classOf[PekkoRegistration])
    verify(locationService, times(noOfInvocation)).register(captor.capture())
    val value = captor.getValue
    value.connection.componentId.prefix.toString().contains(s"$subsystem.${subsystem}_") shouldBe true
    value.connection.componentId.componentType should ===(SequenceComponent)
    value.actorRefURI should ===(sequenceComponentProbe.ref.toURI)
    value.metadata should ===(metadata)

  }
}
