package esw

import akka.actor.typed.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.headers.Accept
import akka.http.scaladsl.model.{HttpRequest, MediaRange, MediaType}
import akka.http.scaladsl.unmarshalling.Unmarshal

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

object GitUtil {
  def latestCommitSHA(repo: String)(implicit actorSystem: ActorSystem[_]): String = {
    val response = Await.result(
      Http()
        .singleRequest(
          HttpRequest(uri = s"https://api.github.com/repos/tmtsoftware/$repo/commits/refs/heads/master")
            .withHeaders(Accept(MediaRange(MediaType.applicationWithOpenCharset("vnd.github.VERSION.sha"))))
        ),
      5.seconds
    )

    Await.result(Unmarshal(response).to[String], 5.seconds)
  }
}
