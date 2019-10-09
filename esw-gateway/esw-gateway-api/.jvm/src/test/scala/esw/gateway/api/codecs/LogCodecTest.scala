package esw.gateway.api.codecs

import csw.logging.models.Level
import esw.gateway.api.protocol.PostRequest.Log
import io.bullet.borer.Json
import org.scalatest.{Matchers, WordSpec}

class LogCodecTest extends WordSpec with Matchers with GatewayCodecs {

  "LogCodec" must {
    "decode json with nested metadata and filter null values" in {
      val json =
        """
          |{
          |  "Log": {
          |    "appName": "app1",
          |    "level": "debug",
          |    "message": "all good",
          |    "metadata": {
          |      "a": 1,
          |      "b": 11.12,
          |      "c": 45.234782634873,
          |      "d": "xyz",
          |      "e": [1,2,null,3],
          |      "new" : null,
          |      "f": {
          |        "g": true,
          |        "new2" : null
          |      }
          |    }
          |  }
          |}
          |""".stripMargin

      val actualLog = Json.decode(json.getBytes).to[Log].value

      val expectedLog = Log(
        "app1",
        Level.DEBUG,
        "all good",
        Map(
          "a" -> 1,
          "b" -> 11.12,
          "c" -> 45.234782634873,
          "d" -> "xyz",
          "e" -> List(1, 2, 3),
          "f" -> Map(
            "g" -> true
          )
        )
      )
      actualLog should ===(expectedLog)
    }
    "encode Log to json with nested metadata and filter null values" in {
      val logWithNulls = Log(
        "app1",
        Level.DEBUG,
        "all good",
        Map(
          "a"   -> 1,
          "b"   -> 11.12,
          "c"   -> 45.234782634873,
          "d"   -> "xyz",
          "new" -> null,
          "e"   -> List(1, 2, 3),
          "f" -> Map(
            "g"    -> true,
            "new2" -> null
          )
        )
      )

      val logWithoutNulls = Log(
        "app1",
        Level.DEBUG,
        "all good",
        Map(
          "a" -> 1,
          "b" -> 11.12,
          "c" -> 45.234782634873,
          "d" -> "xyz",
          "e" -> List(1, 2, 3),
          "f" -> Map(
            "g" -> true
          )
        )
      )
      val encodedLog = Json.encode(logWithNulls).toUtf8String.getBytes
      val actualLog  = Json.decode(encodedLog).to[Log].value
      actualLog should ===(logWithoutNulls)
    }
  }
}
