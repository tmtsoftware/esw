package esw.agent.service.app

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

object AgentServiceApp extends App {

  private val httpWiring = new AgentServiceWiring(Some(4040))

  Await.result(httpWiring.start(), 10.seconds)
}
