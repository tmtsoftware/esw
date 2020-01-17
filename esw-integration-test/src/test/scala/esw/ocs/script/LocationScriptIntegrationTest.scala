package esw.ocs.script

import csw.location.api.scaladsl.RegistrationResult
import csw.location.models.{HttpLocation, LocationRemoved, LocationUpdated}
import csw.params.commands.{CommandName, Sequence, Setup}
import csw.params.core.generics.KeyType.StringKey
import csw.params.events.EventKey
import csw.prefix.models.Prefix
import csw.prefix.models.Subsystem.ESW
import esw.ocs.api.SequencerApi
import esw.ocs.testkit.EswTestKit
import esw.ocs.testkit.Service.EventServer

import scala.collection.mutable
import scala.reflect.ClassTag

class LocationScriptIntegrationTest extends EswTestKit(EventServer) {
  private lazy val sequencer: SequencerApi = spawnSequencerProxy(ESW, "locationScript")

  "Script should have access to location service DSL | ESW-277" in {
    val locationKey              = StringKey.make("locationResponse")
    val locationResponseEventKey = EventKey("iris.motor.location_response")

    val actualResponses = mutable.Set.empty[String]

    eventSubscriber
      .subscribeCallback(
        Set(locationResponseEventKey),
        event => { actualResponses.add(event.paramType(locationKey).head) }
      )

    val prefix        = Prefix("iris.motor")
    val registerCmd   = Setup(prefix, CommandName("track-and-register"), None)
    val resolveCmd    = Setup(prefix, CommandName("resolve"), None)
    val listCmd       = Setup(prefix, CommandName("list-by-prefix"), None)
    val unregisterCmd = Setup(prefix, CommandName("unregister"), None)

    val sequence = Sequence(registerCmd, resolveCmd, listCmd, unregisterCmd)

    sequencer.submit(sequence)

    eventually(
      actualResponses shouldBe Set(
        className[RegistrationResult],
        className[LocationUpdated],
        className[HttpLocation],
        "Found = 1 Locations",
        "Unregistered",
        className[LocationRemoved]
      )
    )
  }

  private def className[T: ClassTag] = scala.reflect.classTag[T].runtimeClass.getSimpleName

}
