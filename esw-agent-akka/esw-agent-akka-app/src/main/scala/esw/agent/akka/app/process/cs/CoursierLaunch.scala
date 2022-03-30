/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package esw.agent.akka.app.process.cs

case class CoursierLaunch(appName: String, appVersion: Option[String]) {
  private val app = appVersion.fold(appName)(version => s"$appName:$version")

  def launch(repos: List[String], mainClass: String, args: List[String]): List[String] =
    List(Coursier.cs, "launch") ::: app :: "-r" :: repos ::: "-M" :: mainClass :: "--" :: args

  // cs launch --channel url://apps.json esw-ocs-app:2.0.0 -- start seqcomp
  def launch(channel: String, args: List[String]): List[String] =
    List(Coursier.cs, "launch") ::: "--channel" :: channel :: app :: "--" :: args

  def fetch(channel: String): List[String] = List(Coursier.cs, "fetch") ::: "--channel" :: channel :: app :: Nil
}
