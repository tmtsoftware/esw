/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package esw.gateway.api.codecs

import csw.logging.models.Level
import csw.prefix.models.Prefix
import esw.gateway.api.protocol.GatewayRequest
import esw.gateway.api.protocol.GatewayRequest.Log
import io.bullet.borer.Json
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class LogCodecTest extends AnyWordSpec with Matchers with GatewayCodecs {

  "LogCodec" must {
    "decode json with nested metadata and filter null values" in {
      val json =
        """
          |{
          |     "_type": "Log",
          |    "prefix": "esw.app1",
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
          |}
          |""".stripMargin

      val actualLog = Json.decode(json.getBytes).to[GatewayRequest].value

      val expectedLog = Log(
        Prefix("esw.app1"),
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
        Prefix("esw.app1"),
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
        Prefix("esw.app1"),
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
      val encodedLog = Json.encode(logWithNulls: GatewayRequest).toUtf8String.getBytes
      val actualLog  = Json.decode(encodedLog).to[GatewayRequest].value
      actualLog should ===(logWithoutNulls)
    }
  }
}
