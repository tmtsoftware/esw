addSbtPlugin("com.timushev.sbt"                  % "sbt-updates"              % "0.5.1")
addSbtPlugin("net.virtual-void"                  % "sbt-dependency-graph"     % "0.10.0-RC1")
addSbtPlugin("io.spray"                          % "sbt-revolver"             % "0.9.1")
addSbtPlugin("org.scoverage"                     % "sbt-scoverage"            % "1.6.1")
addSbtPlugin("com.eed3si9n"                      % "sbt-buildinfo"            % "0.10.0")
addSbtPlugin("org.foundweekends"                 % "sbt-bintray"              % "0.5.6")
addSbtPlugin("org.scalameta"                     % "sbt-scalafmt"             % "2.4.2")
addSbtPlugin("com.dwijnand"                      % "sbt-dynver"               % "4.1.1")
addSbtPlugin("org.portable-scala"                % "sbt-scalajs-crossproject" % "1.0.0")
addSbtPlugin("org.scala-js"                      % "sbt-scalajs"              % "1.2.0")
addSbtPlugin("com.typesafe.sbt"                  % "sbt-multi-jvm"            % "0.4.0")
addSbtPlugin("com.timushev.sbt"                  % "sbt-rewarn"               % "0.1.1")
addSbtPlugin("com.thoughtworks.sbt-api-mappings" % "sbt-api-mappings"         % "3.0.0")

resolvers += Resolver.bintrayRepo("twtmt", "sbt-plugins")
addSbtPlugin("com.github.tmtsoftware" % "sbt-docs"      % "bd4e9c37e4")
addSbtPlugin("com.github.tmtsoftware" % "kotlin-plugin" % "2.1.0-M1")

resolvers += Resolver.jcenterRepo
addSbtPlugin("net.aichler" % "sbt-jupiter-interface" % "0.8.3")

scalacOptions ++= Seq(
  "-encoding",
  "UTF-8",
  "-feature",
  "-unchecked",
  "-deprecation",
  //"-Xfatal-warnings",
  "-Xlint:-unused,_",
  "-Ywarn-dead-code",
  "-Xfuture"
)
