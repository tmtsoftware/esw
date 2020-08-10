package esw.agent.http

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

object AgentServiceApp extends App {

  private val httpWiring = new AgentHttpWiring(Some(4040))

  Await.result(httpWiring.httpService.startAndRegisterServer(), 10.seconds)
}
