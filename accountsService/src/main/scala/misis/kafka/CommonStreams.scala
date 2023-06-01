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

class CommonStreams(repository: Repository, groupId: Int)(implicit
    val system: ActorSystem,
    executionContext: ExecutionContext
) extends WithKafka {

    def group = s"account-create"

    kafkaSource[CreateAccount]
        .mapAsync(1) { command =>
            println(s"[GROUP ${groupId}] TRY TO CREATE ACCOUNT WITH AMOUNT: ${command.initialAmount}")
            repository
                .createAccount(command.categoryId, command.initialAmount)
                .map(account => AccountCreated(account.id, account.amount))
        }
        .to(kafkaSink)
        .run()

    kafkaSource[CreateBankAccount]
        .mapAsync(1) { command =>
            repository
                .createBankAccount(command.initialAmount)
                .map(account => AccountCreated(account.id, account.amount))
        }
        .to(kafkaSink)
        .run()
}
