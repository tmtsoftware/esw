package esw.gateway.api.exceptions

import csw.prefix.models.Prefix

class UnresolvedAkkaLocationException(prefix: Prefix)
    extends RuntimeException(s"Could not resolve $prefix to a valid Akka location")
