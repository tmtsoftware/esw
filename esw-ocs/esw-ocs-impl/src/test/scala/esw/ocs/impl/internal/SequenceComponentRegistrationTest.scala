package esw.ocs.impl.internal

import java.net.URI

import akka.Done
import akka.actor.CoordinatedShutdown
import akka.actor.CoordinatedShutdown.UnknownReason
import akka.actor.testkit.typed.scaladsl.TestProbe
import akka.actor.typed.{ActorRef, ActorSystem, SpawnProtocol}
import csw.location.api.exceptions.{OtherLocationIsRegistered, RegistrationFailed}
import csw.location.api.models.ComponentType.SequenceComponent
import csw.location.api.models.Connection.AkkaConnection
import csw.location.api.models.{AkkaLocation, AkkaRegistration, ComponentId, ComponentType}
import csw.location.api.scaladsl.{LocationService, RegistrationResult}
import csw.prefix.models.{Prefix, Subsystem}
import esw.commons.BaseTestSuite
import esw.ocs.api.actor.messages.SequenceComponentMsg
import esw.ocs.api.actor.messages.SequenceComponentMsg.Stop
import esw.ocs.api.protocol.ScriptError
import org.mockito.ArgumentMatchers.any
import org.scalatest.time.SpanSugar.convertFloatToGrainOfTime

import scala.concurrent.{ExecutionContext, Future}

class SequenceComponentRegistrationTest extends BaseTestSuite {
  private val subsystem = Subsystem.TCS
  private val uri       = new URI("uri")

  override implicit def patienceConfig: PatienceConfig = PatienceConfig(10.seconds)

  private def registerSequenceComponent(locationService: LocationService, name: Option[String], retryCount: Int)(implicit
      actorSystem: ActorSystem[SpawnProtocol.Command]
  ) = {
    val sequenceComponentProbe: TestProbe[SequenceComponentMsg]          = TestProbe[SequenceComponentMsg]()
    val seqCompFactory: Prefix => Future[ActorRef[SequenceComponentMsg]] = _ => Future.successful(sequenceComponentProbe.ref)

    val sequenceComponentRegistration = new SequenceComponentRegistration(subsystem, name, locationService, seqCompFactory)
    (sequenceComponentRegistration.registerSequenceComponent(retryCount), sequenceComponentProbe)
  }

  "registerSequenceComponent with name provided" must {
    val retryCount         = 2
    val registrationResult = mock[RegistrationResult]
    val name               = Some("primary")
    val prefix             = Prefix("TCS.primary")
    val akkaConnection     = AkkaConnection(ComponentId(prefix, SequenceComponent))
    val akkaLocation       = AkkaLocation(akkaConnection, uri)

    "return successful RegistrationResult | ESW-144" in {
      implicit val system: ActorSystem[SpawnProtocol.Command] = ActorSystem(SpawnProtocol(), "test")
      implicit val ec: ExecutionContext                       = system.executionContext
      val coordinatedShutdown                                 = CoordinatedShutdown(system)
      val locationService                                     = mock[LocationService]

      when(registrationResult.location).thenReturn(akkaLocation)
      when(registrationResult.unregister()).thenReturn(Future.successful(Done))
      when(locationService.register(any[AkkaRegistration])).thenReturn(Future(registrationResult))
      when(locationService.list(ComponentType.SequenceComponent)).thenReturn(Future.successful(List.empty))

      registerSequenceComponent(locationService, name, retryCount)._1.rightValue should ===(akkaLocation)
      coordinatedShutdown.run(UnknownReason).futureValue
      verify(registrationResult).unregister()
    }

    "map location service registration failure to RegistrationError | ESW-144" in {
      implicit val system: ActorSystem[SpawnProtocol.Command] = ActorSystem(SpawnProtocol(), "test")
      val coordinatedShutdown                                 = CoordinatedShutdown(system)
      val errorMsg                                            = "error message"
      val locationService                                     = mock[LocationService]

      when(locationService.register(any[AkkaRegistration])).thenReturn(Future.failed(OtherLocationIsRegistered(errorMsg)))
      when(registrationResult.location).thenReturn(akkaLocation)
      when(registrationResult.unregister()).thenReturn(Future.successful(Done))

      registerSequenceComponent(locationService, name, retryCount)._1.leftValue should ===(ScriptError(errorMsg))

      //assert that No retry attempt in case of subsystem and name are provided
      verify(locationService, times(1)).register(any[AkkaRegistration])
      coordinatedShutdown.run(UnknownReason).futureValue
    }
  }

