package esw.ocs.api.models.messages

import akka.Done
import esw.ocs.api.models.messages.error.EditorError

case class EditorResponse(response: Either[EditorError, Done])
