package app.http

import caseapp.{CommandName, ExtraName, HelpMessage}
import csw.prefix.models.Prefix

sealed trait ServerAppCommand

object ServerAppCommand {

  @CommandName("start")
  final case class StartCommand(
      @HelpMessage("port of the app")
      @ExtraName("p")
      port: Option[Int],
      @HelpMessage("prefix of app. For eg: tcs.sample_app, etc")
      @ExtraName("s")
      prefix: Option[Prefix]
  ) extends ServerAppCommand
}
