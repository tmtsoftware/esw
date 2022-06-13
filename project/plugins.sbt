addSbtPlugin("com.timushev.sbt"                  % "sbt-updates"              % "0.6.1")
addSbtPlugin("io.spray"                          % "sbt-revolver"             % "0.9.1")
addSbtPlugin("org.scoverage"                     % "sbt-scoverage"            % "1.9.3")
addSbtPlugin("com.eed3si9n"                      % "sbt-buildinfo"            % "0.10.0")
addSbtPlugin("org.scalameta"                     % "sbt-scalafmt"             % "2.4.6")
addSbtPlugin("org.portable-scala"                % "sbt-scalajs-crossproject" % "1.1.0")
addSbtPlugin("org.scala-js"                      % "sbt-scalajs"              % "1.8.0")
addSbtPlugin("com.typesafe.sbt"                  % "sbt-multi-jvm"            % "0.4.0")
addSbtPlugin("com.timushev.sbt"                  % "sbt-rewarn"               % "0.1.3")
addSbtPlugin("com.thoughtworks.sbt-api-mappings" % "sbt-api-mappings"         % "3.0.0")

resolvers += "jitpack" at "https://jitpack.io"
//libraryDependencies += "com.github.tmtsoftware" % "kotlin-plugin" % "24d598a"
libraryDependencies += "com.github.tmtsoftware" % "kotlin-plugin" % "d148956f8e0d0ef3b0a37c056cb083801006d172"
libraryDependencies += "com.github.tmtsoftware" % "sbt-docs"      % "115000a"

resolvers += Resolver.jcenterRepo
addSbtPlugin("net.aichler" % "sbt-jupiter-interface" % "0.9.1")

scalacOptions ++= Seq(
  "-encoding",
  "UTF-8",
  "-feature",
  "-unchecked",
  "-deprecation",
  // "-Xfatal-warnings",
  "-Xlint:-unused,_",
  "-Ywarn-dead-code",
  "-Xfuture"
)
