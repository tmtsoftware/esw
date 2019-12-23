package agent

import caseapp.{CommandName, HelpMessage}

sealed trait AgentCliCommand

object AgentCliCommand {
  @CommandName("start")
  final case class StartCommand(
      @HelpMessage(
        "Optional port at which Akka cluster will run. " +
          "If a value is not provided, it will be picked up from configuration"
      )
      clusterPort: Option[Int],
      @HelpMessage(
        "Interface to bind http location server. " +
          "For security reasons, default value is set to 127.0.0.1. " +
          "Using another interface will expose the service to other hosts in the network"
      )
      interface: String = "127.0.0.1",
      @HelpMessage(
        "if set to true, this will disable all security for local http server. " +
          "Use with caution. " +
          "Note: This can only be set to true if `interface` is set to `127.0.0.1`"
      )
      unsecured: Boolean = false,
      @HelpMessage(
        "if set to true, agent will kill all the processes it has started on SIGTERM"
      )
      devMode: Boolean = false
  ) extends AgentCliCommand {
    if (unsecured && interface != "127.0.0.1") {
      Console.err.println("unsecured server can only run on loopback address")
      sys.exit(1)
    }
  }
}
