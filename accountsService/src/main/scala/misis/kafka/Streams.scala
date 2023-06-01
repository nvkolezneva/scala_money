package misis.kafka

import akka.kafka.{ConsumerSettings, ProducerSettings, Subscriptions}
import akka.kafka.scaladsl.{Consumer, Producer}
import akka.stream.scaladsl.{Flow, Sink, Source}
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.{ByteArrayDeserializer, StringDeserializer, StringSerializer}
import akka.actor.ActorSystem
import scala.concurrent.ExecutionContext
import misis.repository._
import io.circe._
import io.circe.parser._
import io.circe.syntax._
import io.circe.generic.auto._
import misis.model._
import scala.reflect.ClassTag
import scala.util.{Failure, Success}
import com.typesafe.config.ConfigFactory
import scala.concurrent.Future

class Streams(repository: Repository, groupId: Int)(implicit
    val system: ActorSystem,
    executionContext: ExecutionContext
) extends WithKafka {
    def group = s"account-test-${groupId}"

    kafkaSource[ShowAccountBalance]
        .mapAsync(1) { command =>
            repository.getAccount(command.accountId).map {
                case Some(account) =>
                    println(s"Account ${command.accountId} has balance: ${account.amount}")
                    command
                case None =>
                    println(
                        s"Account ${command.accountId} didn't found. Available accounts: ${repository.getAccountKeys()}"
                    )
                    command
            }
        }
        .to(Sink.ignore)
        .run()

    kafkaSource[TransferStart]
        .map(command => {
            println("(2) Got TransferStart message")
            command
        })
        .filter(command => repository.accountExists(command.sourceId))
        .map { command =>
            println(s"(4) Send AccountFromAck - [SOURCE ACK] Account ${command.sourceId} exists and has enough money")
            AccountFromAck(command.sourceId, command.destinationId, command.value)
        }
        .to(kafkaSink)
        .run()

    kafkaSource[TransferCheckDestination]
        .map(command => {
            println("(6) Got TransferCheckDestination message")
            command
        })
        .filter(command => {
            println(
                s"(6.5) repository.accountExists(command.destinationId) ${repository.accountExists(command.destinationId)}"
            )
            repository.accountExists(command.destinationId)
        })
        .map { command =>
            println(s"(8) Send AccountToAck - [DESTINATION ACK] Account ${command.destinationId} exists ")
            AccountToAck(command.sourceId, command.destinationId, command.value)
        }
        .to(kafkaSink)
        .run()

    kafkaSource[AccountUpdate]
        .map(command => {
            println(s"(10) NEED TO UPDATE ACCOUNT #${command.accountId} WITH AMOUNT ${command.value}")
            println(s"(10.5) ACCOUNT #${command.accountId} EXISTS: ${repository.accountExists(command.accountId)}")
            println(
                s"(10.6) ACCOUNT #${command.accountId} EXISTS WITH AMOUNT: ${repository
                        .accountExistsWithIdAndAmount(command.accountId, command.value)}"
            )
            println(s"FEE VALUE:  ${command.feeValue}")
            command
        })
        .filter(command => repository.accountExistsWithIdAndAmount(command.accountId, command.value - command.feeValue))
        .mapAsync(1) { command =>
            val categoryId = repository.getAccountCategoryId(command.accountId).getOrElse(0)
            repository
                .updateAccount(command.accountId, command.value - command.feeValue)
                .map(_ =>
                    AccountUpdated(
                        command.accountId,
                        command.value,
                        command.feeValue,
                        command.nextAccountId,
                        command.previousAccountId,
                        Some(categoryId)
                    )
                )
        }
        .to(kafkaSink)
        .run()

    kafkaSource[AccountCreated]
        .filter(event => repository.accountExists(event.accountId))
        .map { event =>
            println(s"[GROUP ${groupId}] Account ${event.accountId} was created with amount: ${event.amount}")
            event
        }
        .to(Sink.ignore)
        .run()

    kafkaSource[AccountUpdated]
        .filter(event => repository.accountExists(event.accountId))
        .mapAsync(1) { event =>
            repository.getAccount(event.accountId).map {
                case Some(account) =>
                    if (event.accountId == 0) {
                        println(
                            s"BANK ACCOUNT was updated on ${event.value - event.feeValue}. Bank balance: ${account.amount}"
                        )
                    } else {
                        println(s"Account ${event.accountId} was updated on ${event.value}. Balance: ${account.amount}")
                    }
                    event
                case None => event
            }
        }
        .to(Sink.ignore)
        .run()
}
