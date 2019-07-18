package esw.http.core.cli

import com.typesafe.config.ConfigFactory
import esw.http.core.BuildInfo
import scopt.OptionParser

/**
 * Parses the command line options using `scopt` library.
 */
class ArgsParser(name: String) {

  private val parser: OptionParser[Options] = new scopt.OptionParser[Options](name) {
    head(name, BuildInfo.version)

    private val defaultPort: Int = ConfigFactory.load().getInt("http-server.port")
    opt[Int]("port") action { (x, c) =>
      c.copy(port = Some(x))
    } text s"Optional: Port at which the esw gateway server will start. Default is $defaultPort"

    help("help")
  }

  def parse(args: Seq[String]): Option[Options] = parser.parse(args, Options())
}
