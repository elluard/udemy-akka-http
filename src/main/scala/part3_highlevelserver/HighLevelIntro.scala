package part3_highlevelserver

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, StatusCodes}
import akka.http.scaladsl.server.Route

object HighLevelIntro extends App {
  implicit val system = ActorSystem("HighLevelIntro")

  import system.dispatcher

  // directives
  import akka.http.scaladsl.server.Directives._

  val simpelRoute : Route =
    path("home") { //Directive
      complete(StatusCodes.OK) //Directive
    }

  val pathGetRoute : Route =
    path("home") {
      get {
        complete(StatusCodes.OK)
      }
    }

  val chainedRoute : Route =
    path("myEndPoint") {
      get {
        complete(StatusCodes.OK)
      } ~
      post {
        complete(StatusCodes.Forbidden)
      }
    } ~
    path("home"){
      complete(
        HttpEntity(
          ContentTypes.`text/html(UTF-8)`,
          """
            |<html>
            | <body>
            |   Hello from the high level Akka HTTP!
            | </body>
            |</html>
            |""".stripMargin
        )
      )
    } // Routing tree

  Http().newServerAt("localhost", 8080).bindFlow(chainedRoute)
}
