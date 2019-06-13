import sbt.Keys.{version, _}
import sbt._

object UnidocSite extends AutoPlugin {
  import sbtunidoc.ScalaUnidocPlugin
  import ScalaUnidocPlugin.autoImport._
  import com.typesafe.sbt.site.SitePlugin.autoImport._

  override def requires: Plugins = ScalaUnidocPlugin

  override def projectSettings: Seq[Setting[_]] = Seq(
    siteSubdirName in ScalaUnidoc := "/api/scala",
    addMappingsToSiteDir(mappings in (ScalaUnidoc, packageDoc), siteSubdirName in ScalaUnidoc),
    autoAPIMappings := true
  )
}

object ParadoxSite extends AutoPlugin {
  import com.typesafe.sbt.site.paradox.ParadoxSitePlugin
  import ParadoxSitePlugin.autoImport._
  import _root_.io.github.jonas.paradox.material.theme.ParadoxMaterialThemePlugin
  import ParadoxMaterialThemePlugin.autoImport._
  import com.lightbend.paradox.sbt.ParadoxPlugin.autoImport._

  override def requires: Plugins = ParadoxSitePlugin && ParadoxMaterialThemePlugin

  override def projectSettings: Seq[Setting[_]] = {
    ParadoxMaterialThemePlugin.paradoxMaterialThemeSettings(Paradox) ++
    Seq(
      sourceDirectory in Paradox := baseDirectory.value / "src" / "main",
      sourceDirectory in (Paradox, paradoxTheme) := (sourceDirectory in Paradox).value / "_template",
      paradoxMaterialTheme in Paradox ~= {
        _.withFavicon("assets/tmt_favicon.ico")
          .withRepository(new URI(EswKeys.homepageValue))
      },
      paradoxProperties in Paradox ++= Map(
        "version"             → version.value,
        "scala.binaryVersion" → scalaBinaryVersion.value,
        "scaladoc.base_url"   → s"https://tmtsoftware.github.io/${EswKeys.projectName}/${version.value}/api/scala",
        "github.base_url"     → githubBaseUrl(version.value)
      )
    )
  }

  private def githubBaseUrl(version: String) = {
    val baseRepoUrl = s"${EswKeys.homepageValue}/tree"
    if (version == "0.1-SNAPSHOT") s"$baseRepoUrl/master"
    else s"$baseRepoUrl/v$version"
  }
}
