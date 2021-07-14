package esw.agent.service

import csw.location.api.models.ComponentType.Service
import csw.location.api.models.Metadata
import csw.prefix.models.Prefix
import csw.prefix.models.Subsystem.ESW
import esw.agent.service.app.AgentServiceApp
import esw.ocs.testkit.EswTestKit
import esw.sm.app.TestSetup
import esw.ocs.testkit.Service.AAS

class AgentServiceIntegrationTest extends EswTestKit(AAS) {
  "AgentService" must {

    "start agent service on given port | ESW-532" in {
      val agentPrefix        = Prefix(ESW, "agent1")
      val agentServicePrefix = Prefix(ESW, "agent_service")
      // resolving sequence manager fails for Akka and Http
      intercept[Exception](resolveAkkaLocation(agentServicePrefix, Service))
      intercept[Exception](resolveHTTPLocation(agentServicePrefix, Service))

      // ESW-173 Start agent service using command line arguments without any other ESW dependency
      AgentServiceApp.main(Array("start", "-p", "9999"))

      val agentServiceHttpLocation = resolveHTTPLocation(agentServicePrefix, Service)
      agentServiceHttpLocation.prefix shouldBe agentServicePrefix
      agentServiceHttpLocation.uri.getPort shouldBe 9999

      TestSetup.cleanup()
    }
  }
}
