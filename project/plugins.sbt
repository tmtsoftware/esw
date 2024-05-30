addSbtPlugin("com.timushev.sbt"                  % "sbt-updates"               % "0.6.4")
addSbtPlugin("io.spray"                          % "sbt-revolver"              % "0.10.0")
addSbtPlugin("org.scoverage"                     % "sbt-scoverage"             % "2.0.12")
addSbtPlugin("com.eed3si9n"                      % "sbt-buildinfo"             % "0.12.0")
addSbtPlugin("org.scalameta"                     % "sbt-scalafmt"              % "2.5.2")
addSbtPlugin("org.portable-scala"                % "sbt-scalajs-crossproject"  % "1.3.2")
addSbtPlugin("org.scala-js"                      % "sbt-scalajs"               % "1.16.0")
addSbtPlugin("com.typesafe.sbt"                  % "sbt-multi-jvm"             % "0.4.0")
addSbtPlugin("com.timushev.sbt"                  % "sbt-rewarn"                % "0.1.3")
addSbtPlugin("com.thoughtworks.sbt-api-mappings" % "sbt-api-mappings"          % "3.0.2")
addSbtPlugin("com.dwijnand"                      % "sbt-project-graph"        % "0.4.0")

ThisBuild / libraryDependencySchemes += "org.scala-lang.modules" %% "scala-xml" % VersionScheme.Always

resolvers += "jitpack" at "https://jitpack.io"

libraryDependencies += "com.github.tmtsoftware" % "kotlin-plugin" % "4431819"

libraryDependencies += "com.github.tmtsoftware" % "sbt-docs" % "58a91e5"

resolvers += Resolver.jcenterRepo
addSbtPlugin("net.aichler" % "sbt-jupiter-interface" % "0.11.1")

scalacOptions ++= Seq(
  "-encoding",
  "UTF-8",
  "-feature",
  "-unchecked",
  "-deprecation",
  "-Xlint:-unused,_",
  "-Ywarn-dead-code",
  "-Xfuture"
)

classpathTypes += "maven-plugin"
