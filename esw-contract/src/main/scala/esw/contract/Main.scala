package esw.contract

import csw.contract.generator.{FilesGenerator, Services}


object Main {
  def main(args: Array[String]): Unit = {
    val DefaultOutputPath   = "esw-contract/target/contracts"
    val DefaultResourcePath = "esw-contract/src/main/resources"
    val outputPath          = if (args.isEmpty) DefaultOutputPath else args(0)
    val resourcesPath       = if (args.isEmpty) DefaultResourcePath else args(1)
    FilesGenerator.generate(Services(Map.empty), outputPath, resourcesPath)
  }
}
