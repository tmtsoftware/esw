package esw.ocs.dsl.epic

import akka.actor.typed.{ActorSystem, SpawnProtocol}

object Demo {

  def main(args: Array[String]): Unit = {
    val cswSystem = ActorSystem(SpawnProtocol.behavior, "demo")
    new TemperatureProgram(cswSystem).refresh("init")
//    new RemoteRepl(cswSystem).server().start()
  }

}
