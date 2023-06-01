package misis.route

import akka.http.javadsl.server.Route
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import io.circe._, io.circe.generic.auto._, io.circe.parser._, io.circe.syntax._
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport

class HelloRoute extends FailFastCirceSupport {
    def route = (path("hello") & get) {
        {
            complete(
                HttpEntity(
                    ContentTypes.`text/html(UTF-8)`,
                    "<h1>Сервис Операций запущен</h1>"
                )
            )
        }
    }
}
