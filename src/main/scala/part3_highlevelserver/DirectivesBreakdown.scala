package part3_highlevelserver

import akka.actor.ActorSystem
import akka.event.LoggingAdapter
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpRequest, StatusCodes}

object DirectivesBreakdown extends App {
  implicit val system = ActorSystem("DirectivesBreakdown")

  import system.dispatcher
  import akka.http.scaladsl.server.Directives._

  /**
   * Type #1 : filtering directives
   */

  val simpleHttpMethodRoute =
    post {
      complete(StatusCodes.Forbidden)
    }

  val simplePathRoute =
    path("about") {
      complete(
        HttpEntity(
          ContentTypes.`application/json`,
          """
            |<html>
            | <body>
            |   Hello from the about page!
            | </body>
            |</html>
          """.stripMargin
        )
      )
    }

  val complexPathRoute =
    path("api" / "myEndpoint") {
      complete(StatusCodes.OK)
    }

  //이 경우에는 /api%2FmyEndPoint 로 GET Request 를 보내면 ok 가 떨어진다.
  val dontConfuse =
    path("api/myEndPoint") {
      complete(StatusCodes.OK)
    }

  val pathEndRoute =
    pathEndOrSingleSlash {
      complete(StatusCodes.OK)
    }

//  Http().newServerAt("localhost", 8080).bindFlow(dontConfuse)

  /**
   * Type #2 : extraction directives
   */

  //GET on /api/item/42
  val pathExtractionRoute =
    path("api"/"item"/IntNumber) { (itemNumber : Int) =>
      // other directives
      println(s"I've got a number in my path : $itemNumber")
      complete(StatusCodes.OK)
    }

  val pathMultiExtractRoute =
    path("api" / "order" / IntNumber / IntNumber) {  (id, inventory) =>
      println(s"I've got two numbers in my path, $id, $inventory")
      complete(StatusCodes.OK)
    }

  val queryParamExtractionRoute =
    path("api" / "item") {
      parameter("id") { (itemId: String) =>
        println(s"I've extract the ID as $itemId")
        complete(StatusCodes.OK)
      }
    }

  val extractRequestRoute =
    path("controlEndPoint") {
      extractRequest { (httpRequest : HttpRequest) =>
        extractLog { (log: LoggingAdapter) =>
          log.info(s"I got the http request : $httpRequest")
          complete(StatusCodes.OK)
        }
      }
    }

  /**
   * Type #3: composite directives
   */
  val simpleNestedRoute =
    path("api" / "item") {
      get {
        complete(StatusCodes.OK)
      }
    }

  val compactSimpleNestedRoute = (path("api" / "item") & get) {
    complete(StatusCodes.OK)
  }

  val compactExtractRequestRoute =
    (path("controlEndPoint") & extractRequest & extractLog) { (request, log) =>
      log.info(s"I got eh http request $request")
      complete(StatusCodes.OK)
    }

  // about and /aboutUS
  val repeatedRoute =
    path("about") {
      complete(StatusCodes.OK)
    } ~
    path("aboutUs") {
      complete(StatusCodes.OK)
    }

  val dryRoute =
    (path("about") | path("aboutUS")) {
      complete(StatusCodes.OK)
    }

  // yourblog.com/42 AND yourblog.com?postId=42

  val blogByIdRoute =
    path(IntNumber) { (blogId: Int) =>
      complete(StatusCodes.OK)
    }

  val blogByQueryParamRoute =
    parameter(Symbol("postId").as[Int]) { (blogpostId: Int) =>
      // the SAME server login
      complete(StatusCodes.OK)
    }

  val combindBlogByIdRoute =
    (path(IntNumber) | parameter(Symbol("postId").as[Int])) { (blogpostId: Int) =>
        // your original server logic
      complete(StatusCodes.OK)
    }

  /**
   * Type #4 : "actionable" directives
   */

  val completeOkRoute = complete(StatusCodes.OK)

  val failedRoute =
    path("notSupported") {
      failWith(new RuntimeException("Unsupported!")) // completes with HTTP 500
    }

  val routeWithRejection =
    path("home") {
      reject
    } ~
    path("index") {
      completeOkRoute
    }

  /**
   * Exercise : can you spot the mistake?
   */
  val getOrPutPath = {
    path("api" / "myEndPoint") {
      get {
        completeOkRoute
      } ~
      post {
        complete(StatusCodes.Forbidden)
      }
    }
  }

  Http().newServerAt("localhost", 8080).bindFlow(getOrPutPath)
}
