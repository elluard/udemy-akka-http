package part4_client

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpMethods, HttpRequest, Uri}
import akka.stream.scaladsl.Source
import part4_client.PaymentSystemDomain.PaymentRequest

import scala.util.{Failure, Success}
import spray.json._

object RequestLevel extends App with PaymentJsonProtocol {
  implicit val system = ActorSystem("RequestLevel")

  import system.dispatcher

  val responseFuture = Http().singleRequest(HttpRequest(uri ="http://www.google.com"))

  responseFuture.onComplete {
    case Success(response) =>
      response.discardEntityBytes()
      println(s"The request was successfuil and returned: $response")
    case Failure(ex) =>
      println(s"The request failed with: $ex")
  }

  val creditCards = List(
    CreditCard("4242-4242-4242-4242", "424", "tx-test-account"),
    CreditCard("1234-1234-1234-1234", "123", "tx-daniels-account"),
    CreditCard("1234-1234-4321-1234", "321", "my-awesome-account")
  )

  val paymentsRequests = creditCards.map(creditCard => PaymentRequest(creditCard, "rtjvm-store-account", 99))
  val serverHttpRequests = paymentsRequests.map(paymentsRequest =>
    HttpRequest(
      HttpMethods.POST,
      uri = Uri("http://localhost:8080/api/payments"),
      entity = HttpEntity(
        ContentTypes.`application/json`,
        paymentsRequest.toJson.prettyPrint
      )
    )
  )

  Source(serverHttpRequests)
    .mapAsync(10)(request => Http().singleRequest(request))
    .runForeach(println)
}
