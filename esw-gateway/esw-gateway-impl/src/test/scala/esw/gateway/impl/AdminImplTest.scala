package esw.gateway.impl

import java.net.URI

import akka.actor.testkit.typed.scaladsl.ActorTestKit
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import csw.command.client.messages.RunningMessage.Lifecycle
import csw.command.client.messages.SupervisorContainerCommonMessages.{Restart, Shutdown}
import csw.command.client.messages.{GetComponentLogMetadata, SetComponentLogLevel}
import csw.command.client.models.framework.ToComponentLifecycleMessage.{GoOffline, GoOnline}
import csw.location.api.models
import csw.location.api.models.Connection.AkkaConnection
import csw.location.api.models.{AkkaLocation, ComponentId, ComponentType, Metadata}
import csw.location.api.scaladsl.LocationService
import csw.logging.models.{Level, LogMetadata}
import csw.prefix.models.{Prefix, Subsystem}
import esw.gateway.api.protocol.InvalidComponent
import esw.testcommons.BaseTestSuite

import scala.concurrent.duration.DurationLong
import scala.concurrent.{Await, Future}
import scala.util.Random

class AdminImplTest extends BaseTestSuite {
  lazy val actorTestKit: ActorTestKit      = ActorTestKit()
  lazy implicit val system: ActorSystem[_] = actorTestKit.system

  override def afterAll(): Unit = {
    super.afterAll()
    actorTestKit.shutdownTestKit()
  }

  //Story number not added in test names as its only code movement from csw to esw
  "getLogMetadata" must {
    "get log metadata when component is discovered and it responds with metadata" in {
      val locationService: LocationService = mock[LocationService]
      val expectedLogMetadata              = LogMetadata(Level.FATAL, Level.ERROR, Level.WARN, Level.DEBUG)
      val probe = actorTestKit.spawn(Behaviors.receiveMessage[GetComponentLogMetadata] {
        case GetComponentLogMetadata(replyTo) =>
          replyTo ! expectedLogMetadata
          Behaviors.same
      })
      val adminService: AdminImpl = new AdminImpl(locationService)
      val componentId             = ComponentId(Prefix(Subsystem.AOESW, "test_component"), ComponentType.Assembly)
      when(locationService.find(AkkaConnection(componentId))).thenReturn(
        Future.successful(Some(AkkaLocation(AkkaConnection(componentId), new URI(probe.path.toString), Metadata.empty)))
      )
      val actualLogMetadata = adminService.getLogMetadata(componentId).futureValue
      actualLogMetadata shouldBe expectedLogMetadata
    }

    "get log metadata when sequencer is discovered and it responds with metadata" in {
      val locationService: LocationService = mock[LocationService]
      val expectedLogMetadata              = LogMetadata(Level.FATAL, Level.ERROR, Level.WARN, Level.DEBUG)
      val probe = actorTestKit.spawn(Behaviors.receiveMessage[GetComponentLogMetadata] {
        case GetComponentLogMetadata(replyTo) =>
          replyTo ! expectedLogMetadata
          Behaviors.same
      })
      val adminService: AdminImpl = new AdminImpl(locationService)
      val componentId             = models.ComponentId(Prefix(Subsystem.AOESW, "test_sequencer"), ComponentType.Sequencer)
      when(locationService.find(AkkaConnection(componentId)))
        .thenReturn(
          Future.successful(Some(models.AkkaLocation(AkkaConnection(componentId), new URI(probe.path.toString), Metadata.empty)))
        )
      val actualLogMetadata = adminService.getLogMetadata(componentId).futureValue
      actualLogMetadata shouldBe expectedLogMetadata
    }

    "fail with InvalidComponent when componentId is not not resolved" in {
      val locationService: LocationService = mock[LocationService]
      val adminService: AdminImpl          = new AdminImpl(locationService)
      val componentId                      = models.ComponentId(Prefix(Subsystem.AOESW, "test_sequencer"), ComponentType.Sequencer)
      when(locationService.find(AkkaConnection(componentId))).thenReturn(Future.successful(None))
      val invalidComponent = intercept[InvalidComponent] {
        Await.result(adminService.getLogMetadata(componentId), 100.millis)
      }

      invalidComponent shouldBe InvalidComponent(s"Could not find component : $componentId")

    }
  }

