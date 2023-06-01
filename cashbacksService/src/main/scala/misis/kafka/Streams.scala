package misis.kafka

import io.circe._
import io.circe.parser._
import io.circe.syntax._
import io.circe.generic.auto._
import akka.actor.ActorSystem
import akka.stream.scaladsl.{Flow, Sink, Source}
import misis.kafka.WithKafka
import misis.repository._
import misis.model._

import scala.concurrent.ExecutionContext

class Streams(repository: Repository)(implicit val system: ActorSystem, executionContext: ExecutionContext)
    extends WithKafka {
    override def group: String = "cashback"
    def bankAccountId: Int = 0

    kafkaSource[AccountUpdated]
        .filter(event => event.previousAccountId.isDefined && event.accountId != bankAccountId)
        .map { event =>
            val cashbackAccountId = event.previousAccountId.getOrElse(-1)
            val cashbackAmount =
                repository
                    .calculateCashbackAmount(event.categoryId.getOrElse(-1), event.value)
                    .getOrElse(0)
            println(s"ADD CASHBACK FOR ACCOUNT ${cashbackAccountId}")
            if (!repository.accountCashbackExists(cashbackAccountId)) {
                println(s"CASHBACK ENTITY FOR ACCOUNT ${cashbackAccountId} NOT FOUND")
                repository.createAccountCashback(cashbackAccountId, cashbackAmount)
            } else {
                println(s"FOUND CASHBACK ENTITY FOR ACCOUNT ${cashbackAccountId}")
                repository.updateCashback(cashbackAccountId, cashbackAmount)
            }
            CashbackUpdated(cashbackAccountId, cashbackAmount)
        }
        .to(kafkaSink)
        .run()

    kafkaSource[CashbackUpdated]
        .mapAsync(1) { event =>
            repository.getAccountCashback(event.accountId).map {
                case Some(accountCashback) =>
                    println(
                        s"CASHBACK UPDATED FOR ACCOUNT ${event.accountId} ON VALUE ${event.value}. STORED CASHBACK: ${accountCashback.cashback}"
                    )
                    event
                case None =>
                    println(
                        s"PROBLEM FOR ACCOUNT ${event.accountId}"
                    )
                    event
            }
        }
        .to(Sink.ignore)
        .run()

    kafkaSource[ReturnCashback]
        .mapAsync(1) { command =>
            repository.getAccountCashback(command.accountId).map {
                case Some(accountCashback) =>
                    println(
                        s"SHOULD RETURN CASHBACK FOR ACCOUNT ${command.accountId}. STORED CASHBACK: ${accountCashback.cashback}"
                    )
                    repository.resetCashback(command.accountId)
                    AccountUpdate(command.accountId, accountCashback.cashback, 0, None, None)
                case None =>
                    AccountUpdate(-1, 0, 0, None, None)
            }
        }
        .to(kafkaSink)
        .run()
}
