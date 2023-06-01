package misis.kafka

import io.circe._
import io.circe.parser._
import io.circe.syntax._
import io.circe.generic.auto._
import akka.actor.ActorSystem
import akka.stream.scaladsl.{Flow, Sink, Source}
import misis.kafka.WithKafka
import misis.model._

import scala.concurrent.ExecutionContext

class Streams()(implicit val system: ActorSystem, executionContext: ExecutionContext) extends WithKafka {
    override def group: String = "operation"

    kafkaSource[AccountFromAck]
        .map { e =>
            println(
                s"(5) Got AccountFromAck and send TransferCheckDestination - Ask for existence of account ${e.sourceId}"
            )
            TransferCheckDestination(e.sourceId, e.destinationId, e.value)
        }
        .to(kafkaSink)
        .run()

    kafkaSource[AccountUpdated]
        .filter(event => event.nextAccountId.isDefined)
        .map { e =>
            println(s"[2 OF 3 SUCCESS] Send accrual request")
            AccountUpdate(e.nextAccountId.getOrElse(0), -e.value, 0, None, Some(e.accountId))
        }
        .to(kafkaSink)
        .run()
}