  "setLogLevel" must {
    "get log metadata when component is discovered" in {
      val locationService: LocationService = mock[LocationService]
      val probe                            = actorTestKit.createTestProbe[SetComponentLogLevel]()
      val adminService: AdminImpl          = new AdminImpl(locationService)
      val componentId                      = models.ComponentId(Prefix(Subsystem.AOESW, "test_component"), ComponentType.Assembly)
      when(locationService.find(AkkaConnection(componentId))).thenReturn(
        Future.successful(
          Some(models.AkkaLocation(AkkaConnection(componentId), new URI(probe.ref.path.toString), Metadata.empty))
        )
      )
      adminService.setLogLevel(componentId, Level.FATAL)
      probe.expectMessage(500.millis, SetComponentLogLevel(Level.FATAL))
    }

    "get log metadata when sequencer is discovered" in {
      val locationService: LocationService = mock[LocationService]
      val probe                            = actorTestKit.createTestProbe[SetComponentLogLevel]()
      val adminService: AdminImpl          = new AdminImpl(locationService)
      val componentId                      = models.ComponentId(Prefix(Subsystem.AOESW, "test_component"), ComponentType.Sequencer)
      when(locationService.find(AkkaConnection(componentId))).thenReturn(
        Future.successful(
          Some(models.AkkaLocation(AkkaConnection(componentId), new URI(probe.ref.path.toString), Metadata.empty))
        )
      )
      adminService.setLogLevel(componentId, Level.FATAL)
      probe.expectMessage(500.millis, SetComponentLogLevel(Level.FATAL))
    }

    "fail with InvalidComponent when componentId is not not resolved" in {
      val locationService: LocationService = mock[LocationService]
      val adminService: AdminImpl          = new AdminImpl(locationService)
      val componentId                      = models.ComponentId(Prefix(Subsystem.AOESW, "test_component"), ComponentType.Sequencer)
      when(locationService.find(AkkaConnection(componentId))).thenReturn(Future.successful(None))
      val invalidComponent = intercept[InvalidComponent] {
        Await.result(adminService.setLogLevel(componentId, Level.FATAL), 100.millis)
      }

      invalidComponent shouldBe InvalidComponent(s"Could not find component : $componentId")
    }

  }

  "shutdown" must {
    val rnd            = new Random
    val componentTypes = Array(ComponentType.Assembly, ComponentType.HCD, ComponentType.Container)
    val componentId    = ComponentId(Prefix(Subsystem.AOESW, "test_component"), componentTypes(rnd.nextInt(componentTypes.length)))

    "send Shutdown message to the given component when it is running | ESW-378" in {
      val locationService: LocationService = mock[LocationService]
      val probe                            = actorTestKit.createTestProbe[Shutdown.type]()
      val adminService: AdminImpl          = new AdminImpl(locationService)

      when(locationService.find(AkkaConnection(componentId))).thenReturn(
        Future.successful(
          Some(models.AkkaLocation(AkkaConnection(componentId), new URI(probe.ref.path.toString), Metadata.empty))
        )
      )

      adminService.shutdown(componentId)
      probe.expectMessage(500.millis, Shutdown)
    }

    "fail with InvalidComponent when componentId is not not resolved | ESW-378" in {
      val locationService: LocationService = mock[LocationService]
      val adminService: AdminImpl          = new AdminImpl(locationService)
      when(locationService.find(AkkaConnection(componentId))).thenReturn(Future.successful(None))
      val invalidComponent = intercept[InvalidComponent] {
        Await.result(adminService.shutdown(componentId), 100.millis)
      }

      invalidComponent shouldBe InvalidComponent(s"Could not find component : $componentId")
    }
  }

