package esw.gateway.api.codecs

import csw.logging.models.Level
import esw.gateway.api.protocol.PostRequest.Log
import io.bullet.borer.Json
import org.scalatest.WordSpec

class ElementConverterTest extends WordSpec with GatewayCodecs {

  val map: Map[String, Any] = Map(
    "a"  -> 1,
    "a1" -> 11.234,
    "a2" -> 11.234f,
    "b"  -> "xyz",
    "b1"  -> null,
    "c"  -> List(1, 2, 3),
    "d"  -> Map("1" -> "11", "2" -> "22")
  )

  val data = Log("app1", Level.DEBUG, "all good", map)


  "demo" in {
    val string = Json.encode(data).toUtf8String
    println(string)
    val value = Json.decode(string.getBytes()).to[Log].value
    println(value)
  }

}
