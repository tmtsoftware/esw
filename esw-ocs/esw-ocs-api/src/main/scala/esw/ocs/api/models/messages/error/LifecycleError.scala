package esw.ocs.api.models.messages.error

import esw.ocs.api.serializer.OcsFrameworkAkkaSerializable

sealed trait LifecycleError extends OcsFrameworkAkkaSerializable

final case class ShutdownError(msg: String) extends LifecycleError
final case class AbortError(msg: String)    extends LifecycleError
