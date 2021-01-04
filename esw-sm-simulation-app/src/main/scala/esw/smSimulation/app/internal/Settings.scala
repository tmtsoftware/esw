package esw.smSimulation.app.internal

import com.typesafe.config.ConfigFactory
import csw.network.utils.Networks

case class Settings(
    clusterPort: String,
    locationHttpPort: String,
    interfaceName: String,
    outsideInterfaceName: String,
    logHome: String
) {
  val hostName: String = Networks.interface(Some(interfaceName)).hostname
}

object Settings {
  def apply(interface: Option[String] = None, outsideInterface: Option[String] = None): Settings = {
    val config = ConfigFactory.load().getConfig("csw")

    // automatically determine correct interface if INTERFACE_NAME env variable not set or -i command line option not provided
    val interfaceName        = interface.getOrElse((sys.env ++ sys.props).getOrElse("INTERFACE_NAME", ""))
    val outsideInterfaceName = outsideInterface.getOrElse(interfaceName)

    new Settings(
      config.getString("clusterPort"),
      config.getString("locationHttpPort"),
      interfaceName,
      outsideInterfaceName,
      config.getString("logHome")
    )
  }
}
