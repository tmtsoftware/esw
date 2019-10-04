package esw.ocs.dsl.script.epic

import akka.actor.typed.ActorSystem
import esw.ocs.dsl.script.epic.TemperatureProgram.{High, Init, Ok, State}
import esw.ocs.dsl.script.epic.internal.{Machine, Var}

import scala.concurrent.duration.DurationLong

class TemperatureProgram(cswSystem: ActorSystem[_]) extends Program(cswSystem) {
//  val temp2: Var[Int] = Var.assign(0, "nfiraos.temp.updates", "temp")

  var x = 0

  setup(new Machine[State]("temp-monitor", Init) {

    val temp: Var[Int] = Var.assign(0, "nfiraos.temp.updates", "temp1")

    def logic: Logic = {
      case Init =>
        entry {
          temp.pvMonitor()
        }
        when(delay = 1.seconds) {
          become(Ok)
        }
      case Ok =>
        when(temp > 40) {
          temp := 25
          become(High)
        }
      case High =>
        when(temp < 30) {
          become(Ok)
        }
    }

    override def debugString: String = s"temp1 = $temp"
  })

  setup(new Machine[State]("temp-get", Init) {
    val temp: Var[Int] = Var.assign(0, "nfiraos.temp.updates", "temp2")

    override def logic: Logic = {
      case Init =>
        when() {
          temp := 45
          temp.pvPut()
          become(Ok)
        }
      case Ok =>
        when(temp > 40) {
          temp := 25
          become(High)
        }
      case High =>
        when(temp < 30) {
          temp.pvGet()
          become(Ok)
        }
    }

    override def debugString: String = s"temp2 = $temp"
  })
}

object TemperatureProgram {
  sealed trait State

  case object Init extends State
  case object Ok   extends State
  case object High extends State
}
