package esw.gateway.impl

import csw.logging.client.scaladsl.LoggerFactory
import csw.prefix.models.Prefix
import csw.prefix.models.Subsystem.ESW

private[gateway] object AdminLogger extends LoggerFactory(Prefix(ESW, "esw_admin_impl"))
