package esw.sm.impl.utils

import java.net.URI

import csw.location.api.models.ComponentType.Machine
import csw.location.api.models.Connection.AkkaConnection
import csw.location.api.models.{AkkaLocation, ComponentId}
import csw.prefix.models.Prefix
import csw.prefix.models.Subsystem.{ESW, IRIS}
import esw.sm.api.models.ProvisionConfig
import esw.sm.api.protocol.ProvisionResponse.CouldNotFindMachines
import esw.testcommons.BaseTestSuite

class AgentAllocatorTest extends BaseTestSuite {
  private val uri           = new URI("test-uri")
  private val eswPrimaryM   = AkkaLocation(AkkaConnection(ComponentId(Prefix(ESW, "primary"), Machine)), uri)
  private val eswSecondaryM = AkkaLocation(AkkaConnection(ComponentId(Prefix(ESW, "secondary"), Machine)), uri)
  private val irisPrimaryM  = AkkaLocation(AkkaConnection(ComponentId(Prefix(IRIS, "primary"), Machine)), uri)

  val allocator = new AgentAllocator()

  "allocate" must {

    "return a mapping of machine -> seq comp prefix for given config | ESW-347" in {
      val config   = ProvisionConfig(Map(eswPrimaryM.prefix -> 3, eswSecondaryM.prefix -> 2, irisPrimaryM.prefix -> 2))
      val machines = List(eswPrimaryM, eswSecondaryM, irisPrimaryM)

      val mapping = allocator.allocate(config, machines).rightValue

      mapping should contain allElementsOf List(
        //-------- on ESW primary machine ----------
        eswPrimaryM -> Prefix(ESW, "ESW_1"),
        eswPrimaryM -> Prefix(ESW, "ESW_2"),
        eswPrimaryM -> Prefix(ESW, "ESW_3"),
        //-------- on ESW secondary machine --------
        eswSecondaryM -> Prefix(ESW, "ESW_4"),
        eswSecondaryM -> Prefix(ESW, "ESW_5"),
        //-------- on IRIS primary machine ---------
        irisPrimaryM -> Prefix(IRIS, "IRIS_1"),
        irisPrimaryM -> Prefix(IRIS, "IRIS_2")
      )
    }

    "return CouldNotFindMachines if there is not machine with given machine | ESW-347" in {
      val config = ProvisionConfig(Map(eswPrimaryM.prefix -> 1, eswSecondaryM.prefix -> 1, irisPrimaryM.prefix -> 1))

      allocator.allocate(config, List(eswPrimaryM)).leftValue should ===(
        CouldNotFindMachines(Set(eswSecondaryM.prefix, irisPrimaryM.prefix))
      )
    }
  }
}
