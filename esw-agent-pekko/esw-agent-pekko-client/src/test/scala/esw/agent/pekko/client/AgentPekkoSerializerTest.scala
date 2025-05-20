//package esw.agent.pekko.client
//
//import org.scalatest.funsuite.AnyFunSuiteLike
//import esw.agent.service.api.models.{AgentResponse, Failed}
//import org.scalatest.BeforeAndAfterAll
//
// XXX TODO FIXME
//class AgentPekkoSerializerTest extends AnyFunSuiteLike {
//
//  test("Should serialize and deserialize") {
//    val s = AgentPekkoSerializer()
//    import s.*
//    val t: AgentResponse = Failed("test")
//    val b                = toBinary(t)
//    try {
//      fromBinary(b, Some(Failed.getClass)).asInstanceOf[Failed]
//    }
//    catch {
//      case e: Exception =>
//        e.printStackTrace()
//    }
//  }
//}
