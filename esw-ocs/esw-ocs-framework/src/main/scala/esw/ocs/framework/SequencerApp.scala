package esw.ocs.framework

object SequencerApp extends SequencerAppTemplate

class SequencerAppTemplate {
  def main(args: Array[String]): Unit = {
    args match {
      case Array("-seqcomp", name) =>
        new SequenceComponentWiring(name).start()
      case Array("-sequencer", sequencerId, observingMode) =>
        new SequencerWiring(sequencerId, observingMode).start()
      case _ => println("""
          |please provide one of these alternatives:
          |-seqcomp sequencerComponentName
          |-sequencer sequencerId observingMode
          |""".stripMargin)
    }
  }
}
