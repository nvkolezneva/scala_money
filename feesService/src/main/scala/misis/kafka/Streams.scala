package misis.kafka

import io.circe._
import io.circe.parser._
import io.circe.syntax._
import io.circe.generic.auto._
import akka.actor.ActorSystem
import akka.stream.scaladsl.{Flow, Sink, Source}
import misis.kafka.WithKafka
import misis.repository.Repository
import misis.model._

import scala.concurrent.ExecutionContext

class Streams(repository: Repository, feePercent: Int)(implicit
    val system: ActorSystem,
    executionContext: ExecutionContext
) extends WithKafka {
    override def group: String = "fees"
    def bankAccountId: Int = 0

    kafkaSource[AccountToAck]
        .map { e =>
            if (!repository.accountFreeLimitExists(e.sourceId)) {
                repository.createAccountLimit(e.sourceId)
                println(s"ACCOUNT LIMIT CREATED FOR ACCOUNT #${e.sourceId}")
            }
            val limit =
                repository
                    .getAccountLimit(e.sourceId)
                    .getOrElse(AccountFreeLimit(e.sourceId, repository.defaultLimit))
            println(s"ACCOUNT ${e.sourceId} HAS LIMIT: ${limit.freeLimit}")
            if (repository.isFreeLimitOver(e.sourceId, e.value)) {
                val fee = e.value * feePercent / 100
                AccountUpdate(e.sourceId, -e.value, fee, Some(e.destinationId))
            } else {
                AccountUpdate(e.sourceId, -e.value, 0, Some(e.destinationId))
            }
        }
        .to(kafkaSink)
        .run()

    kafkaSource[AccountUpdated]
        .filter(event => event.nextAccountId.isDefined)
        .map { event =>
            repository.updateFreeLimit(event.accountId, -event.value)
            println(s"ACCOUNT ${event.accountId} LIMIT WAS UPDATED ON: ${event.value}")
            val limit =
                repository
                    .getAccountLimit(event.accountId)
                    .getOrElse(AccountFreeLimit(event.accountId, repository.defaultLimit))
            println(s"ACCOUNT ${event.accountId} HAS LIMIT: ${limit.freeLimit}")
            AccountUpdate(bankAccountId, event.feeValue)
        }
        .to(kafkaSink)
        .run()
}
