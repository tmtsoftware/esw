package esw.commons.utils.location

import java.net.URI

import akka.actor.typed.{ActorSystem, SpawnProtocol}
import akka.util.Timeout
import csw.location.api.models.ComponentType.{Machine, SequenceComponent, Sequencer}
import csw.location.api.models.Connection.AkkaConnection
import csw.location.api.models.{AkkaLocation, ComponentId}
import csw.prefix.models.Prefix
import csw.prefix.models.Subsystem.TCS
import esw.agent.client.AgentClient
import esw.commons.utils.location.EswLocationError.{RegistrationListingFailed, ResolveLocationFailed}
import esw.commons.{BaseTestSuite, Timeouts}
import esw.ocs.api.SequencerApi
import esw.ocs.api.actor.client.SequenceComponentImpl

import scala.concurrent.duration.DurationInt
import scala.concurrent.{ExecutionContext, Future}

class ComponentFactoryTest extends BaseTestSuite {
  implicit val actorSystem: ActorSystem[SpawnProtocol.Command] = ActorSystem(SpawnProtocol(), "test")
  implicit val ec: ExecutionContext                            = actorSystem.executionContext
  implicit val timeout: Timeout                                = Timeouts.DefaultTimeout
  private val locationServiceUtil                              = mock[LocationServiceUtil]

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(locationServiceUtil)
  }

  override def afterAll(): Unit = {
    super.afterAll()
    actorSystem.terminate()
  }

  "resolveSequencer" must {
    val subsystem      = TCS
    val observingMode  = "darknight"
    val prefix         = Prefix(subsystem, observingMode)
    val uri            = new URI("uri")
    val akkaConnection = AkkaConnection(ComponentId(prefix, Sequencer))
    val akkaLocation   = AkkaLocation(akkaConnection, uri)

    "return a location which matches a given subsystem and observing mode | ESW-119" in {
      when(locationServiceUtil.resolve(akkaConnection, Timeouts.DefaultTimeout))
        .thenReturn(Future.successful(Right(akkaLocation)))

      val componentFactory = new ComponentFactory(locationServiceUtil)

      componentFactory.resolveSequencer(subsystem, observingMode).rightValue shouldBe a[SequencerApi]
      verify(locationServiceUtil).resolve(akkaConnection, Timeouts.DefaultTimeout)
    }

    "return a ResolveLocationFailed when no matching subsystem and observing mode is found | ESW-119" in {
      when(locationServiceUtil.resolve(akkaConnection, 200.millis))
        .thenReturn(
          Future.successful(Left(ResolveLocationFailed(s"Could not resolve location matching connection: $akkaConnection")))
        )

      val componentFactory = new ComponentFactory(locationServiceUtil)
      componentFactory.resolveSequencer(subsystem, observingMode, 200.millis).leftValue shouldBe
      ResolveLocationFailed(s"Could not resolve location matching connection: $akkaConnection")
    }

    "return a RegistrationListingFailed when location service call throws exception | ESW-119" in {
      when(locationServiceUtil.resolve(akkaConnection, 200.millis))
        .thenReturn(Future.successful(Left(RegistrationListingFailed(s"Location Service Error: listing failed"))))

      val componentFactory = new ComponentFactory(locationServiceUtil)
      componentFactory.resolveSequencer(subsystem, observingMode, 200.millis).leftValue shouldBe
      RegistrationListingFailed(s"Location Service Error: listing failed")
    }
  }

  "resolveSeqComp" must {
    val subsystem      = TCS
    val componentName  = "primary"
    val prefix         = Prefix(subsystem, componentName)
    val uri            = new URI("uri")
    val akkaConnection = AkkaConnection(ComponentId(prefix, SequenceComponent))
    val akkaLocation   = AkkaLocation(akkaConnection, uri)

    "return a location which matches a given subsystem and observing mode | ESW-119" in {
      when(locationServiceUtil.resolve(akkaConnection, Timeouts.DefaultTimeout))
        .thenReturn(Future.successful(Right(akkaLocation)))

      val componentFactory = new ComponentFactory(locationServiceUtil)

      componentFactory.resolveSeqComp(prefix).rightValue shouldBe a[SequenceComponentImpl]
      verify(locationServiceUtil).resolve(akkaConnection, Timeouts.DefaultTimeout)
    }

    "return a ResolveLocationFailed when no matching location for prefix is found | ESW-119" in {
      when(locationServiceUtil.resolve(akkaConnection, Timeouts.DefaultTimeout))
        .thenReturn(
          Future.successful(Left(ResolveLocationFailed(s"Could not resolve location matching connection: $akkaConnection")))
        )

      val componentFactory = new ComponentFactory(locationServiceUtil)
      componentFactory.resolveSeqComp(prefix).leftValue shouldBe
      ResolveLocationFailed(s"Could not resolve location matching connection: $akkaConnection")
    }

    "return a RegistrationListingFailed when location service call throws exception | ESW-119" in {
      when(locationServiceUtil.resolve(akkaConnection, Timeouts.DefaultTimeout))
        .thenReturn(Future.successful(Left(RegistrationListingFailed(s"Location Service Error: listing failed"))))

      val componentFactory = new ComponentFactory(locationServiceUtil)
      componentFactory.resolveSeqComp(prefix).leftValue shouldBe
      RegistrationListingFailed(s"Location Service Error: listing failed")
    }
  }

  "findAgent" must {
    val subsystem      = TCS
    val uri            = new URI("uri")
    val akkaConnection = AkkaConnection(ComponentId(Prefix(subsystem, "agent"), Machine))
    val akkaLocation   = AkkaLocation(akkaConnection, uri)

    "return a location which matches a given subsystem and observing mode | ESW-119" in {
      when(locationServiceUtil.listAkkaLocationsBy(subsystem, Machine)).thenReturn(Future.successful(Right(List(akkaLocation))))

      val componentFactory = new ComponentFactory(locationServiceUtil) {
        override private[commons] def getAgentClient(locations: List[AkkaLocation]) = {
          Future.successful(mock[AgentClient])
        }
      }

      componentFactory.findAgent(subsystem).rightValue shouldBe a[AgentClient]
      verify(locationServiceUtil).listAkkaLocationsBy(subsystem, Machine)
    }

    "return a RegistrationListingFailed when locationUtil call returns error  | ESW-119" in {
      when(locationServiceUtil.listAkkaLocationsBy(subsystem, Machine))
        .thenReturn(Future.successful(Left(RegistrationListingFailed(s"Location Service Error: listing failed"))))

      val componentFactory = new ComponentFactory(locationServiceUtil) {
        override private[commons] def getAgentClient(locations: List[AkkaLocation]) = {
          Future.successful(mock[AgentClient])
        }
      }

      componentFactory.findAgent(subsystem).leftValue shouldBe
      RegistrationListingFailed(s"Location Service Error: listing failed")

      verify(locationServiceUtil).listAkkaLocationsBy(subsystem, Machine)
    }

    "return a ResolveLocationFailed when getAgentClient call returns error  | ESW-119" in {
      when(locationServiceUtil.listAkkaLocationsBy(subsystem, Machine)).thenReturn(Future.successful(Right(List(akkaLocation))))

      val componentFactory: ComponentFactory = new ComponentFactory(locationServiceUtil) {
        override private[commons] def getAgentClient(locations: List[AkkaLocation]): Future[AgentClient] = {
          Future.failed(new RuntimeException("Exception in getAgentClient"))
        }
      }
      componentFactory.findAgent(subsystem).leftValue shouldBe
      ResolveLocationFailed(s"Could not find agent matching $subsystem")

      verify(locationServiceUtil).listAkkaLocationsBy(subsystem, Machine)
    }

  }
}
