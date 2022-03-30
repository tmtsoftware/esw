/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package esw.commons.utils.location

/**
 * A common trait representing location Service Error that may occur in different service calls.
 */
sealed trait EswLocationError extends Exception {
  def msg: String
  override def getMessage: String = msg
}

object EswLocationError {
  sealed trait RegistrationError                    extends EswLocationError
  sealed trait FindLocationError                    extends EswLocationError
  case class LocationNotFound(msg: String)          extends FindLocationError
  case class RegistrationListingFailed(msg: String) extends FindLocationError
  case class RegistrationFailed(msg: String)        extends RegistrationError
  case class OtherLocationIsRegistered(msg: String) extends RegistrationError
}
