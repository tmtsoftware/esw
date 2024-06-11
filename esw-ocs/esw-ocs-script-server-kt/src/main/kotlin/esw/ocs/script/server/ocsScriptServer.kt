package esw.ocs.script.server

import org.http4k.core.HttpHandler
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.routing.bind
import org.http4k.routing.path
import org.http4k.routing.routes
import org.http4k.server.Jetty
import org.http4k.server.asServer
import play.api.libs.json.JsValue
import play.api.libs.json.Json
import csw.params.core.formats.JavaJsonSupport
import csw.params.core.formats.JsonSupport
import csw.params.commands.SequenceCommand

fun main() {
    // we can bind HttpHandlers (which are just functions from  Request -> Response) to paths/methods to create a Route,
    // then combine many Routes together to make another HttpHandler
    val app: HttpHandler = routes(
        "/execute" bind POST to {
            val setup: SequenceCommand = JavaJsonSupport.readSequenceCommand(Json.parse(it.bodyString()))
            setup
            Response(OK).body("pong!")
        },
        "/greet/{name}" bind GET to { req: Request ->
            val name: String? = req.path("name")
            Response(OK).body("hello ${name ?: "anon!"}")
        }
    )
    app.asServer(Jetty(9000)).start()
}
