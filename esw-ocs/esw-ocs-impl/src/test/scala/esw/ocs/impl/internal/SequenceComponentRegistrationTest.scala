package esw.ocs.impl.internal

import java.net.URI

import akka.Done
import akka.actor.CoordinatedShutdown
import akka.actor.CoordinatedShutdown.UnknownReason
import akka.actor.testkit.typed.scaladsl.TestProbe
import akka.actor.typed.{ActorRef, ActorSystem, SpawnProtocol}
import csw.location.api.AkkaRegistrationFactory
import csw.location.api.exceptions.{OtherLocationIsRegistered, RegistrationFailed}
import csw.location.api.extensions.ActorExtension.RichActor
import csw.location.api.models.ComponentType.SequenceComponent
import csw.location.api.models.Connection.AkkaConnection
import csw.location.api.models._
import csw.location.api.scaladsl.{LocationService, RegistrationResult}
import csw.prefix.models.{Prefix, Subsystem}
import esw.commons.utils.location.EswLocationError
import esw.ocs.api.actor.messages.SequenceComponentMsg
import esw.ocs.api.actor.messages.SequenceComponentMsg.Stop
import esw.testcommons.BaseTestSuite
import org.mockito.ArgumentMatchers.any
import org.mockito.captor.ArgCaptor
import org.scalatest.time.SpanSugar.convertFloatToGrainOfTime

import scala.concurrent.{ExecutionContext, Future}

class SequenceComponentRegistrationTest extends BaseTestSuite {
  private val subsystem = Subsystem.TCS
  private val uri       = new URI("uri")

  override implicit def patienceConfig: PatienceConfig = PatienceConfig(10.seconds)

  private val agentPrefix        = "ESW.agent1"
  private val metadata: Metadata = Metadata().withAgentPrefix(Prefix(agentPrefix))

