package esw.sm.impl.utils

import java.net.URI

import csw.location.api.models.ComponentType.Machine
import csw.location.api.models.Connection.AkkaConnection
import csw.location.api.models.{AkkaLocation, ComponentId}
import csw.prefix.models.Prefix
import csw.prefix.models.Subsystem.{ESW, IRIS}
import esw.sm.api.models.ProvisionConfig
import esw.sm.api.protocol.ProvisionResponse.NoMachineFoundForSubsystems
import esw.testcommons.BaseTestSuite

class AgentAllocatorTest extends BaseTestSuite {
  private val uri           = new URI("test-uri")
  private val eswPrimaryM   = AkkaLocation(AkkaConnection(ComponentId(Prefix(ESW, "primary"), Machine)), uri)
  private val eswSecondaryM = AkkaLocation(AkkaConnection(ComponentId(Prefix(ESW, "secondary"), Machine)), uri)
  private val irisPrimaryM  = AkkaLocation(AkkaConnection(ComponentId(Prefix(IRIS, "primary"), Machine)), uri)

  val allocator = new AgentAllocator()

  "allocate" must {
    "return a mapping of seq comp prefix -> machine on which it will be spawned | ESW-346" in {
      val config   = ProvisionConfig(Map(ESW -> 2, IRIS -> 1))
      val machines = List(eswPrimaryM, irisPrimaryM)
      val mapping  = allocator.allocate(config, machines).rightValue

      mapping.size shouldBe 3
      mapping should contain allElementsOf List(
        Prefix(ESW, "ESW_1")   -> eswPrimaryM,
        Prefix(ESW, "ESW_2")   -> eswPrimaryM,
        Prefix(IRIS, "IRIS_1") -> irisPrimaryM
      )
    }

    "distribute required number of sequence components on available machines equally | ESW-346" in {
      val config   = ProvisionConfig(Map(ESW -> 5, IRIS -> 2))
      val machines = List(eswPrimaryM, eswSecondaryM, irisPrimaryM)
      val mapping  = allocator.allocate(config, machines).rightValue

      mapping.size shouldBe 7
      mapping should contain allElementsOf List(
        //-------- on ESW primary machine ----------
        Prefix(ESW, "ESW_1") -> eswPrimaryM,
        Prefix(ESW, "ESW_3") -> eswPrimaryM,
        Prefix(ESW, "ESW_5") -> eswPrimaryM,
        //-------- on ESW secondary machine --------
        Prefix(ESW, "ESW_4") -> eswSecondaryM,
        Prefix(ESW, "ESW_2") -> eswSecondaryM,
        //-------- on IRIS primary machine ---------
        Prefix(IRIS, "IRIS_1") -> irisPrimaryM,
        Prefix(IRIS, "IRIS_2") -> irisPrimaryM
      )
    }

    "return NoMachineFoundForSubsystems if subsystem to provision does not have machine available | ESW-346" in {
      val config = ProvisionConfig(Map(ESW -> 1, IRIS -> 1))
      allocator.allocate(config, List(eswPrimaryM)).leftValue shouldBe NoMachineFoundForSubsystems(Set(IRIS))
    }
  }
}
