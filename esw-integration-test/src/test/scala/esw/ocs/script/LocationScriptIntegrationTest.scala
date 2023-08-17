package esw.ocs.script

import org.apache.pekko.actor.testkit.typed.scaladsl.TestProbe
import csw.location.api.models.{HttpLocation, LocationRemoved, LocationUpdated}
import csw.location.api.scaladsl.RegistrationResult
import csw.params.commands.{CommandName, Sequence, Setup}
import csw.params.core.generics.KeyType.StringKey
import csw.params.events.EventKey
import csw.prefix.models.Prefix
import csw.prefix.models.Subsystem.ESW
import csw.testkit.scaladsl.CSWService.EventServer
import esw.ocs.api.SequencerApi
import esw.ocs.api.models.ObsMode
import esw.ocs.testkit.EswTestKit

import scala.reflect.ClassTag

class LocationScriptIntegrationTest extends EswTestKit(EventServer) {
  private lazy val sequencer: SequencerApi = spawnSequencerProxy(ESW, ObsMode("locationScript"))

  "Script should have access to location service DSL | ESW-277" in {
    val locationKey              = StringKey.make("locationResponse")
    val locationResponseEventKey = EventKey("iris.motor.location_response")

    val probe = TestProbe[String]()

    eventSubscriber
      .subscribeCallback(
        Set(locationResponseEventKey),
        event => { probe.ref ! event.paramType(locationKey).head }
      )

    val prefix              = Prefix("iris.motor")
    val trackAndRegisterCmd = Setup(prefix, CommandName("track-and-register"), None)
    val resolveCmd          = Setup(prefix, CommandName("resolve"), None)
    val listCmd             = Setup(prefix, CommandName("list-by-prefix"), None)
    val unregisterCmd       = Setup(prefix, CommandName("unregister"), None)

    val sequence = Sequence(trackAndRegisterCmd, resolveCmd, listCmd, unregisterCmd)

    sequencer.submit(sequence)

    probe.expectMessage(className[RegistrationResult])
    probe.expectMessage(className[LocationUpdated])
    probe.expectMessage(className[HttpLocation])
    probe.expectMessage("Found = 1 Locations")
    probe.expectMessage("Unregistered")
    probe.expectMessage(className[LocationRemoved])
  }

  private def className[T: ClassTag] = scala.reflect.classTag[T].runtimeClass.getSimpleName

}
