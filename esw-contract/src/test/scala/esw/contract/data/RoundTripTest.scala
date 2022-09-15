/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package esw.contract.data

import csw.contract.generator.{ModelType, RoundTrip}
import io.bullet.borer.{Cbor, Json}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers

class RoundTripTest extends AnyFreeSpec with Matchers {

  EswData.services.data.foreach { case (serviceName, service) =>
    s"$serviceName" - {
      "models" - {
        service.models.modelTypes.foreach { modelType =>
          modelType.name - {
            validate(modelType)
          }
        }
      }

      "http requests" - {
        service.`http-contract`.requests.modelTypes.foreach { modelType =>
          validate(modelType)
        }
      }

      "websocket requests" - {
        service.`websocket-contract`.requests.modelTypes.foreach { modelType =>
          validate(modelType)
        }
      }
    }
  }

  private def validate(modelType: ModelType[_]): Unit = {
    modelType.models.zipWithIndex.foreach { case (modelData, index) =>
      s"${modelData.getClass.getSimpleName.stripSuffix("$")}: $index" - {
        List(Json, Cbor).foreach { format =>
          s"$format | ESW-278, ESW-355, ESW-376" in {
            RoundTrip.roundTrip(modelData, modelType.codec, format) shouldBe modelData
          }
        }
      }
    }
  }
}
