/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package esw.performance.utils

import java.io.{File, FileOutputStream, PrintStream}

import esw.performance.InfrastructureOverheadTest.log
import org.HdrHistogram.Histogram

import scala.util.{Try, Using}

object PerfUtils {

  def printResults(histogram: Histogram): Unit = {
    println("50 %tile: " + histogram.getValueAtPercentile(50))
    println("90 %tile: " + histogram.getValueAtPercentile(90))
    println("99 %tile: " + histogram.getValueAtPercentile(99))
  }

  def recordResults(histogram: Histogram, filename: String): Unit =
    Try {
      val resultsFile = new File(filename)
      resultsFile.createNewFile()
      println(s"Histogram results are written to file ${resultsFile.getAbsolutePath}")

      Using.resource(new FileOutputStream(resultsFile)) { fos =>
        val printStream = new PrintStream(fos)
        histogram.outputPercentileDistribution(printStream, 1.0)
        printStream.println()
        printStream.println(s"50%tile: ${histogram.getValueAtPercentile(50)}")
        printStream.println(s"90%tile : ${histogram.getValueAtPercentile(90)}")
        printStream.println(s"99%tile : ${histogram.getValueAtPercentile(99)}")
      }
    }.recover(e => log.error("Writing histogram results failed with error", ex = e))
}
