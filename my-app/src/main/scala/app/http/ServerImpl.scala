package app.http

import app.http.models.ServerResponse

class ServerImpl {
  def sayHello(): ServerResponse        = ServerResponse("Hello!!!")
  def securedSayHello(): ServerResponse = ServerResponse("Secured Hello!!!")
}
