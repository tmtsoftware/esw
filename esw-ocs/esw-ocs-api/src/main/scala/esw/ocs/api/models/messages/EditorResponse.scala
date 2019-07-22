package esw.ocs.api.models.messages

import akka.Done
import esw.ocs.api.models.messages.error.EditorError
import esw.ocs.api.serializer.OcsFrameworkAkkaSerializable

case class EditorResponse(response: Either[EditorError, Done]) extends OcsFrameworkAkkaSerializable