  private val registrationResult: RegistrationResult = mock[RegistrationResult]
  private val locationService: LocationService       = mock[LocationService]

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    reset(registrationResult)
    reset(locationService)
  }

  "registerSequenceComponent with name and agent prefix provided" must {
    val retryCount     = 2
    val name           = Some("primary")
    val prefix         = Prefix("TCS.primary")
    val akkaConnection = AkkaConnection(ComponentId(prefix, SequenceComponent))
    val akkaLocation   = AkkaLocation(akkaConnection, uri, metadata)

    "return successful RegistrationResult | ESW-144" in {
      implicit val system: ActorSystem[SpawnProtocol.Command] = ActorSystem(SpawnProtocol(), "test")
      implicit val ec: ExecutionContext                       = system.executionContext
      val coordinatedShutdown                                 = CoordinatedShutdown(system)
      val (sequenceComponentRegistration, sequenceComponentProbe) =
        createSequenceComponentRegistration(locationService, name, Some(agentPrefix))
      val akkaRegistration = AkkaRegistrationFactory.make(akkaConnection, sequenceComponentProbe.ref, metadata)

      when(locationService.register(akkaRegistration)).thenReturn(Future(registrationResult))
      when(registrationResult.location).thenReturn(akkaLocation)
      when(registrationResult.unregister()).thenReturn(Future.successful(Done))

      sequenceComponentRegistration.registerSequenceComponent(retryCount).rightValue should ===(akkaLocation)
      coordinatedShutdown.run(UnknownReason).futureValue
      verify(locationService).register(akkaRegistration)
      verify(registrationResult, times(2)).location
      verify(registrationResult).unregister()
    }

    "map location service registration failure to RegistrationError | ESW-144" in {
      implicit val system: ActorSystem[SpawnProtocol.Command] = ActorSystem(SpawnProtocol(), "test")
      val coordinatedShutdown                                 = CoordinatedShutdown(system)
      val errorMsg                                            = "error message"
      val locationService                                     = mock[LocationService]
      val (sequenceComponentRegistration, sequenceComponentProbe) =
        createSequenceComponentRegistration(locationService, name, Some(agentPrefix))
      val akkaRegistration = AkkaRegistrationFactory.make(akkaConnection, sequenceComponentProbe.ref, metadata)

      when(locationService.register(akkaRegistration)).thenReturn(Future.failed(OtherLocationIsRegistered(errorMsg)))

      sequenceComponentRegistration.registerSequenceComponent(retryCount).leftValue should ===(
        EswLocationError.OtherLocationIsRegistered(errorMsg)
      )

      //assert that No retry attempt in case of subsystem and name are provided
      verify(locationService, times(1)).register(akkaRegistration)
      coordinatedShutdown.run(UnknownReason).futureValue
    }
  }

  "registerSequenceComponent without name" must {
    val prefix         = Prefix("TCS.TCS_23")
    val akkaConnection = AkkaConnection(ComponentId(prefix, SequenceComponent))
    val akkaLocation   = AkkaLocation(akkaConnection, uri, metadata)

    "return successful RegistrationResult | ESW-144" in {
      implicit val system: ActorSystem[SpawnProtocol.Command] = ActorSystem(SpawnProtocol(), "test")
      implicit val ec: ExecutionContext                       = system.executionContext
      val coordinatedShutdown                                 = CoordinatedShutdown(system)
      val retryCount                                          = 2
      val (sequenceComponentRegistration, sequenceComponentProbe) =
        createSequenceComponentRegistration(locationService, None, Some(agentPrefix))

      when(locationService.register(any[AkkaRegistration])).thenReturn(Future(registrationResult))
      when(registrationResult.location).thenReturn(akkaLocation)
      when(registrationResult.unregister()).thenReturn(Future.successful(Done))

      sequenceComponentRegistration.registerSequenceComponent(retryCount).rightValue should ===(akkaLocation)

      coordinatedShutdown.run(UnknownReason).futureValue

      verifyRegister(sequenceComponentProbe, metadata)
      verify(registrationResult, times(2)).location
      verify(registrationResult).unregister()
    }

    "retry if OtherLocationIsRegistered | ESW-144" in {
      implicit val system: ActorSystem[SpawnProtocol.Command] = ActorSystem(SpawnProtocol(), "test")
      val coordinatedShutdown                                 = CoordinatedShutdown(system)

      val errorMsg   = "error message"
      val retryCount = 1
      val (sequenceComponentRegistration, sequenceComponentProbe) =
        createSequenceComponentRegistration(locationService, None, Some(agentPrefix))

      when(locationService.register(any[AkkaRegistration]))
        .thenReturn(
          Future.failed(OtherLocationIsRegistered(errorMsg)),
          Future.successful(registrationResult)
        )
      when(registrationResult.location).thenReturn(akkaLocation)
      when(registrationResult.unregister()).thenReturn(Future.successful(Done))

      val regResult = sequenceComponentRegistration.registerSequenceComponent(retryCount)

      //Assert that sequenceComponentActor ref receives Stop message once when OtherLocationIsRegistered is received
      sequenceComponentProbe.expectMessage(Stop)
      regResult.rightValue should ===(akkaLocation)
      coordinatedShutdown.run(UnknownReason).futureValue
      verifyRegister(sequenceComponentProbe, metadata, 2)
      verify(registrationResult, times(2)).location
      verify(registrationResult).unregister()
    }

    "not retry if RegistrationFailed | ESW-144" in {
      implicit val system: ActorSystem[SpawnProtocol.Command] = ActorSystem(SpawnProtocol(), "test")
      implicit val ec: ExecutionContext                       = system.executionContext

      val errorMsg   = "error message"
      val retryCount = 3
      val (sequenceComponentRegistration, sequenceComponentProbe) =
        createSequenceComponentRegistration(locationService, None, Some(agentPrefix))

      when(locationService.register(any[AkkaRegistration]))
        .thenReturn(Future.failed(RegistrationFailed(errorMsg)), Future(registrationResult))

      val error = sequenceComponentRegistration.registerSequenceComponent(retryCount).leftValue

      error should ===(EswLocationError.RegistrationFailed(errorMsg))
      verifyRegister(sequenceComponentProbe, metadata)
      system.terminate()
      system.whenTerminated.futureValue
    }

    "map location service registration failure to RegistrationError if could not register after retry attempts | ESW-144" in {
      implicit val system: ActorSystem[SpawnProtocol.Command] = ActorSystem(SpawnProtocol(), "test")
      val (sequenceComponentRegistration, sequenceComponentProbe) =
        createSequenceComponentRegistration(locationService, None, Some(agentPrefix))

      val errorMsg   = "error message"
      val retryCount = 2

      when(locationService.register(any[AkkaRegistration]))
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
    val retryCount     = 2
    val name           = Some("primary")
    val prefix         = Prefix("TCS.primary")
    val akkaConnection = AkkaConnection(ComponentId(prefix, SequenceComponent))
    val akkaLocation   = AkkaLocation(akkaConnection, uri, Metadata.empty)

    "return successful RegistrationResult | ESW-144" in {
      implicit val system: ActorSystem[SpawnProtocol.Command] = ActorSystem(SpawnProtocol(), "test")
      implicit val ec: ExecutionContext                       = system.executionContext
      val coordinatedShutdown                                 = CoordinatedShutdown(system)
      val (sequenceComponentRegistration, sequenceComponentProbe) =
        createSequenceComponentRegistration(locationService, name, None)
      val akkaRegistration = AkkaRegistrationFactory.make(akkaConnection, sequenceComponentProbe.ref, Metadata.empty)

      when(locationService.register(akkaRegistration)).thenReturn(Future(registrationResult))
      when(registrationResult.location).thenReturn(akkaLocation)
      when(registrationResult.unregister()).thenReturn(Future.successful(Done))

      sequenceComponentRegistration.registerSequenceComponent(retryCount).rightValue should ===(akkaLocation)
      coordinatedShutdown.run(UnknownReason).futureValue
      verify(locationService).register(akkaRegistration)
      verify(registrationResult, times(2)).location
      verify(registrationResult).unregister()
    }

  }

  private def createSequenceComponentRegistration(
      locationService: LocationService,
      name: Option[String],
      agentPrefix: Option[String]
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
    val captor = ArgCaptor[AkkaRegistration]
    verify(locationService, times(noOfInvocation)).register(captor)
    captor.values.foreach { value =>
      value.connection.componentId.prefix.toString().contains(s"$subsystem.${subsystem}_") shouldBe true
      value.connection.componentId.componentType should ===(SequenceComponent)
      value.actorRefURI should ===(sequenceComponentProbe.ref.toURI)
      value.metadata should ===(metadata)
    }
  }
}
