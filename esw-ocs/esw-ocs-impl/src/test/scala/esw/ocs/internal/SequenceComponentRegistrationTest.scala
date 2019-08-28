package esw.ocs.internal

import java.net.URI

import akka.Done
import akka.actor.CoordinatedShutdown
import akka.actor.CoordinatedShutdown.UnknownReason
import akka.actor.testkit.typed.scaladsl.{ScalaTestWithActorTestKit, TestProbe}
import akka.actor.typed.scaladsl.adapter.TypedActorSystemOps
import akka.actor.typed.{ActorRef, ActorSystem, SpawnProtocol}
import csw.location.api.exceptions.{OtherLocationIsRegistered, RegistrationFailed}
import csw.location.api.scaladsl.{LocationService, RegistrationResult}
import csw.location.models.ComponentType.SequenceComponent
import csw.location.models.Connection.AkkaConnection
import csw.location.models.{AkkaLocation, AkkaRegistration, ComponentId, ComponentType}
import csw.params.core.models.Prefix
import esw.ocs.api.BaseTestSuite
import esw.ocs.api.models.responses.RegistrationError
import esw.ocs.client.messages.SequenceComponentMsg
import esw.ocs.client.messages.SequenceComponentMsg.Stop
import org.mockito.ArgumentMatchers.any

import scala.concurrent.{ExecutionContext, Future}

class SequenceComponentRegistrationTest extends ScalaTestWithActorTestKit with BaseTestSuite {
  private val locationService = mock[LocationService]

  private val prefix         = Prefix("tcs.home.datum")
  private val uri            = new URI("uri")
  private val akkaConnection = AkkaConnection(ComponentId("TCS_1", SequenceComponent))
  private val akkaLocation   = AkkaLocation(akkaConnection, prefix, uri)

  "registerWithRetry" must {
    "return successful RegistrationResult | ESW-144" in {
      implicit val system: ActorSystem[SpawnProtocol] = ActorSystem(SpawnProtocol.behavior, "test")

      implicit val ec: ExecutionContext = system.executionContext
      val coordinatedShutdown           = CoordinatedShutdown(system.toUntyped)
      val retryCount                    = 2
      val registrationResult            = mock[RegistrationResult]

      when(registrationResult.location).thenReturn(akkaLocation)
      when(registrationResult.unregister()).thenReturn(Future.successful(Done))
      when(locationService.register(any[AkkaRegistration])).thenReturn(Future(registrationResult))
      when(locationService.list(ComponentType.SequenceComponent)).thenReturn(Future.successful(List.empty))

      val sequenceComponentProbe: TestProbe[SequenceComponentMsg] = TestProbe[SequenceComponentMsg]()
      def sequenceComponentFactory(sequenceComponentName: String): Future[ActorRef[SequenceComponentMsg]] =
        Future.successful(sequenceComponentProbe.ref)

      val sequenceComponentRegistration =
        new SequenceComponentRegistration(
          prefix,
          locationService,
          sequenceComponentFactory
        )

      sequenceComponentRegistration
        .registerWithRetry(retryCount)
        .rightValue should ===(
        akkaLocation
      )
      coordinatedShutdown.run(UnknownReason).futureValue
      verify(registrationResult).unregister()
    }

    "retry if OtherLocationIsRegistered | ESW-144" in {
      implicit val system: ActorSystem[SpawnProtocol] = ActorSystem(SpawnProtocol.behavior, "test")

      val coordinatedShutdown = CoordinatedShutdown(system.toUntyped)
      val errorMsg            = "error message"
      val retryCount          = 1
      val registrationResult  = mock[RegistrationResult]

      when(registrationResult.location).thenReturn(akkaLocation)
      when(registrationResult.unregister()).thenReturn(Future.successful(Done))
      when(locationService.register(any[AkkaRegistration]))
        .thenReturn(Future.failed(OtherLocationIsRegistered(errorMsg)), Future.successful(registrationResult))
      when(locationService.list(ComponentType.SequenceComponent)).thenReturn(Future.successful(List(akkaLocation)))

      val sequenceComponentProbe: TestProbe[SequenceComponentMsg] = TestProbe[SequenceComponentMsg]()
      def sequenceComponentFactory(sequenceComponentName: String): Future[ActorRef[SequenceComponentMsg]] =
        Future.successful(sequenceComponentProbe.ref)

      val sequenceComponentRegistration =
        new SequenceComponentRegistration(prefix, locationService, sequenceComponentFactory)

      val regResult = sequenceComponentRegistration.registerWithRetry(retryCount)
      sequenceComponentProbe.expectMessage(Stop)
      regResult.rightValue should ===(akkaLocation)
      coordinatedShutdown.run(UnknownReason).futureValue
      verify(registrationResult).unregister()
    }

    "not retry if RegistrationFailed | ESW-144" in {
      implicit val system: ActorSystem[SpawnProtocol] = ActorSystem(SpawnProtocol.behavior, "test")
      implicit val ec: ExecutionContext               = system.executionContext
      val errorMsg                                    = "error message"
      val retryCount                                  = 3
      val registrationResult                          = mock[RegistrationResult]

      when(registrationResult.location).thenReturn(akkaLocation)
      when(registrationResult.unregister()).thenReturn(Future.successful(Done))
      when(locationService.register(any[AkkaRegistration]))
        .thenReturn(Future.failed(RegistrationFailed(errorMsg)), Future(registrationResult))
      when(locationService.list(ComponentType.SequenceComponent)).thenReturn(Future.successful(List(akkaLocation)))

      val sequenceComponentProbe: TestProbe[SequenceComponentMsg] = TestProbe[SequenceComponentMsg]()
      def sequenceComponentFactory(sequenceComponentName: String): Future[ActorRef[SequenceComponentMsg]] =
        Future.successful(sequenceComponentProbe.ref)

      val sequenceComponentRegistration =
        new SequenceComponentRegistration(prefix, locationService, sequenceComponentFactory)
      sequenceComponentRegistration
        .registerWithRetry(retryCount)
        .leftValue should ===(
        RegistrationError(errorMsg)
      )
      system.terminate()
    }

    "map location service registration failure to RegistrationError if could not register after retry attempts | ESW-144" in {
      implicit val system: ActorSystem[SpawnProtocol] = ActorSystem(SpawnProtocol.behavior, "test")
      val errorMsg                                    = "error message"
      val retryCount                                  = 2
      when(locationService.register(any[AkkaRegistration]))
        .thenReturn(
          Future.failed(OtherLocationIsRegistered(errorMsg)),
          Future.failed(OtherLocationIsRegistered(errorMsg)),
          Future.failed(OtherLocationIsRegistered(errorMsg))
        )
      when(locationService.list(ComponentType.SequenceComponent)).thenReturn(Future.successful(List(akkaLocation)))

      val sequenceComponentProbe: TestProbe[SequenceComponentMsg] = TestProbe[SequenceComponentMsg]()
      def sequenceComponentFactory(sequenceComponentName: String): Future[ActorRef[SequenceComponentMsg]] =
        Future.successful(sequenceComponentProbe.ref)

      val sequenceComponentRegistration =
        new SequenceComponentRegistration(prefix, locationService, sequenceComponentFactory)

      sequenceComponentRegistration
        .registerWithRetry(retryCount)
        .leftValue should ===(
        RegistrationError(errorMsg)
      )
      system.terminate()
    }

  }

}
