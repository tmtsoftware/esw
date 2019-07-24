package esw.ocs.api.models.messages

import akka.Done
import esw.ocs.api.models.messages.error.LifecycleError
import esw.ocs.api.serializer.OcsFrameworkAkkaSerializable

case class LifecycleResponse(response: Either[LifecycleError, Done]) extends OcsFrameworkAkkaSerializable
