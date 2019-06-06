ThisBuild / scalaVersion := EswKeys.scalaVersion
ThisBuild / version := EswKeys.projectVersion
ThisBuild / organization := "com.github.tmtsoftware.esw"
ThisBuild / organizationName := "TMT Org"
ThisBuild / homepage := Some(new URL(EswKeys.homepageValue))

val aggregateProjects = Seq()

lazy val esw = (project in file("."))
  .aggregate(aggregateProjects: _*)
  .settings(
    name := EswKeys.projectName,
    libraryDependencies ++= Dependencies.esw.value
  )
