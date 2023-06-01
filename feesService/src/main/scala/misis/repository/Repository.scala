package misis.repository

import misis.kafka.TopicName
import misis.kafka.Streams
import misis.model._
import io.circe.generic.auto._
import scala.concurrent.Future
import scala.collection.mutable

class Repository(defaultFreeLimit: Int) {
    val defaultLimit = defaultFreeLimit
    private val accountsFreeLimits = mutable.Map.empty[Int, AccountFreeLimit]

    def accountFreeLimitExists(accountId: Int): Boolean = {
        accountsFreeLimits.contains(accountId)
    }

    def createAccountLimit(accountId: Int, limit: Int = defaultFreeLimit): Future[AccountFreeLimit] = {
        val accountLimit = AccountFreeLimit(accountId, limit)
        accountsFreeLimits += (accountId -> accountLimit)
        Future.successful(accountLimit)
    }

    def isFreeLimitOver(accountId: Int, value: Int): Boolean = {
        accountsFreeLimits.get(accountId).exists(_.freeLimit - value <= 0)
    }

    def updateFreeLimit(accountId: Int, value: Int): Future[Unit] = {
        accountsFreeLimits.get(accountId) match {
            case Some(accountLimit) =>
                val updatedAccountLimit = accountLimit.updateFreeLimit(value)
                accountsFreeLimits += (accountId -> updatedAccountLimit)
                Future.successful(())
            case None =>
                Future.failed(new IllegalArgumentException(s"Limit with account $accountId does not exist"))
        }
    }

    def getAccountLimit(accountId: Int): Option[AccountFreeLimit] = {
        accountsFreeLimits.get(accountId)
    }
}
