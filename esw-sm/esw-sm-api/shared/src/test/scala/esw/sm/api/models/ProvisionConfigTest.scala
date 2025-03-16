package esw.sm.api.models

import csw.prefix.models.Prefix
import csw.prefix.models.Subsystem.{ESW, IRIS}
import org.scalatest.matchers.must.Matchers
import org.scalatest.matchers.should.Matchers.shouldBe
import org.scalatest.wordspec.AnyWordSpec

import scala.util.Random

class ProvisionConfigTest extends AnyWordSpec with Matchers {

  "ProvisionConfig" must {
    "create provision config for valid config | ESW-347" in {
      val config = ProvisionConfig(Prefix(ESW, "primary") -> 2, Prefix(IRIS, "primary") -> 1)
      config.config.size shouldBe 2
    }

    "throw IllegalArgumentException if multiple entries with same agent prefix are given | ESW-347" in {
      val prefix = Prefix(ESW, "primary")
      intercept[IllegalArgumentException] {
        ProvisionConfig(prefix -> 2, Prefix(IRIS, "primary") -> 1, prefix -> 1)
      }
    }
  }

  "AgentProvisionConfig" must {
    "create agent provision config if sequence component count is above 0 | ESW-347" in {
      val config = ProvisionConfig(Prefix(ESW, "primary") -> 2, Prefix(IRIS, "primary") -> 1)
      config.config.size shouldBe 2
    }

    "throw IllegalArgumentException if given sequence component count is 0 or less | ESW-347" in {
      val prefix = Prefix(ESW, "primary")
      val count  = Random.between(-100, 0) // negative count
      intercept[IllegalArgumentException] { AgentProvisionConfig(prefix, count) }
    }

  }
}