  "registerSequenceComponent without name" must {
    val registrationResult = mock[RegistrationResult]
    val prefix             = Prefix("TCS.TCS_23")
    val akkaConnection     = AkkaConnection(ComponentId(prefix, SequenceComponent))
    val akkaLocation       = AkkaLocation(akkaConnection, uri)
    val locationService    = mock[LocationService]

    "return successful RegistrationResult | ESW-144" in {
      implicit val system: ActorSystem[SpawnProtocol.Command] = ActorSystem(SpawnProtocol(), "test")
      implicit val ec: ExecutionContext                       = system.executionContext
      val coordinatedShutdown                                 = CoordinatedShutdown(system)
      val retryCount                                          = 2

      when(locationService.register(any[AkkaRegistration])).thenReturn(Future(registrationResult))
      when(registrationResult.location).thenReturn(akkaLocation)
      when(registrationResult.unregister()).thenReturn(Future.successful(Done))

      registerSequenceComponent(locationService, None, retryCount)._1.rightValue should ===(akkaLocation)

      coordinatedShutdown.run(UnknownReason).futureValue
      verify(registrationResult).unregister()
    }

    "retry if OtherLocationIsRegistered | ESW-144" in {
      implicit val system: ActorSystem[SpawnProtocol.Command] = ActorSystem(SpawnProtocol(), "test")
      val coordinatedShutdown                                 = CoordinatedShutdown(system)
      val locationService                                     = mock[LocationService]

      val errorMsg           = "error message"
      val retryCount         = 1
      val registrationResult = mock[RegistrationResult]

      when(locationService.register(any[AkkaRegistration]))
        .thenReturn(
          Future.failed(OtherLocationIsRegistered(errorMsg)),
          Future.successful(registrationResult)
        )
      when(registrationResult.location).thenReturn(akkaLocation)
      when(registrationResult.unregister()).thenReturn(Future.successful(Done))

      val (regResult, sequenceComponentProbe) = registerSequenceComponent(locationService, None, retryCount)

      //Assert that sequenceComponentActor ref receives Stop message once when OtherLocationIsRegistered is received
      sequenceComponentProbe.expectMessage(Stop)
      regResult.rightValue should ===(akkaLocation)
      coordinatedShutdown.run(UnknownReason).futureValue
      verify(registrationResult).unregister()
    }

    "not retry if RegistrationFailed | ESW-144" in {
      implicit val system: ActorSystem[SpawnProtocol.Command] = ActorSystem(SpawnProtocol(), "test")
      implicit val ec: ExecutionContext                       = system.executionContext
      val locationService                                     = mock[LocationService]

      val errorMsg           = "error message"
      val retryCount         = 3
      val registrationResult = mock[RegistrationResult]

      when(locationService.register(any[AkkaRegistration]))
        .thenReturn(Future.failed(RegistrationFailed(errorMsg)), Future(registrationResult))
      when(registrationResult.location).thenReturn(akkaLocation)
      when(registrationResult.unregister()).thenReturn(Future.successful(Done))

      registerSequenceComponent(locationService, None, retryCount)._1.leftValue should ===(ScriptError(errorMsg))
      system.terminate()
      system.whenTerminated.futureValue
    }

    "map location service registration failure to RegistrationError if could not register after retry attempts | ESW-144" in {
      implicit val system: ActorSystem[SpawnProtocol.Command] = ActorSystem(SpawnProtocol(), "test")
      val locationService                                     = mock[LocationService]

      val errorMsg   = "error message"
      val retryCount = 2

      when(locationService.register(any[AkkaRegistration]))
        .thenReturn(
          Future.failed(OtherLocationIsRegistered(errorMsg)),
          Future.failed(OtherLocationIsRegistered(errorMsg)),
          Future.failed(OtherLocationIsRegistered(errorMsg))
        )
      when(registrationResult.location).thenReturn(akkaLocation)
      when(registrationResult.unregister()).thenReturn(Future.successful(Done))

      registerSequenceComponent(locationService, None, retryCount)._1.leftValue should ===(ScriptError(errorMsg))
      system.terminate()
      system.whenTerminated.futureValue
    }
  }
}
