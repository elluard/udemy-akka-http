package part4_client

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpMethods, HttpRequest, HttpResponse, StatusCodes, Uri}
import akka.stream.scaladsl.Source
import part4_client.PaymentSystemDomain.PaymentRequest
import spray.json._

import java.util.UUID
import scala.util.{Failure, Success}

object HostLevel extends App with PaymentJsonProtocol {
  implicit val system = ActorSystem("HostLevel")

  import system.dispatcher

  val poolFlow = Http().cachedHostConnectionPool[Int]("www.google.com")

  val creditCards = List(
    CreditCard("4242-4242-4242-4242", "424", "tx-test-account"),
    CreditCard("1234-1234-1234-1234", "123", "tx-daniels-account"),
    CreditCard("1234-1234-4321-1234", "321", "my-awesome-account")
  )

  val paymentsRequests = creditCards.map(creditCard => PaymentRequest(creditCard, "rtjvm-store-account", 99))
  val serverHttpRequests = paymentsRequests.map(paymentsRequest =>
    (
      HttpRequest(
        HttpMethods.POST,
        uri = Uri("/api/payments"),
        entity = HttpEntity(
          ContentTypes.`application/json`,
          paymentsRequest.toJson.prettyPrint
        )
      ),
      UUID.randomUUID().toString
    )
  )
  Source(serverHttpRequests)
    .via(Http().cachedHostConnectionPool[String]("localhost",8080))
    .runForeach { // (Try[HttpResposne], String)
      case (Success(response@HttpResponse(StatusCodes.Forbidden, _, _, _)), orderId) =>
        println(s"The order ID $orderId was not allowd to proceed: $response")
      case (Success(reseponse), orderId) =>
        println(s"The order ID $orderId was successful and returned the response : $reseponse")
      case (Failure(ex), orderId) =>
        println(s"The order ID $orderId could not be completed: $ex")
    }
}
