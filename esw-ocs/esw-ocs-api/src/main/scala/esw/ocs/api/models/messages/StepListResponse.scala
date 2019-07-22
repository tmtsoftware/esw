package esw.ocs.api.models.messages

import esw.ocs.api.models.StepList
import esw.ocs.api.serializer.OcsFrameworkAkkaSerializable

case class StepListResponse(response: Option[StepList]) extends OcsFrameworkAkkaSerializable
