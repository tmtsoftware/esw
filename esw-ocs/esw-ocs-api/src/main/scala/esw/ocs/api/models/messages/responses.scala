package esw.ocs.api.models.messages

import akka.Done
import csw.command.client.messages.sequencer.SequenceError
import esw.ocs.api.models.messages.error.{EditorError, LifecycleError}
import esw.ocs.api.serializer.OcsFrameworkAkkaSerializable

final case class LoadSequenceResponse(response: Either[SequenceError, Done]) extends OcsFrameworkAkkaSerializable

final case class EditorResponse(response: Either[EditorError, Done]) extends OcsFrameworkAkkaSerializable

final case class LifecycleResponse(response: Either[LifecycleError, Done]) extends OcsFrameworkAkkaSerializable
