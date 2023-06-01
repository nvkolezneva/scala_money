package misis

import akka.actor.ActorSystem
import scala.concurrent.Future
import scala.util.Success
import scala.concurrent.ExecutionContext
import slick.jdbc.PostgresProfile.api._
import misis.route._
import scala.io.StdIn
import akka.http.scaladsl.Http
import misis.repository.Repository
import misis.kafka.Streams
import misis.model.AccountUpdate
import akka.http.scaladsl.server.Directives._
import io.circe.generic.auto._
import io.circe.parser._
import io.circe.syntax._

object AkkaKafkaDemo extends App {
    implicit val system: ActorSystem = ActorSystem("App")
    implicit val ec: ExecutionContext = system.dispatcher

    val helloRoute = new HelloRoute().route
    val limit = 1000
    val feePercent = 20

    private val repository = new Repository(limit)
    private val streams = new Streams(repository, feePercent)

    val bindingFuture =
        Http()
            .newServerAt("0.0.0.0", 8081)
            .bind(
                helloRoute
            )

    println(
        s"Fees service started.\nPress RETURN to stop..."
    )
    StdIn.readLine()
}