  "restart" must {
    val rnd            = new Random
    val componentTypes = Array(ComponentType.Assembly, ComponentType.HCD, ComponentType.Container)
    val componentId    = ComponentId(Prefix(Subsystem.AOESW, "test_component"), componentTypes(rnd.nextInt(componentTypes.length)))

    "send Restart message to the given component when it is running | ESW-378" in {
      val locationService: LocationService = mock[LocationService]
      val probe                            = actorTestKit.createTestProbe[Restart.type]()
      val adminService: AdminImpl          = new AdminImpl(locationService)

      when(locationService.find(AkkaConnection(componentId))).thenReturn(
        Future.successful(
          Some(models.AkkaLocation(AkkaConnection(componentId), new URI(probe.ref.path.toString), Metadata.empty))
        )
      )

      adminService.restart(componentId)
      probe.expectMessage(500.millis, Restart)
    }

    "fail with InvalidComponent when componentId is not not resolved | ESW-378" in {
      val locationService: LocationService = mock[LocationService]
      val adminService: AdminImpl          = new AdminImpl(locationService)
      when(locationService.find(AkkaConnection(componentId))).thenReturn(Future.successful(None))
      val invalidComponent = intercept[InvalidComponent] {
        Await.result(adminService.restart(componentId), 100.millis)
      }

      invalidComponent shouldBe InvalidComponent(s"Could not find component : $componentId")
    }
  }

  "goOffline" must {
    val rnd            = new Random
    val componentTypes = Array(ComponentType.Assembly, ComponentType.HCD, ComponentType.Container)
    val componentId    = ComponentId(Prefix(Subsystem.AOESW, "test_component"), componentTypes(rnd.nextInt(componentTypes.length)))

    "send GoOffline message to the given component when it is running | ESW-378" in {
      val locationService: LocationService = mock[LocationService]
      val probe                            = actorTestKit.createTestProbe[Lifecycle]()
      val adminService: AdminImpl          = new AdminImpl(locationService)

      when(locationService.find(AkkaConnection(componentId))).thenReturn(
        Future.successful(
          Some(models.AkkaLocation(AkkaConnection(componentId), new URI(probe.ref.path.toString), Metadata.empty))
        )
      )

      adminService.goOffline(componentId)
      probe.expectMessage(500.millis, Lifecycle(GoOffline))
    }

    "fail with InvalidComponent when componentId is not not resolved | ESW-378" in {
      val locationService: LocationService = mock[LocationService]
      val adminService: AdminImpl          = new AdminImpl(locationService)
      when(locationService.find(AkkaConnection(componentId))).thenReturn(Future.successful(None))
      val invalidComponent = intercept[InvalidComponent] {
        Await.result(adminService.goOffline(componentId), 100.millis)
      }

      invalidComponent shouldBe InvalidComponent(s"Could not find component : $componentId")
    }
  }

  "goOnline" must {
    val rnd            = new Random
    val componentTypes = Array(ComponentType.Assembly, ComponentType.HCD, ComponentType.Container)
    val componentId    = ComponentId(Prefix(Subsystem.AOESW, "test_component"), componentTypes(rnd.nextInt(componentTypes.length)))

    "send GoOffline message to the given component when it is running | ESW-378" in {
      val locationService: LocationService = mock[LocationService]
      val probe                            = actorTestKit.createTestProbe[Lifecycle]()
      val adminService: AdminImpl          = new AdminImpl(locationService)

      when(locationService.find(AkkaConnection(componentId))).thenReturn(
        Future.successful(
          Some(models.AkkaLocation(AkkaConnection(componentId), new URI(probe.ref.path.toString), Metadata.empty))
        )
      )

      adminService.goOnline(componentId)
      probe.expectMessage(500.millis, Lifecycle(GoOnline))
    }

    "fail with InvalidComponent when componentId is not not resolved | ESW-378" in {
      val locationService: LocationService = mock[LocationService]
      val adminService: AdminImpl          = new AdminImpl(locationService)
      when(locationService.find(AkkaConnection(componentId))).thenReturn(Future.successful(None))
      val invalidComponent = intercept[InvalidComponent] {
        Await.result(adminService.goOnline(componentId), 100.millis)
      }

      invalidComponent shouldBe InvalidComponent(s"Could not find component : $componentId")
    }
  }

}
