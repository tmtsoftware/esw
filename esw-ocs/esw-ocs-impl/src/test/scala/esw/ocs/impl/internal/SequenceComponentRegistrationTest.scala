package esw.ocs.impl.internal

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
import csw.params.core.models.{Prefix, Subsystem}
import esw.ocs.api.BaseTestSuite
import esw.ocs.api.protocol.LoadScriptError
import esw.ocs.impl.messages.SequenceComponentMsg
import esw.ocs.impl.messages.SequenceComponentMsg.Stop
import org.mockito.ArgumentMatchers.any

import scala.concurrent.{ExecutionContext, Future}

class SequenceComponentRegistrationTest extends ScalaTestWithActorTestKit with BaseTestSuite {
  private val subsystem = Subsystem.TCS
  private val uri       = new URI("uri")

  "registerSequenceComponent with name provided" must {
    val retryCount         = 2
    val registrationResult = mock[RegistrationResult]
    val name               = Some("primary")
    val akkaConnection     = AkkaConnection(ComponentId("TCS.primary", SequenceComponent))
    val prefix             = Prefix("TCS.primary")
    val akkaLocation       = AkkaLocation(akkaConnection, prefix, uri)

    "return successful RegistrationResult | ESW-144" in {
      implicit val system: ActorSystem[SpawnProtocol.Command] = ActorSystem(SpawnProtocol(), "test")
      implicit val ec: ExecutionContext                       = system.executionContext
      val coordinatedShutdown                                 = CoordinatedShutdown(system.toClassic)
      val locationService                                     = mock[LocationService]

      when(registrationResult.location).thenReturn(akkaLocation)
      when(registrationResult.unregister()).thenReturn(Future.successful(Done))
      when(locationService.register(any[AkkaRegistration])).thenReturn(Future(registrationResult))
      when(locationService.list(ComponentType.SequenceComponent)).thenReturn(Future.successful(List.empty))

      val sequenceComponentProbe: TestProbe[SequenceComponentMsg] = TestProbe[SequenceComponentMsg]()

      def sequenceComponentFactory(sequenceComponentName: String): Future[ActorRef[SequenceComponentMsg]] =
        Future.successful(sequenceComponentProbe.ref)
      val sequenceComponentRegistration =
        new SequenceComponentRegistration(
          subsystem,
          name,
          locationService,
          sequenceComponentFactory
        )

      sequenceComponentRegistration
        .registerSequenceComponent(retryCount)
        .rightValue should ===(
        akkaLocation
      )
      coordinatedShutdown.run(UnknownReason).futureValue
      verify(registrationResult).unregister()
    }

    "map location service registration failure to RegistrationError | ESW-144" in {
      implicit val system: ActorSystem[SpawnProtocol.Command] = ActorSystem(SpawnProtocol(), "test")
      val errorMsg                                            = "error message"
      val locationService                                     = mock[LocationService]

      when(locationService.register(any[AkkaRegistration]))
        .thenReturn(
          Future.failed(OtherLocationIsRegistered(errorMsg))
        )
      when(registrationResult.location).thenReturn(akkaLocation)
      when(registrationResult.unregister()).thenReturn(Future.successful(Done))

      val sequenceComponentProbe: TestProbe[SequenceComponentMsg] = TestProbe[SequenceComponentMsg]()
      def sequenceComponentFactory(sequenceComponentName: String): Future[ActorRef[SequenceComponentMsg]] =
        Future.successful(sequenceComponentProbe.ref)

      val sequenceComponentRegistration =
        new SequenceComponentRegistration(subsystem, name, locationService, sequenceComponentFactory)

      sequenceComponentRegistration
        .registerSequenceComponent(retryCount)
        .leftValue should ===(
        LoadScriptError(errorMsg)
      )

      //assert that No retry attempt in case of subsystem and name are provided
      verify(locationService, times(1)).register(any[AkkaRegistration])
    }
  }

  "registerSequenceComponent without name" must {
    val registrationResult = mock[RegistrationResult]
    val akkaConnection     = AkkaConnection(ComponentId("TCS.TCS_23", SequenceComponent))
    val prefix             = Prefix("TCS.TCS_23")
    val akkaLocation       = AkkaLocation(akkaConnection, prefix, uri)
    val locationService    = mock[LocationService]

    "return successful RegistrationResult | ESW-144" in {
      implicit val system: ActorSystem[SpawnProtocol.Command] = ActorSystem(SpawnProtocol(), "test")
      implicit val ec: ExecutionContext                       = system.executionContext
      val coordinatedShutdown                                 = CoordinatedShutdown(system.toClassic)
      val retryCount                                          = 2

      when(locationService.register(any[AkkaRegistration])).thenReturn(Future(registrationResult))
      when(registrationResult.location).thenReturn(akkaLocation)
      when(registrationResult.unregister()).thenReturn(Future.successful(Done))

      val sequenceComponentProbe: TestProbe[SequenceComponentMsg] = TestProbe[SequenceComponentMsg]()
      def sequenceComponentFactory(sequenceComponentName: String): Future[ActorRef[SequenceComponentMsg]] =
        Future.successful(sequenceComponentProbe.ref)
      val sequenceComponentRegistration =
        new SequenceComponentRegistration(
          subsystem,
          None,
          locationService,
          sequenceComponentFactory
        )

      sequenceComponentRegistration
        .registerSequenceComponent(retryCount)
        .rightValue should ===(
        akkaLocation
      )

      coordinatedShutdown.run(UnknownReason).futureValue
      verify(registrationResult).unregister()
    }

    "retry if OtherLocationIsRegistered | ESW-144" in {
      implicit val system: ActorSystem[SpawnProtocol.Command] = ActorSystem(SpawnProtocol(), "test")
      val coordinatedShutdown                                 = CoordinatedShutdown(system.toClassic)
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

      val sequenceComponentProbe: TestProbe[SequenceComponentMsg] = TestProbe[SequenceComponentMsg]()
      def sequenceComponentFactory(sequenceComponentName: String): Future[ActorRef[SequenceComponentMsg]] =
        Future.successful(sequenceComponentProbe.ref)

      val sequenceComponentRegistration =
        new SequenceComponentRegistration(subsystem, None, locationService, sequenceComponentFactory)

      val regResult = sequenceComponentRegistration.registerSequenceComponent(retryCount)

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

      val sequenceComponentProbe: TestProbe[SequenceComponentMsg] = TestProbe[SequenceComponentMsg]()
      def sequenceComponentFactory(sequenceComponentName: String): Future[ActorRef[SequenceComponentMsg]] =
        Future.successful(sequenceComponentProbe.ref)

      val sequenceComponentRegistration =
        new SequenceComponentRegistration(subsystem, None, locationService, sequenceComponentFactory)
      sequenceComponentRegistration
        .registerSequenceComponent(retryCount)
        .leftValue should ===(
        LoadScriptError(errorMsg)
      )
      system.terminate()
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

      val sequenceComponentProbe: TestProbe[SequenceComponentMsg] = TestProbe[SequenceComponentMsg]()
      def sequenceComponentFactory(sequenceComponentName: String): Future[ActorRef[SequenceComponentMsg]] =
        Future.successful(sequenceComponentProbe.ref)

      val sequenceComponentRegistration =
        new SequenceComponentRegistration(subsystem, None, locationService, sequenceComponentFactory)

      sequenceComponentRegistration
        .registerSequenceComponent(retryCount)
        .leftValue should ===(
        LoadScriptError(errorMsg)
      )
      system.terminate()
    }
  }

}
