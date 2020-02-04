addSbtPlugin("com.timushev.sbt"      % "sbt-updates"              % "0.5.0")
addSbtPlugin("net.virtual-void"      % "sbt-dependency-graph"     % "0.10.0-RC1")
addSbtPlugin("io.spray"              % "sbt-revolver"             % "0.9.1")
addSbtPlugin("org.scoverage"         % "sbt-scoverage"            % "1.6.1")
addSbtPlugin("com.typesafe.sbt"      % "sbt-native-packager"      % "1.5.2")
addSbtPlugin("com.eed3si9n"          % "sbt-buildinfo"            % "0.9.0")
addSbtPlugin("org.foundweekends"     % "sbt-bintray"              % "0.5.5")
addSbtPlugin("org.scalameta"         % "sbt-scalafmt"             % "2.3.0")
addSbtPlugin("com.dwijnand"          % "sbt-dynver"               % "4.0.0")
addSbtPlugin("org.portable-scala"    % "sbt-scalajs-crossproject" % "0.6.1")
addSbtPlugin("org.scala-js"          % "sbt-scalajs"              % "0.6.31")
addSbtPlugin("com.typesafe.sbt"      % "sbt-multi-jvm"            % "0.4.0")
addSbtPlugin("com.lightbend.paradox" % "sbt-paradox"              % "0.6.8")

resolvers += "Jenkins repo" at "https://repo.jenkins-ci.org/public/"
addSbtPlugin("ohnosequences" % "sbt-github-release" % "0.7.0")

resolvers += Resolver.bintrayRepo("twtmt", "sbt-plugins")
addSbtPlugin("com.github.tmtsoftware" % "kotlin-plugin" % "2.0.1-RC1")

resolvers += Resolver.defaultLocal
addSbtPlugin("com.github.tmtsoftware" % "sbt-docs"      % "0.1.3-RC2")

resolvers += Resolver.jcenterRepo
addSbtPlugin("net.aichler" % "sbt-jupiter-interface" % "0.8.3")

libraryDependencies += "com.sun.activation" % "javax.activation" % "1.2.0"

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
