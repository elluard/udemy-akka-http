package part2_lowlevelserver

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.Http.IncomingConnection
import akka.http.scaladsl.model.headers.Location
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpMethods, HttpRequest, HttpResponse, StatusCodes, Uri}
import akka.stream.scaladsl.{Flow, Sink}

import scala.concurrent.Future
import scala.concurrent.duration.DurationInt
import scala.language.postfixOps
import scala.util.{Failure, Success}

object LowLevelAPI extends App {
  implicit val system: ActorSystem = ActorSystem("LowLevelServerAPI")

  import system.dispatcher

  val serverSource = Http().newServerAt("localhost", 8000).connectionSource()
  val connectionSink = Sink.foreach[IncomingConnection] { connection =>
    println(s"Accepted incoming connection from : ${connection.remoteAddress}")
  }

  val serverBindingFuture = serverSource.to(connectionSink).run()

  serverBindingFuture.onComplete {
    case Success(binding) =>
      println(s"Server binding successful")
      binding.terminate(2 seconds)
    case Failure(exception) =>  println(s"Server binding failed: ${exception}")
  }

  /*
    Method 1: synchronously serve HTTP responses
   */
  val requestHandler : HttpRequest => HttpResponse = {
    case HttpRequest(HttpMethods.GET, uri, value, entity, protocol) =>
      HttpResponse(
        StatusCodes.OK,
        entity = HttpEntity(
          ContentTypes.`text/html(UTF-8)`,
          """
            |<html>
            | <body>
            |   Hello from Akka HTTP!
            | </body>
            |</html>
            |""".stripMargin
        )
      )
    case request: HttpRequest =>
      request.discardEntityBytes()
      HttpResponse(
        StatusCodes.NotFound,
        entity = HttpEntity(
          ContentTypes.`text/html(UTF-8)`,
          """
            |<html>
            | <body>
            |   OOPPS! The resource can't be found.
            | </body>
            |</html>
            |""".stripMargin
        )
      )
  }

  val httpSyncConnectionHandler = Sink.foreach[IncomingConnection] { connection =>
    connection.handleWithSyncHandler(requestHandler)
  }

  Http().newServerAt("localhost", 8080).bindSync(requestHandler)
//  Http().newServerAt("localhost", 8080).connectionSource().runWith(httpSyncConnectionHandler)]

  /*
    Method 2: serve back HTTP response asynchronously
   */
  val asyncRequestHandler : HttpRequest => Future[HttpResponse] = {
    case HttpRequest(HttpMethods.GET, Uri.Path("/home"), value, entity, protocol) =>
      Future{ HttpResponse(
        StatusCodes.OK,
        entity = HttpEntity(
          ContentTypes.`text/html(UTF-8)`,
          """
            |<html>
            | <body>
            |   Hello from Akka HTTP!
            | </body>
            |</html>
            |""".stripMargin
        )
      )}
    case request: HttpRequest =>
      request.discardEntityBytes()
      Future { HttpResponse(
        StatusCodes.NotFound,
        entity = HttpEntity(
          ContentTypes.`text/html(UTF-8)`,
          """
            |<html>
            | <body>
            |   OOPPS! The resource can't be found.
            | </body>
            |</html>
            |""".stripMargin
        )
      )}
  }

  val httpAsyncConnectionHandler = Sink.foreach[IncomingConnection] { connection =>
    connection.handleWithAsyncHandler(asyncRequestHandler)
  }

//  Http().newServerAt("localhost", 8081).connectionSource().runWith(httpAsyncConnectionHandler)
//  Http().newServerAt("localhost", 8081).bind(asyncRequestHandler)

  /*
    Method 3: - async via akka streams
   */
  val streamsBasedRequestHandler : Flow[HttpRequest, HttpResponse, _] = Flow[HttpRequest].map {
    case HttpRequest(HttpMethods.GET, Uri.Path("/home"), value, entity, protocol) =>
      HttpResponse(
        StatusCodes.OK,
        entity = HttpEntity(
          ContentTypes.`text/html(UTF-8)`,
          """
          |<html>
          | <body>
          |   Hello from Akka HTTP!
          | </body>
          |</html>
          |""".stripMargin
        )
      )
    case request: HttpRequest =>
      request.discardEntityBytes()
      HttpResponse(
        StatusCodes.NotFound,
        entity = HttpEntity(
          ContentTypes.`text/html(UTF-8)`,
          """
            |<html>
            | <body>
            |   OOPPS! The resource can't be found.
            | </body>
            |</html>
            |""".stripMargin
        )
      )
  }

  // "manual" version
//  Http().newServerAt("localhost", 8082).connectionSource().runForeach { connection =>
//    connection.handleWith(streamsBasedRequestHandler)
//  }

  // shorthand version
  Http().newServerAt("localhost", 8082).bindFlow(streamsBasedRequestHandler)

  /**
   * Exercise : create your own HTTP Server running on localhost on 8388, which replies
   *  - with a welcome message on the "front door" localhost:8388
   *  - with a proper HTML on localhost:8388/about
   *  - with a 404 message otherwise
   */

  val exerciseHandler : Flow[HttpRequest, HttpResponse, _] = Flow[HttpRequest].map {
    case HttpRequest(HttpMethods.GET, Uri.Path("/"), value, entity, protocol) =>
      HttpResponse(
        StatusCodes.OK,
        entity = HttpEntity(
          ContentTypes.`text/html(UTF-8)`,
          """
            |<html>
            | <body>
            |   Welcome!
            | </body>
            |</html>
            |""".stripMargin
        )
      )
    case HttpRequest(HttpMethods.GET, Uri.Path("/about"), value, entity, protocol) =>
      HttpResponse(
        StatusCodes.OK,
        entity = HttpEntity(
          ContentTypes.`text/html(UTF-8)`,
          """
            |<html>
            | <body>
            |   here is about
            | </body>
            |</html>
            |""".stripMargin
        )
      )
    case HttpRequest(HttpMethods.GET, Uri.Path("/search"), _, _, _) =>
      HttpResponse(
        StatusCodes.Found,
        headers = List(Location("http://google.com"))
      )
    case request: HttpRequest =>
      request.discardEntityBytes()
      HttpResponse(
        StatusCodes.NotFound,
        entity = HttpEntity(
          ContentTypes.`text/html(UTF-8)`,
          """
            |<html>
            | <body>
            |   OOPPS! The resource can't be found.
            | </body>
            |</html>
            |""".stripMargin
        )
      )
  }
  val bindingFuture = Http().newServerAt("localhost", 8388).bindFlow(exerciseHandler)

  // shutdown the server
  bindingFuture
    .flatMap(binding => binding.unbind())
    .onComplete(_ => system.terminate())
}
