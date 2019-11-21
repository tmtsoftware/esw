package esw.ocs.dsl.epics

data class InvalidStateException(val name: String) : RuntimeException("Failed transition to invalid state: $name")