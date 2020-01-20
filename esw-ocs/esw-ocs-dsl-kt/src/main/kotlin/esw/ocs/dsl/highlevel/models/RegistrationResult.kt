package esw.ocs.dsl.highlevel.models

import csw.location.models.Location

data class RegistrationResult(val location: Location, val unregister: () -> Unit)