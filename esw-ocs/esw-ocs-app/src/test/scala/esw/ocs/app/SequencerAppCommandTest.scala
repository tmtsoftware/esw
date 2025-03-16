package esw.ocs.app

import org.scalatest.EitherValues
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class SequencerAppCommandTest extends AnyWordSpec with EitherValues with Matchers {

  "subsystemParser" must {
    "return error if invalid subsystem is provided | ESW-102, ESW-136" in {
      val subsystem = "Invalid"
      SequencerAppCommand.subsystemParser.parse(subsystem, 0, 0).left.value.message should ===(
        s"Subsystem [$subsystem] is invalid"
      )
    }
  }
}
