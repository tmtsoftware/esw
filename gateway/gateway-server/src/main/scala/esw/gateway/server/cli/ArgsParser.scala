package esw.gateway.server.cli

import scopt.OptionParser

/**
 * Parses the command line options using `scopt` library.
 */
class ArgsParser(name: String) {

  private val parser: OptionParser[Options] = new scopt.OptionParser[Options](name) {
    head(name)

    opt[Int]("port") action { (x, c) =>
      c.copy(port = Some(x))
    } text "Optional: Port at which the esw gateway server will start. Default is 6000"

    help("help")
  }

  def parse(args: Seq[String]): Option[Options] = parser.parse(args, Options())
}
