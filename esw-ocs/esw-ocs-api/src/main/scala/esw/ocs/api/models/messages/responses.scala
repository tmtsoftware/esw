package esw.ocs.api.models.messages

import akka.Done
import csw.location.models.AkkaLocation
import esw.ocs.api.models.StepList
import esw.ocs.api.serializer.OcsFrameworkAkkaSerializable

object SequencerResponses {
  final case class LoadSequenceResponse(response: Either[SequenceError, Done]) extends OcsFrameworkAkkaSerializable
  final case class EditorResponse(response: Either[EditorError, Done])         extends OcsFrameworkAkkaSerializable
  final case class LifecycleResponse(response: Either[LifecycleError, Done])   extends OcsFrameworkAkkaSerializable
  final case class StepListResponse(response: Option[StepList])                extends OcsFrameworkAkkaSerializable
}

object SequenceComponentResponses {
  final case class LoadScriptResponse(response: Either[RegistrationError, AkkaLocation]) extends OcsFrameworkAkkaSerializable
  final case class GetStatusResponse(response: Option[AkkaLocation])                     extends OcsFrameworkAkkaSerializable
}
