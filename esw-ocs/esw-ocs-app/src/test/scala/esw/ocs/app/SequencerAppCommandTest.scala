package esw.ocs.app

import org.scalatest.{EitherValues, Matchers, WordSpec}

class SequencerAppCommandTest extends WordSpec with EitherValues with Matchers {

  "stringParser" must {
    "return error if invalid packageId is provided | ESW-102, ESW-136" in {
      val invalidPackageId = "x@y"
      SequencerAppCommand.stringParser.parse(invalidPackageId).left.value.message should ===(s"[$invalidPackageId] is invalid")
    }

  }

  "subsystemParser" must {
    "return error if invalid subsystem is provided | ESW-102, ESW-136" in {
      val subsystem = "Invalid"
      SequencerAppCommand.subsystemParser.parse(subsystem).left.value.message should ===(s"Subsystem [$subsystem] is invalid")
    }
  }
}
