package esw.performance.utils

import java.io.{File, FileOutputStream, PrintStream}

import esw.performance.InfrastructureOverheadTest.log
import org.HdrHistogram.Histogram

object PerfUtils {

  def printResults(histogram: Histogram): Unit = {
    println("50 %tile: " + histogram.getValueAtPercentile(50))
    println("90 %tile: " + histogram.getValueAtPercentile(90))
    println("99 %tile: " + histogram.getValueAtPercentile(99))
  }

  def recordResults(histogram: Histogram, filename: String): Unit = {
    try {
      val resultsFile = new File(filename)
      resultsFile.createNewFile()
      println(s"Histogram results are written to file ${resultsFile.getAbsolutePath}")
      val fileOutputStream = new FileOutputStream(resultsFile)
      try {
        val printStream = new PrintStream(fileOutputStream)
        histogram.outputPercentileDistribution(printStream, 1.0)
        printStream.println()
        printStream.println(s"50%tile: ${histogram.getValueAtPercentile(50)}")
        printStream.println(s"90%tile : ${histogram.getValueAtPercentile(90)}")
        printStream.println(s"99%tile : ${histogram.getValueAtPercentile(99)}")
      }
      finally if (fileOutputStream != null) fileOutputStream.close()
    }
    catch {
      case e: Exception =>
        log.error("Writing histogram results failed with error", ex = e)
    }
  }
}
