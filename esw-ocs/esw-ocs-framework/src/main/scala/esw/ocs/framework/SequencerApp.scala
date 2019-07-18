package esw.ocs.framework

import esw.ocs.framework.internal.{SequenceComponentWiring, SequencerWiring}

object SequencerApp extends SequencerAppTemplate

// fixme: use scopt to parse args
class SequencerAppTemplate {
  def main(args: Array[String]): Unit = {
    args match {
      case Array("-seqcomp", name)                         => new SequenceComponentWiring(name).start()
      case Array("-sequencer", sequencerId, observingMode) => new SequencerWiring(sequencerId, observingMode).start()
      case _ =>
        println(
          """
          |please provide one of these alternatives:
          |-seqcomp sequencerComponentName
          |-sequencer sequencerId observingMode
          |""".stripMargin
        )
    }
  }
}
