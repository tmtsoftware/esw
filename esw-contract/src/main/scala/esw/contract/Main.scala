package esw.contract

import csw.contract.generator.FilesGenerator
import esw.contract.data.EswData

/**
 * The main which generates contract data
 */
object Main {
  def main(args: Array[String]): Unit = {
    val DefaultOutputPath = "esw-contract/target/contracts"
    val outputPath        = if (args.isEmpty) DefaultOutputPath else args(0)
    FilesGenerator.generate(EswData.services, outputPath)
  }
}
