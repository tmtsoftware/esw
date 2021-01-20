package esw.performance

import akka.actor.typed.{ActorSystem, SpawnProtocol}
import csw.location.api.models.ComponentType.Service
import csw.location.api.scaladsl.LocationService
import csw.location.client.ActorSystemFactory
import csw.location.client.scaladsl.HttpLocationServiceFactory
import csw.location.client.utils.LocationServerStatus
import csw.prefix.models.Prefix
import csw.prefix.models.Subsystem.ESW
import esw.ocs.api.models.ObsMode
import esw.ocs.testkit.utils.LocationUtils
import esw.sm.api.SequenceManagerApi
import esw.sm.api.actor.client.SequenceManagerApiFactory
import esw.sm.api.models.{AgentProvisionConfig, ProvisionConfig}

import scala.concurrent.duration.DurationInt

object SequenceManagerLatencyTest extends LocationUtils {

  override implicit def actorSystem: ActorSystem[SpawnProtocol.Command] = ActorSystemFactory.remote(SpawnProtocol(), "perf-test")
  override def locationService: LocationService                         = HttpLocationServiceFactory.makeLocalClient
  override implicit def patienceConfig: PatienceConfig                  = PatienceConfig(30.seconds, 50.millis)

  def main(args: Array[String]): Unit = {

    // Check Location server is up
    LocationServerStatus.requireUpLocally()

    // sm client
    val sequenceManagerPrefix = Prefix(ESW, "sequence_manager")
    val smLocation            = resolveAkkaLocation(sequenceManagerPrefix, Service)
    val smClient              = SequenceManagerApiFactory.makeAkkaClient(smLocation)

    // provision seq-componets
    // OCS, TCS, AOESW, IRIS
    val agentProvisionConfig = List(
      AgentProvisionConfig(Prefix("ESW.machine1"), 1),
      AgentProvisionConfig(Prefix("IRIS.machine1"), 1),
      AgentProvisionConfig(Prefix("TCS.machine1"), 1),
      AgentProvisionConfig(Prefix("AOESW.machine1"), 1)
    )
    val provisionConfig = ProvisionConfig(agentProvisionConfig)

    val provisionResponse = smClient.provision(provisionConfig).futureValue
    println(provisionResponse)

    // configure obsmode-1
    // OCS, TCS, AOESW, IRIS
    val deadline = 20.seconds.fromNow
    while (deadline.hasTimeLeft()) {
      scenario(smClient)
    }

    actorSystem.terminate()
  }

  private def scenario(smClient: SequenceManagerApi): Unit = {
    val obsmode1           = ObsMode("IRIS_Darknight")
    val configureResponse1 = smClient.configure(obsmode1).futureValue
    println(s"Configured ObsMode ${obsmode1} response ${configureResponse1}")

    Thread.sleep(500) //todo: async delay

    val time0             = System.currentTimeMillis()
    val shutdownResponse1 = smClient.shutdownObsModeSequencers(obsmode1).futureValue

    val obsmode2           = ObsMode("IRIS_Calib")
    val configureResponse2 = smClient.configure(obsmode2).futureValue
    println(s"Configured ObsMode ${obsmode2} response ${configureResponse2}")

    val time1 = System.currentTimeMillis()
    println("Shutdown + configure time: " + (time1 - time0))
    Thread.sleep(500) //todo: async delay

    val shutdownResponse2 = smClient.shutdownObsModeSequencers(obsmode2).futureValue

  }
}
