package esw.ocs.script.server

import csw.command.client.messages.DiagnosticDataMessage.DiagnosticMode
import csw.params.commands.SequenceCommand
import csw.params.core.formats.JsonSupport
import csw.time.core.models.UTCTime
import esw.ocs.impl.script.ScriptApi
import org.apache.pekko.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import spray.json.*
import java.time.Instant
import ScriptJsonSupport.*

private[ocs] object ScriptJsonSupport {
  case class ExceptionMessage(msg: String)
}

private[ocs] trait ScriptJsonSupport extends SprayJsonSupport with DefaultJsonProtocol with JsonSupport {

  implicit object sequenceCommandFormat extends RootJsonFormat[SequenceCommand] {
    def write(obj: SequenceCommand): JsValue = writeSequenceCommand(obj).toString.parseJson

    def read(json: JsValue): SequenceCommand = json match {
      case js: JsObject =>
        val sc: SequenceCommand = readSequenceCommand(play.api.libs.json.Json.parse(js.compactPrint))
        sc
      case x =>
        throw DeserializationException("SequenceCommand expected")
    }
  }

  implicit object instantFormat extends RootJsonFormat[Instant] {
    def write(obj: Instant): JsValue = JsString(obj.toString)

    def read(json: JsValue): Instant = json match {
      case JsString(s) =>
        Instant.parse(s)
      case x =>
        throw DeserializationException("Instant expected")
    }
  }

  implicit val utcTimeFormat: RootJsonFormat[UTCTime]               = jsonFormat1(UTCTime.apply)
  implicit val diagnosticModeFormat: RootJsonFormat[DiagnosticMode] = jsonFormat2(DiagnosticMode.apply)
}
