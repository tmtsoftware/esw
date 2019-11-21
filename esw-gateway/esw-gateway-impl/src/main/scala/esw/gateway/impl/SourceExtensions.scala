package esw.gateway.impl

import akka.stream.KillSwitches
import akka.stream.scaladsl.{Keep, Source}
import msocket.api.models.Subscription

object SourceExtensions {

  implicit class RichSource[T, Mat](stream: Source[T, Mat]) {

    def withSubscription(): Source[T, Subscription] =
      stream
        .viaMat(KillSwitches.single)(Keep.right)
        .mapMaterializedValue(switch => () => switch.shutdown())
  }
}
