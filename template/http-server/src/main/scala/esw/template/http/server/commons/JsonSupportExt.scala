package esw.template.http.server.commons

import csw.params.commands.{CommandIssue, CommandResponse}
import csw.params.core.formats.JsonSupport
import de.heikoseeberger.akkahttpplayjson.PlayJsonSupport
import julienrf.json.derived
import play.api.libs.json.{OFormat, __}

trait JsonSupportExt extends JsonSupport with PlayJsonSupport {

  implicit lazy val commandIssueFormat: OFormat[CommandIssue]       = derived.flat.oformat((__ \ "type").format[String])
  implicit lazy val commandResponseFormat: OFormat[CommandResponse] = derived.flat.oformat((__ \ "type").format[String])

}
