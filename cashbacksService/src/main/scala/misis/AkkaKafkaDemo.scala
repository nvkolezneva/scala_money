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

    private val repository = new Repository()
    private val streams = new Streams(repository)

    val bindingFuture =
        Http()
            .newServerAt("0.0.0.0", 8081)
            .bind(
                helloRoute
            )

    println(
        s"Cashbacks service started.\nPress RETURN to stop..."
    )
    StdIn.readLine()
}
